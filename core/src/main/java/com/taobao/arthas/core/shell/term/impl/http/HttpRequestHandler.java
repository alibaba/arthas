package com.taobao.arthas.core.shell.term.impl.http;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.term.impl.http.api.HttpApiHandler;
import com.taobao.arthas.mcp.server.protocol.server.handler.McpHttpRequestHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.util.Logging;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static com.taobao.arthas.core.util.HttpUtils.createRedirectResponse;
import static com.taobao.arthas.core.util.HttpUtils.createResponse;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * HTTP请求处理器
 * 处理所有HTTP请求，包括API调用、MCP请求、WebUI资源和静态文件
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-11-06
 * @author gongdewei 2020-03-18
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);

    /**
     * WebSocket URI路径
     */
    private final String wsUri;

    /**
     * 输出目录，用于存放静态文件
     */
    private File dir;

    /**
     * HTTP API处理器
     */
    private HttpApiHandler httpApiHandler;

    /**
     * MCP（Model Context Protocol）请求处理器
     */
    private McpHttpRequestHandler mcpRequestHandler;

    /**
     * 构造HTTP请求处理器（使用默认输出目录）
     *
     * @param wsUri WebSocket URI路径
     */
    public HttpRequestHandler(String wsUri) {
        this(wsUri, ArthasBootstrap.getInstance().getOutputPath());
    }

    /**
     * 构造HTTP请求处理器
     *
     * @param wsUri WebSocket URI路径
     * @param dir   输出目录，用于存放静态文件
     */
    public HttpRequestHandler(String wsUri, File dir) {
        this.wsUri = wsUri;
        this.dir = dir;
        dir.mkdirs();
        this.httpApiHandler = ArthasBootstrap.getInstance().getHttpApiHandler();
        this.mcpRequestHandler = ArthasBootstrap.getInstance().getMcpRequestHandler();
    }

    /**
     * 处理HTTP请求
     * 这是Netty的请求处理入口方法，负责分发不同类型的请求
     *
     * @param ctx     通道处理器上下文
     * @param request 完整的HTTP请求
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 解析请求路径
        String path = new URI(request.uri()).getPath();

        // 如果是WebSocket请求，传递给下一个处理器
        if (wsUri.equalsIgnoreCase(path)) {
            ctx.fireChannelRead(request.retain());
        } else {
            // 处理HTTP请求
            // 检查是否需要发送100 Continue响应
            if (HttpUtil.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

            HttpResponse response = null;
            // 将根路径重定向到index.html
            if ("/".equals(path)) {
                path = "/index.html";
            }

            boolean isFileResponseFinished = false;
            boolean isMcpHandled = false;
            try {
                // 处理HTTP RESTful API请求
                if ("/api".equals(path)) {
                    response = httpApiHandler.handle(ctx, request);
                }

                // 处理MCP（Model Context Protocol）请求
                if (mcpRequestHandler != null) {
                    String mcpEndpoint = mcpRequestHandler.getMcpEndpoint();
                    if (mcpEndpoint.equals(path)) {
                        mcpRequestHandler.handle(ctx, request);
                        isMcpHandled = true;
                        return;
                    }
                }

                // 处理WebUI请求
                if (path.equals("/ui")) {
                    response = createRedirectResponse(request, "/ui/");
                }
                if (path.equals("/ui/")) {
                    path += "index.html";
                }

                // 优先尝试从类路径资源中读取文件
                if (response == null) {
                    response = readFileFromResource(request, path);
                }

                // 然后尝试从输出目录中读取文件，避免覆盖类路径资源文件
                if (response == null) {
                    response = DirectoryBrowser.directView(dir, path, request, ctx);
                    isFileResponseFinished = response != null;
                }

                // 如果都没有找到，返回404错误
                if (response == null) {
                    response = createResponse(request, HttpResponseStatus.NOT_FOUND, "Not found");
                }
            } catch (Throwable e) {
                // 记录处理HTTP请求时的错误
                logger.error("arthas process http request error: " + request.uri(), e);
            } finally {
                // 如果响应为null，说明发生了错误，返回500错误
                if (response == null) {
                    response = createResponse(request, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Server error");
                }
                // 如果文件响应未完成且不是MCP请求，则写入响应并关闭连接
                if (!isFileResponseFinished && !isMcpHandled) {
                    ChannelFuture future = writeResponse(ctx, response);
                    future.addListener(ChannelFutureListener.CLOSE);
                }
            }
        }
    }

    /**
     * 写入HTTP响应
     * 根据响应类型选择合适的写入方式
     *
     * @param ctx      通道处理器上下文
     * @param response HTTP响应对象
     * @return 写入操作的ChannelFuture
     */
    private ChannelFuture writeResponse(ChannelHandlerContext ctx, HttpResponse response) {
        // 对于DefaultFullHttpResponse，尝试添加content-length头
        if (!HttpUtil.isTransferEncodingChunked(response)
                && response instanceof DefaultFullHttpResponse) {
            // 设置连接为关闭模式
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            // 设置内容长度
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                    ((DefaultFullHttpResponse) response).content().readableBytes());
            return ctx.writeAndFlush(response);
        }

        // 分块传输编码的响应
        ctx.write(response);
        return ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }

    /**
     * 从类路径资源中读取文件
     * 支持html、js、css等静态资源文件
     *
     * @param request HTTP请求对象
     * @param path    资源路径
     * @return HTTP响应对象，如果资源不存在则返回null
     * @throws IOException 读取资源时可能抛出IO异常
     */
    private HttpResponse readFileFromResource(FullHttpRequest request, String path) throws IOException {
        DefaultFullHttpResponse fullResp = null;
        InputStream in = null;
        try {
            // 从类路径中查找资源
            URL res = HttpTtyConnection.class.getResource("/com/taobao/arthas/core/http" + path);
            if (res != null) {
                // 创建HTTP 200响应
                fullResp = new DefaultFullHttpResponse(request.protocolVersion(),
                        HttpResponseStatus.OK);
                in = res.openStream();
                // 读取资源内容并写入响应
                byte[] tmp = new byte[256];
                for (int l = 0; l != -1; l = in.read(tmp)) {
                    fullResp.content().writeBytes(tmp, 0, l);
                }

                // 根据文件扩展名设置Content-Type
                int li = path.lastIndexOf('.');
                if (li != -1 && li != path.length() - 1) {
                    String ext = path.substring(li + 1);
                    String contentType;
                    if ("html".equals(ext)) {
                        contentType = "text/html";
                    } else if ("js".equals(ext)) {
                        contentType = "application/javascript";
                    } else if ("css".equals(ext)) {
                        contentType = "text/css";
                    } else {
                        contentType = null;
                    }

                    if (contentType != null) {
                        fullResp.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                    }
                }
            }
        } finally {
            IOUtils.close(in);
        }
        return fullResp;
    }

    /**
     * 发送100 Continue响应
     * 当客户端期望在发送请求体之前先确认服务器愿意接收请求时使用
     *
     * @param ctx 通道处理器上下文
     */
    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    /**
     * 处理异常情况
     * 当发生IO异常时，记录错误并关闭连接
     *
     * @param ctx    通道处理器上下文
     * @param cause 异常原因
     * @throws Exception 可能抛出的异常
     */
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Logging.logReportedIoError(cause);
        ctx.close();
    }
}
