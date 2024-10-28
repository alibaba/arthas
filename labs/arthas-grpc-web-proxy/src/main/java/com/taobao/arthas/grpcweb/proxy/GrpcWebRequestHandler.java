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

import com.taobao.arthas.common.Pair;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcWebRequestHandler {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());
    private final GrpcServiceConnectionManager grpcServiceConnectionManager;

    public GrpcWebRequestHandler(GrpcServiceConnectionManager g) {
        grpcServiceConnectionManager = g;
    }

    public void handle(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 处理 CORS OPTIONS 请求
        if (req.method().equals(HttpMethod.OPTIONS)) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            CorsUtils.updateCorsHeader(response.headers());
            ctx.writeAndFlush(response);
            return;
        }

        String contentTypeStr = req.headers().get(HttpHeaderNames.CONTENT_TYPE);

        MessageUtils.ContentType contentType = MessageUtils.validateContentType(contentTypeStr);
        SendGrpcWebResponse sendResponse = new SendGrpcWebResponse(ctx, req);

        try {
            // From the request, get the rpc-method name and class name and then get their
            // corresponding
            // concrete objects.
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
            String pathInfo = queryStringDecoder.path();

            Pair<String, String> classAndMethodNames = getClassAndMethod(pathInfo);
            String className = classAndMethodNames.getFirst();
            String methodName = classAndMethodNames.getSecond();
            Class cls = getClassObject(className);
            if (cls == null) {
                logger.error("cannot find service impl in the request, className: " + className);
                // incorrect classname specified in the request.
                sendResponse.returnUnimplementedStatusCode(className);
                return;
            }

            // Create a ClientInterceptor object
            CountDownLatch latch = new CountDownLatch(1);
            GrpcWebClientInterceptor interceptor = new GrpcWebClientInterceptor(latch, sendResponse);
            Channel channel = grpcServiceConnectionManager.getChannelWithClientInterceptor(interceptor);

            // get the stub for the rpc call and the method to be called within the stub
            io.grpc.stub.AbstractStub asyncStub = getRpcStub(channel, cls, "newStub");
            Metadata headers = MetadataUtil.getHtpHeaders(req.headers());
            if (!headers.keys().isEmpty()) {
                asyncStub = MetadataUtils.attachHeaders(asyncStub, headers);
            }
            Method asyncStubCall = getRpcMethod(asyncStub, methodName);
            // Get the input object bytes
            ByteBuf content = req.content();
            InputStream in = new ByteBufInputStream(content);
            MessageDeframer deframer = new MessageDeframer();
            Object inObj = null;
            if (deframer.processInput(in, contentType)) {
                inObj = MessageUtils.getInputProtobufObj(asyncStubCall, deframer.getMessageBytes());
            }
            ManagedChannel managedChannel = grpcServiceConnectionManager.getChannel();
            // Invoke the rpc call
            asyncStubCall.invoke(asyncStub, inObj, new GrpcCallResponseReceiver(sendResponse, latch,managedChannel));
            if (!latch.await( 1000, TimeUnit.MILLISECONDS)) {
                logger.warn("grpc call took too long!");
            }
        } catch (Exception e) {
            logger.error("try to invoke grpc serivce error, uri: {}", req.uri(), e);
            sendResponse.writeError(Status.UNAVAILABLE.withCause(e));
        }
    }

    private Pair<String, String> getClassAndMethod(String pathInfo) throws IllegalArgumentException {
        // pathInfo starts with "/". ignore that first char.
        String[] rpcClassAndMethodTokens = pathInfo.substring(1).split("/");
        if (rpcClassAndMethodTokens.length != 2) {
            throw new IllegalArgumentException("incorrect pathinfo: " + pathInfo);
        }

        String rpcClassName = rpcClassAndMethodTokens[0];
        String rpcMethodNameRecvd = rpcClassAndMethodTokens[1];
        String rpcMethodName = rpcMethodNameRecvd.substring(0, 1).toLowerCase() + rpcMethodNameRecvd.substring(1);
        return new Pair<>(rpcClassName, rpcMethodName);
    }

    private Class<?> getClassObject(String className) {
        Class rpcClass = null;
        try {
            rpcClass = Class.forName(className + "Grpc");
        } catch (ClassNotFoundException e) {
            logger.info("no such class " + className);
        }
        return rpcClass;
    }

    private io.grpc.stub.AbstractStub getRpcStub(Channel ch, Class cls, String stubName) {
        try {
            Method m = cls.getDeclaredMethod(stubName, io.grpc.Channel.class);
            return (io.grpc.stub.AbstractStub) m.invoke(null, ch);
        } catch (Exception e) {
            logger.warn("Error when fetching " + stubName + " for: " + cls.getName());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Find the matching method in the stub class.
     */
    private Method getRpcMethod(Object stub, String rpcMethodName) {
        for (Method m : stub.getClass().getMethods()) {
            if (m.getName().equals(rpcMethodName)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Couldn't find rpcmethod: " + rpcMethodName);
    }

    private static class GrpcCallResponseReceiver<Object> implements StreamObserver {
        private final SendGrpcWebResponse sendResponse;
        private final CountDownLatch latch;

        private final ManagedChannel channel;

        GrpcCallResponseReceiver(SendGrpcWebResponse s, CountDownLatch c, ManagedChannel channel) {
            sendResponse = s;
            latch = c;
            this.channel = channel;
        }

        @Override
        public void onNext(java.lang.Object resp) {
            // TODO verify that the resp object is of Class instance returnedCls.
            byte[] outB = ((com.google.protobuf.GeneratedMessageV3) resp).toByteArray();
            if(!sendResponse.writeResponse(outB)){
                // 这里需要断开grpc
                this.channel.shutdownNow();
                logger.error("Grpc shutdown from grpc web proxy client");
            }
        }

        @Override
        public void onError(Throwable t) {
            Status s = Status.fromThrowable(t);
            sendResponse.writeError(s);
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            sendResponse.writeTrailer(Status.OK, null);
            latch.countDown();
        }
    }
}
