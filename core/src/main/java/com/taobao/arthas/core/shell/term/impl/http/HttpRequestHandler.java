package com.taobao.arthas.core.shell.term.impl.http;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.term.impl.http.api.HttpApiHandler;
import com.taobao.arthas.core.shell.term.impl.httptelnet.HttpTelnetTermServer;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
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
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-11-06
 * @author gongdewei 2020-03-18
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpTelnetTermServer.class);

    private final String wsUri;

    private File dir;

    private HttpApiHandler httpApiHandler;


    public HttpRequestHandler(String wsUri) {
        this(wsUri, ArthasBootstrap.getInstance().getOutputPath());
    }

    public HttpRequestHandler(String wsUri, File dir) {
        this.wsUri = wsUri;
        this.dir = dir;
        dir.mkdirs();
        this.httpApiHandler = ArthasBootstrap.getInstance().getHttpApiHandler();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String path = new URI(request.uri()).getPath();
        if (wsUri.equalsIgnoreCase(path)) {
            ctx.fireChannelRead(request.retain());
        } else {
            if (HttpUtil.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

            HttpResponse response = null;
            if ("/".equals(path)) {
                path = "/index.html";
            }

            boolean isHttpApiResponse = false;
            boolean isFileResponseFinished = false;
            try {
                //handle http restful api
                if ("/api".equals(path)) {
                    response = httpApiHandler.handle(ctx, request);
                    isHttpApiResponse = true;
                }

                //handle webui requests
                if (path.equals("/ui")) {
                    response = createRedirectResponse(request, "/ui/");
                }
                if (path.equals("/ui/")) {
                    path += "index.html";
                }

                //try classpath resource first
                if (response == null) {
                    response = readFileFromResource(request, path);
                }

                //try output dir later, avoid overlay classpath resources files
                if (response == null) {
                    response = DirectoryBrowser.directView(dir, path, request, ctx);
                    isFileResponseFinished = (response == null) ? false : true;
                }

                //not found
                if (response == null) {
                    response = createResponse(request, HttpResponseStatus.NOT_FOUND, "Not found");
                }
            } catch (Throwable e) {
                logger.error("arthas process http request error: " + request.uri(), e);
            } finally {
                //If it is null, an error may occur
                if (response == null) {
                    response = createResponse(request, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Server error");
                }
                if (!isFileResponseFinished) {
                    ChannelFuture future = writeResponse(ctx, response);
                    future.addListener(ChannelFutureListener.CLOSE);
                    //reuse http api response buf
                    if (isHttpApiResponse && response instanceof DefaultFullHttpResponse) {
                        final HttpResponse finalResponse = response;
                        future.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                httpApiHandler.onCompleted((DefaultFullHttpResponse) finalResponse);
                            }
                        });
                    }
                }
            }
        }
    }

    private ChannelFuture writeResponse(ChannelHandlerContext ctx, HttpResponse response) {
        // try to add content-length header for DefaultFullHttpResponse
        if (!HttpUtil.isTransferEncodingChunked(response)
            && response instanceof DefaultFullHttpResponse) {
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                ((DefaultFullHttpResponse) response).content().readableBytes());
            return ctx.writeAndFlush(response);
        }

        //chunk response
        ctx.write(response);
        return ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }

    private HttpResponse readFileFromResource(FullHttpRequest request, String path) throws IOException {
        DefaultFullHttpResponse fullResp = null;
        InputStream in = null;
        try {
            URL res = HttpTtyConnection.class.getResource("/com/taobao/arthas/core/http" + path);
            if (res != null) {
                fullResp = new DefaultFullHttpResponse(request.protocolVersion(),
                    HttpResponseStatus.OK);
                in = res.openStream();
                byte[] tmp = new byte[256];
                for (int l = 0; l != -1; l = in.read(tmp)) {
                    fullResp.content().writeBytes(tmp, 0, l);
                }
                int li = path.lastIndexOf('.');
                if (li != -1 && li != path.length() - 1) {
                    String ext = path.substring(li + 1, path.length());
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

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Logging.logReportedIoError(cause);
        ctx.close();
    }
}
