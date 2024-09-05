package com.taobao.arthas.grpc.server.handler;/**
 * @author: 風楪
 * @date: 2024/7/7 下午9:58
 */

import com.taobao.arthas.grpc.server.protobuf.ProtobufCodec;
import com.taobao.arthas.grpc.server.protobuf.ProtobufProxy;
import com.taobao.arthas.grpc.server.service.req.ArthasSampleRequest;
import com.taobao.arthas.grpc.server.service.res.ArthasSampleResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: FengYe
 * @date: 2024/7/7 下午9:58
 * @description: Http2Handler
 */
public class Http2Handler extends SimpleChannelInboundHandler<Http2Frame> {

    /**
     * 暂存收到的所有请求的数据
     */
    private ConcurrentHashMap<Integer, GrpcRequest> dataBuffer = new ConcurrentHashMap<>();

    private static final String HEADER_PATH = ":path";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Frame frame) throws IOException {
        if (frame instanceof Http2HeadersFrame) {
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
        String path = headersFrame.headers().get(HEADER_PATH).toString();
        // 去掉前面的斜杠，然后按斜杠分割
        String[] parts = path.substring(1).split("/");
        dataBuffer.put(id, new GrpcRequest(headersFrame.stream().id(), parts[0], parts[1]));
        System.out.println("Received headers: " + headersFrame.headers());
    }

    private void handleGrpcData(Http2DataFrame dataFrame, ChannelHandlerContext ctx) throws IOException {
        GrpcRequest grpcRequest = dataBuffer.get(dataFrame.stream().id());
        grpcRequest.writeData(dataFrame.content());

        if (dataFrame.isEndStream()) {


            try {

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            byte[] bytes = grpcRequest.readData();
            ProtobufCodec<ArthasSampleRequest> requestCodec = ProtobufProxy.create(ArthasSampleRequest.class);
            ProtobufCodec<ArthasSampleResponse> responseCodec = ProtobufProxy.create(ArthasSampleResponse.class);

            ArthasSampleRequest decode = requestCodec.decode(bytes);

            System.out.println(decode);

            ArthasSampleResponse arthasSampleResponse = new ArthasSampleResponse();
            arthasSampleResponse.setMessage("Hello ArthasSample!");
            byte[] responseData = responseCodec.encode(arthasSampleResponse);


            Http2Headers endHeader = new DefaultHttp2Headers()
                    .status("200");
//                    .set("content-type", "application/grpc")
//                    .set("grpc-encoding", "identity")
//                    .set("grpc-accept-encoding", "identity,deflate,gzip");
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
}