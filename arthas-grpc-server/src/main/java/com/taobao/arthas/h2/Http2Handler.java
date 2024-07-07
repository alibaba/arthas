package com.taobao.arthas.h2;/**
 * @author: 風楪
 * @date: 2024/7/7 下午9:58
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;
import io.netty.util.CharsetUtil;

/**
 * @author: FengYe
 * @date: 2024/7/7 下午9:58
 * @description: Http2Handler
 */
public class Http2Handler extends SimpleChannelInboundHandler<Http2Frame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) {
        if (frame instanceof Http2HeadersFrame) {
            Http2HeadersFrame headersFrame = (Http2HeadersFrame) frame;
            System.out.println("Received headers: " + headersFrame.headers());

            // Respond to the client with headers
            Http2Headers headers = new DefaultHttp2Headers().status("200");
            ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers).stream(((Http2HeadersFrame) frame).stream()));

        } else if (frame instanceof Http2DataFrame) {
            Http2DataFrame dataFrame = (Http2DataFrame) frame;
            System.out.println("Received data: " + dataFrame.content().toString(CharsetUtil.UTF_8));
            dataFrame.release();

            // Respond to the client with data
            Http2DataFrame responseData = new DefaultHttp2DataFrame(ctx.alloc().buffer().writeBytes("Hello, HTTP/2".getBytes(CharsetUtil.UTF_8)), true)
                    .stream(((Http2HeadersFrame) frame).stream());
            ctx.writeAndFlush(responseData);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}