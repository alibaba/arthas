package com.taobao.arthas.core.shell.term.impl.http;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.core.shell.term.impl.httptelnet.HttpTelnetTermServer;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.util.Logging;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-11-06
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpTelnetTermServer.class);

    private final String wsUri;

    private File dir;

    public HttpRequestHandler(String wsUri, File dir) {
        this.wsUri = wsUri;
        this.dir = dir;
        dir.mkdirs();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (wsUri.equalsIgnoreCase(request.uri())) {
            ctx.fireChannelRead(request.retain());
        } else {
            if (HttpUtil.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }

            HttpResponse response = new DefaultHttpResponse(request.protocolVersion(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);

            String path = new URI(request.uri()).getPath();

            if ("/".equals(path)) {
                path = "/index.html";
            }

            InputStream in = null;
            try {

                DefaultFullHttpResponse fileViewResult = DirectoryBrowser.view(dir, path, request.protocolVersion());

                if (fileViewResult != null) {
                    response = fileViewResult;
                } else {
                    URL res = HttpTtyConnection.class.getResource("/com/taobao/arthas/core/http" + path);
                    if (res != null) {
                        DefaultFullHttpResponse fullResp = new DefaultFullHttpResponse(request.protocolVersion(),
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
                        response = fullResp;
                    } else {
                        response.setStatus(HttpResponseStatus.NOT_FOUND);
                    }
                }

            } catch (Throwable e) {
                logger.error("arthas process http request error: " + request.uri(), e);
            } finally {
                ctx.write(response);
                ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                future.addListener(ChannelFutureListener.CLOSE);
                IOUtils.close(in);
            }
        }
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Logging.logReportedIoError(cause);
        ctx.close();
    }
}
