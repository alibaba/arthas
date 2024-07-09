package com.taobao.arthas.h2;/**
 * @author: 風楪
 * @date: 2024/7/7 下午9:58
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

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
            System.out.println(headersFrame.headers().path().toString());

//            // Respond to the client with headers
//            Http2Headers responseHeaders = new DefaultHttp2Headers()
//                    .status("200")
//                    .set("content-type", "text/plain; charset=UTF-8");
//
//            // 创建响应数据
//            byte[] content = "Hello, HTTP/2 World!".getBytes();
//            Http2DataFrame dataFrame = new DefaultHttp2DataFrame(ctx.alloc().buffer().writeBytes(content), true);
//
//            // 发送响应头
//            ctx.write(new DefaultHttp2HeadersFrame(responseHeaders).stream(headersFrame.stream()));
//
//            // 发送响应数据
//            ctx.writeAndFlush(dataFrame.stream(headersFrame.stream()));

        } else if (frame instanceof Http2DataFrame) {
            Http2DataFrame dataFrame = (Http2DataFrame) frame;
            System.out.println("Received data: " + dataFrame.content().toString(CharsetUtil.UTF_8));
            ByteBuf content = dataFrame.content();
            byte[] byteArray = new byte[content.readableBytes()];
            content.readBytes(byteArray);


            try {
                Object o = decompressAndDeserialize(byteArray);
                System.out.println(o);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

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

    public static Object decompressAndDeserialize(byte[] compressedData) throws IOException, ClassNotFoundException {
        // 解压缩数据
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);

        // 反序列化对象
        Object deserializedObject = objectInputStream.readObject();

        // 关闭流
        objectInputStream.close();
        gzipInputStream.close();
        byteArrayInputStream.close();

        return deserializedObject;
    }
}