/*
 * Copyright 2020  Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taobao.arthas.grpcweb.proxy;

import com.taobao.arthas.grpcweb.proxy.MessageUtils.ContentType;
import io.grpc.Metadata;
import io.grpc.Status;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedStream;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Base64;
import java.util.Map;

/**
 * <pre>
 * * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md
 * * https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md
 * 
 * 据协议和抓包分析，grpc-web 回应需要以 HTTP chunk数据包，包装 grpc 本身的数据。
 * 
 * grpc-web 的 http1.1 Response 由三部分组成：
 * 1. headers , 返回 status 总是 200
 * 2. data chunk ，可能多个
 * 3. trailer chunk , grpc的 grpc-status, grpc-message 在这里
 * 
 * </pre>
 * 
 * @author hengyunabc 2023-09-06
 *
 */
class SendGrpcWebResponse {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private final String contentType;

    /**
     * 回应的 http1.1 header 是否已发送
     */
    private boolean isHeaderSent = false;

    /**
     * 所有的 grpc message 都会转换为一个 HTTP Chunk，所有的 Chunk 发送完之后，需要发送一个空的 Chunk 结束
     */
    private boolean isEndChunkSent = false;

    /**
     * 在 grpc 协议里，在发送完 DATA 后，最后可能发送一个 trailer，它也需要转换为 HTTP Chunk
     */
    private boolean isTrailerSent = false;

    /**
     * 客户端主动断开连接后,需要断开相应的grpc连接, grpc服务端才能停止监听
     */
    private Boolean isSuccessSendData = true;

    private ChannelHandlerContext ctx;

    SendGrpcWebResponse(ChannelHandlerContext ctx, FullHttpRequest req) {
        HttpHeaders headers = req.headers();
        contentType = headers.get(HttpHeaderNames.CONTENT_TYPE);
        this.ctx = ctx;
    }

    synchronized void writeHeaders(Metadata headers) {
        if (isHeaderSent) {
            return;
        }
        // 发送 http1.1 开头部分的内容
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType).set(HttpHeaderNames.TRANSFER_ENCODING,
                "chunked");

        CorsUtils.updateCorsHeader(response.headers());

        if (headers != null) {
            Map<String, String> ht = MetadataUtil.getHttpHeadersFromMetadata(headers);
            for (String key : ht.keySet()) {
                response.headers().set(key, ht.get(key));
            }
        }

        logger.debug("write headers: {}", response);

        ctx.writeAndFlush(response);

        isHeaderSent = true;
    }

    synchronized void returnUnimplementedStatusCode(String className) {
        writeHeaders(null);
        writeTrailer(
                Status.UNIMPLEMENTED.withDescription("Can not find service impl, check dep, service: " + className),
                null);
    }

    // 发送最后的 http chunked 空块
    private void writeEndChunk() {
        if (isEndChunkSent) {
            return;
        }
        LastHttpContent end = new DefaultLastHttpContent();
        ctx.writeAndFlush(end);
        isEndChunkSent = true;
    }

    synchronized void writeError(Status s) {
        writeHeaders(null);
        writeTrailer(s, null);
    }

    synchronized void writeTrailer(Status status, Metadata trailer) {
        if (isTrailerSent) {
            return;
        }
        StringBuffer sb = new StringBuffer();
        if (trailer != null) {
            Map<String, String> ht = MetadataUtil.getHttpHeadersFromMetadata(trailer);
            for (String key : ht.keySet()) {
                sb.append(String.format("%s:%s\r\n", key, ht.get(key)));
            }
        }
        sb.append(String.format("grpc-status:%d\r\n", status.getCode().value()));
        if (status.getDescription() != null && !status.getDescription().isEmpty()) {
            sb.append(String.format("grpc-message:%s\r\n", status.getDescription()));
        }

        writeResponse(sb.toString().getBytes(), MessageFramer.Type.TRAILER);

        isTrailerSent = true;

        writeEndChunk();
    }

    synchronized boolean writeResponse(byte[] out) {
        return writeResponse(out, MessageFramer.Type.DATA);
    }

    private boolean writeResponse(byte[] out, MessageFramer.Type type) {
        if (isTrailerSent) {
            logger.error("grpcweb trailer sented, writeResponse can not be called, framer type: {}", type);
            return false;
        }

        try {
            // PUNT multiple frames not handled
            byte[] prefix = new MessageFramer().getPrefix(out, type);
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            // binary encode if it is "text" content type
            if (MessageUtils.getContentType(contentType) == ContentType.GRPC_WEB_TEXT) {
                byte[] concated = new byte[out.length + 5];
                System.arraycopy(prefix, 0, concated, 0, 5);
                System.arraycopy(out, 0, concated, 5, out.length);
                oStream.write(Base64.getEncoder().encode(concated));
            } else {
                oStream.write(prefix);
                oStream.write(out);
            }

            byte[] byteArray = oStream.toByteArray();

            InputStream dataStream = new ByteArrayInputStream(byteArray);
            ChunkedStream chunkedStream = new ChunkedStream(dataStream);
            SingleHttpChunkedInput httpChunkedInput = new SingleHttpChunkedInput(chunkedStream);
            ChannelFuture channelFuture = ctx.writeAndFlush(httpChunkedInput);
            ChannelFutureListener channelFutureListener = new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (!future.isSuccess()) {
                        // 写入操作失败
                        isSuccessSendData = false;
                    }
                }
            };
            channelFuture.addListener(channelFutureListener);
            return isSuccessSendData;

        } catch (IOException e) {
            logger.error("write grpcweb response error, framer type: {}", type, e);
            return false;
        }
    }

}
