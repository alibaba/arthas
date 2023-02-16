package com.alibaba.arthas.tunnel.client;

import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.taobao.arthas.common.ArthasConstants;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author hengyunabc 2020-10-22
 *
 */
public class ProxyClient {
    private static final Logger logger = LoggerFactory.getLogger(ProxyClient.class);

    public SimpleHttpResponse query(String targetUrl) throws InterruptedException {
        final Promise<SimpleHttpResponse> httpResponsePromise = GlobalEventExecutor.INSTANCE.newPromise();

        final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-ProxyClient", true));
        ChannelFuture closeFuture = null;
        try {
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            b.group(group).channel(LocalChannel.class).handler(new ChannelInitializer<LocalChannel>() {
                @Override
                protected void initChannel(LocalChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH),
                            new HttpProxyClientHandler(httpResponsePromise));
                }
            });

            LocalAddress localAddress = new LocalAddress(ArthasConstants.NETTY_LOCAL_ADDRESS);
            Channel localChannel = b.connect(localAddress).sync().channel();

            // Prepare the HTTP request.
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, targetUrl,
                    Unpooled.EMPTY_BUFFER);
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            localChannel.writeAndFlush(request);

            closeFuture = localChannel.closeFuture();
            logger.info("proxy client connect to server success, targetUrl: " + targetUrl);

            return httpResponsePromise.get(5000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            logger.error("ProxyClient error, targetUrl: {}", targetUrl, e);
        } finally {
            if (closeFuture != null) {
                closeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        group.shutdownGracefully();
                    }
                });
            } else {
                group.shutdownGracefully();
            }
        }

        SimpleHttpResponse httpResponse = new SimpleHttpResponse();
        try {
            httpResponse.setContent("error".getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        return httpResponse;
    }

    public SimpleHttpResponse apiRequest(String apiRequest) throws UnsupportedEncodingException {
        // Prepare the HTTP request.
        byte[] bytes = apiRequest.getBytes("UTF-8");
        logger.info("send apiRequest: {}, bytes length: {}", apiRequest, bytes.length);
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/api",
                Unpooled.wrappedBuffer(bytes));
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, bytes.length);

        return request(request);
    }

    private SimpleHttpResponse request(HttpRequest request) {
        final Promise<SimpleHttpResponse> httpResponsePromise = GlobalEventExecutor.INSTANCE.newPromise();

        final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-proxyClient", true));
        ChannelFuture closeFuture = null;
        try {
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH),
                            new HttpProxyClientHandler(httpResponsePromise));
                }
            });

//            LocalAddress localAddress = new LocalAddress(ArthasConstants.NETTY_LOCAL_ADDRESS);
            Channel localChannel = b.connect("127.0.0.1", 8563).sync().channel();

            localChannel.writeAndFlush(request);

            closeFuture = localChannel.closeFuture();
            logger.info("proxy client connect to server success");

            return httpResponsePromise.get(5000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            logger.error("ProxyClient error", e);
        } finally {
            if (closeFuture != null) {
                closeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        group.shutdownGracefully();
                    }
                });
            } else {
                group.shutdownGracefully();
            }
        }

        SimpleHttpResponse httpResponse = new SimpleHttpResponse();
        try {
            httpResponse.setContent("error".getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            // ignore
        }
        return httpResponse;
    }

    static class HttpProxyClientHandler extends SimpleChannelInboundHandler<HttpObject> {

        private final Promise<SimpleHttpResponse> promise;

        private final SimpleHttpResponse simpleHttpResponse = new SimpleHttpResponse();

        public HttpProxyClientHandler(Promise<SimpleHttpResponse> promise) {
            this.promise = promise;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;

                simpleHttpResponse.setStatus(response.status().code());
                if (!response.headers().isEmpty()) {
                    for (String name : response.headers().names()) {
                        for (String value : response.headers().getAll(name)) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("header: {}, value: {}", name, value);
                            }

                            simpleHttpResponse.addHeader(name, value);
                        }
                    }
                }
            }
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;

                ByteBuf byteBuf = content.content();
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);

                simpleHttpResponse.setContent(bytes);

                promise.setSuccess(simpleHttpResponse);

                if (content instanceof LastHttpContent) {
                    ctx.close();
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Proxy Client error", cause);
            ctx.close();
        }
    }
}
