package com.alibaba.arthas.tunnel.client;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.arthas.tunnel.common.SimpleHttpResponse;
import com.taobao.arthas.common.ArthasConstants;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

/**
 * 代理客户端类
 *
 * 该类用于通过本地通道向Arthas服务器发送HTTP请求并获取响应。
 * 主要功能是通过Netty的LocalChannel连接到本地的Arthas服务器，
 * 发送HTTP请求并接收响应内容。
 *
 * 工作流程：
 * 1. 创建Netty的NioEventLoopGroup用于处理IO事件
 * 2. 通过LocalChannel连接到本地Arthas服务器
 * 3. 构造HTTP请求并发送
 * 4. 通过Promise异步等待响应
 * 5. 解析响应并返回SimpleHttpResponse对象
 *
 * @author hengyunabc 2020-10-22
 *
 */
public class ProxyClient {
    /**
     * 日志记录器，用于记录代理客户端的运行状态和错误信息
     */
    private static final Logger logger = LoggerFactory.getLogger(ProxyClient.class);

    /**
     * 查询指定URL并返回HTTP响应
     *
     * 该方法创建一个临时的HTTP客户端，通过本地通道连接到Arthas服务器，
     * 发送HTTP GET请求并返回响应结果。
     *
     * @param targetUrl 目标URL路径，将被发送到Arthas服务器
     * @return HTTP响应对象，包含状态码、响应头和响应内容；如果发生错误则返回包含错误信息的响应
     * @throws InterruptedException 当等待响应时被中断
     */
    public SimpleHttpResponse query(String targetUrl) throws InterruptedException {
        // 创建一个Promise对象，用于异步获取HTTP响应结果
        final Promise<SimpleHttpResponse> httpResponsePromise = GlobalEventExecutor.INSTANCE.newPromise();

        // 创建事件循环组，使用单个线程处理IO事件，线程命名为"arthas-ProxyClient"
        final EventLoopGroup group = new NioEventLoopGroup(1, new DefaultThreadFactory("arthas-ProxyClient", true));
        ChannelFuture closeFuture = null;
        try {
            // 创建Netty Bootstrap对象，用于配置和启动客户端
            Bootstrap b = new Bootstrap();
            // 设置连接超时时间为5秒
            b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
            // 配置使用LocalChannel进行本地通信
            b.group(group).channel(LocalChannel.class).handler(new ChannelInitializer<LocalChannel>() {
                @Override
                protected void initChannel(LocalChannel ch) {
                    // 获取ChannelPipeline，用于添加处理器
                    ChannelPipeline p = ch.pipeline();
                    // 添加HTTP客户端编解码器，用于处理HTTP协议
                    // 添加HTTP对象聚合器，将HTTP消息聚合成完整的消息，最大内容长度由ArthasConstants定义
                    // 添加自定义的HTTP代理客户端处理器，用于处理响应
                    p.addLast(new HttpClientCodec(), new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH),
                            new HttpProxyClientHandler(httpResponsePromise));
                }
            });

            // 创建本地地址，使用Arthas定义的本地地址名称
            LocalAddress localAddress = new LocalAddress(ArthasConstants.NETTY_LOCAL_ADDRESS);
            // 连接到本地服务器，并同步等待连接完成
            Channel localChannel = b.connect(localAddress).sync().channel();

            // 准备HTTP请求对象，使用HTTP/1.1协议，GET方法
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, targetUrl,
                    Unpooled.EMPTY_BUFFER);
            // 设置Connection头为CLOSE，表示请求完成后关闭连接
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);

            // 将请求写入并发送到服务器
            localChannel.writeAndFlush(request);

            // 获取通道关闭的Future，用于在通道关闭后清理资源
            closeFuture = localChannel.closeFuture();
            logger.info("proxy client connect to server success, targetUrl: " + targetUrl);

            // 等待HTTP响应，超时时间为5秒
            return httpResponsePromise.get(5000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            // 捕获并记录查询过程中的错误
            logger.error("ProxyClient error, targetUrl: {}", targetUrl, e);
        } finally {
            // 清理资源：在通道关闭后优雅地关闭事件循环组
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

        // 如果发生错误，创建一个包含错误信息的响应对象
        SimpleHttpResponse httpResponse = new SimpleHttpResponse();
        try {
            // 设置响应内容为"error"字符串的UTF-8编码字节数组
            httpResponse.setContent("error".getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            // 忽略编码异常，UTF-8应该总是支持的
        }
        return httpResponse;
    }

    /**
     * HTTP代理客户端处理器
     *
     * 这是一个内部类，继承自SimpleChannelInboundHandler，专门用于处理HTTP响应。
     * 它负责接收服务器的HTTP响应，解析状态码、响应头和响应体，
     * 并将结果设置到Promise中，以便query方法可以获取响应结果。
     */
    static class HttpProxyClientHandler extends SimpleChannelInboundHandler<HttpObject> {

        /**
         * Promise对象，用于异步设置HTTP响应结果
         */
        private Promise<SimpleHttpResponse> promise;

        /**
         * 构造的HTTP响应对象，用于存储从服务器接收到的响应数据
         */
        private SimpleHttpResponse simpleHttpResponse = new SimpleHttpResponse();

        /**
         * 构造函数
         *
         * @param promise Promise对象，用于设置异步响应结果
         */
        public HttpProxyClientHandler(Promise<SimpleHttpResponse> promise) {
            this.promise = promise;
        }

        /**
         * 读取并处理HTTP响应消息
         *
         * 该方法会在接收到HTTP消息时被调用，它会分别处理HttpResponse和HttpContent。
         * HttpResponse包含状态码和响应头，HttpContent包含响应体内容。
         *
         * @param ctx ChannelHandlerContext，通道处理器上下文
         * @param msg 接收到的HTTP消息对象
         */
        @Override
        public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
            // 如果接收到的是HTTP响应（包含状态码和响应头）
            if (msg instanceof HttpResponse) {
                HttpResponse response = (HttpResponse) msg;

                // 设置响应状态码
                simpleHttpResponse.setStatus(response.status().code());
                // 如果响应头不为空，遍历所有响应头并添加到响应对象中
                if (!response.headers().isEmpty()) {
                    for (String name : response.headers().names()) {
                        for (String value : response.headers().getAll(name)) {
                            // 在调试模式下记录响应头信息
                            if (logger.isDebugEnabled()) {
                                logger.debug("header: {}, value: {}", name, value);
                            }

                            // 添加响应头到SimpleHttpResponse对象
                            simpleHttpResponse.addHeader(name, value);
                        }
                    }
                }
            }
            // 如果接收到的是HTTP内容（响应体）
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;

                ByteBuf byteBuf = null;
                try{
                    // 获取内容缓冲区
                    byteBuf = content.content();
                    // 创建字节数组，大小为可读字节数
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    // 将缓冲区内容读取到字节数组
                    byteBuf.readBytes(bytes);

                    // 设置响应内容
                    simpleHttpResponse.setContent(bytes);

                    // 设置Promise成功，将响应对象传递给等待的query方法
                    promise.setSuccess(simpleHttpResponse);

                    // 如果是最后一个HTTP内容块，关闭连接
                    if (content instanceof LastHttpContent) {
                        ctx.close();
                    }
                }finally {
                    // 释放ByteBuf资源，防止内存泄漏
                    if (byteBuf != null) {
                        byteBuf.release();
                    }
                }

            }
        }

        /**
         * 处理异常情况
         *
         * 当处理过程中发生异常时，记录错误日志并关闭通道。
         *
         * @param ctx ChannelHandlerContext，通道处理器上下文
         * @param cause 异常对象
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // 记录错误日志
            logger.error("Proxy Client error", cause);
            // 关闭通道
            ctx.close();
        }
    }
}
