package com.taobao.arthas.h2;/**
 * @author: 風楪
 * @date: 2024/7/7 下午9:58
 */

import com.taobao.arthas.protobuf.ProtobufCodec;
import com.taobao.arthas.protobuf.ProtobufProxy;
import com.taobao.arthas.service.req.ArthasSampleRequest;
import com.taobao.arthas.service.res.ArthasSampleResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * @author: FengYe
 * @date: 2024/7/7 下午9:58
 * @description: Http2Handler
 */
public class Http2Handler extends SimpleChannelInboundHandler<Http2Frame> {

    /**
     * 暂存收到的所有请求的数据
     */
    private ConcurrentHashMap<Integer, ByteBuf> dataBuffer = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) throws IOException {
        if (frame instanceof Http2SettingsFrame) {

        } else if (frame instanceof Http2HeadersFrame) {
            handleGrpcRequest((Http2HeadersFrame) frame, ctx);
        } else if (frame instanceof Http2DataFrame) {
            handleGrpcData((Http2DataFrame) frame, ctx);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void handleGrpcRequest(Http2HeadersFrame headersFrame, ChannelHandlerContext ctx) {
        int id = headersFrame.stream().id();
        dataBuffer.put(id, ctx.alloc().buffer());
        System.out.println("Received headers: " + headersFrame.headers());
    }

    private void handleGrpcData(Http2DataFrame dataFrame, ChannelHandlerContext ctx) throws IOException {
        byte[] data = new byte[dataFrame.content().readableBytes()];
        dataFrame.content().readBytes(data);

//          Decompress if needed
        byte[] decompressedData = decompressGzip(data);
        ByteBuf byteBuf = dataBuffer.get(dataFrame.stream().id());
        byteBuf.writeBytes(decompressedData);

        if (dataFrame.isEndStream()) {

            boolean b = byteBuf.readBoolean();
            int length = byteBuf.readInt();
            System.out.println(b);
            System.out.println(length);

            byte[] byteArray = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(byteArray);
            ProtobufCodec<ArthasSampleRequest> requestCodec = ProtobufProxy.create(ArthasSampleRequest.class);
            ProtobufCodec<ArthasSampleResponse> responseCodec = ProtobufProxy.create(ArthasSampleResponse.class);

            ArthasSampleRequest decode = requestCodec.decode(byteArray);

            System.out.println(decode);

            ArthasSampleResponse arthasSampleResponse = new ArthasSampleResponse();
            arthasSampleResponse.setMessage("Hello ArthasSample!");
            byte[] responseData = responseCodec.encode(arthasSampleResponse);



            Http2Headers endHeader = new DefaultHttp2Headers()
                    .status("200")
                    .set("content-type", "application/grpc")
                    .set("grpc-encoding", "identity")
                    .set("grpc-accept-encoding", "identity,deflate,gzip");
            ctx.write(new DefaultHttp2HeadersFrame(endHeader).stream(dataFrame.stream()));

            ByteBuf buffer = ctx.alloc().buffer();
            buffer.writeBoolean(false);
            buffer.writeInt(responseData.length);
            buffer.writeBytes(responseData);
            System.out.println(responseData.length);
            DefaultHttp2DataFrame resDataFrame = new DefaultHttp2DataFrame(buffer).stream(dataFrame.stream());
            ctx.write(resDataFrame);


            Http2Headers endStream = new DefaultHttp2Headers()
                    .set("grpc-status", "0");
            DefaultHttp2HeadersFrame endStreamFrame = new DefaultHttp2HeadersFrame(endStream, true).stream(dataFrame.stream());
            ctx.writeAndFlush(endStreamFrame);
        } else {

        }
    }


    private static byte[] decompressGzip(byte[] compressedData) throws IOException {
        boolean isGzip = (compressedData.length > 2 && (compressedData[0] & 0xff) == 0x1f && (compressedData[1] & 0xff) == 0x8b);
        if (isGzip) {
            try {
                InputStream byteStream = new ByteArrayInputStream(compressedData);
                GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                byte[] buffer = new byte[1024];
                int len;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((len = gzipStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return out.toByteArray();
            } catch (IOException e) {
                System.err.println("Failed to decompress GZIP data: " + e.getMessage());
            }
            return null;
        } else {
            return compressedData;
        }
    }
}