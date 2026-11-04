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

import io.grpc.*;
import io.grpc.ClientCall.Listener;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;

import java.util.concurrent.CountDownLatch;

class GrpcWebClientInterceptor implements ClientInterceptor {

    private final CountDownLatch latch;
    private final SendGrpcWebResponse sendResponse;

    GrpcWebClientInterceptor(CountDownLatch latch, SendGrpcWebResponse send) {
        this.latch = latch;
        sendResponse = send;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions, Channel channel) {
        return new SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new MetadataResponseListener<RespT>(responseListener), headers);
            }
        };
    }

    class MetadataResponseListener<T> extends SimpleForwardingClientCallListener<T> {
        private boolean headersSent = false;

        MetadataResponseListener(Listener<T> responseListener) {
            super(responseListener);
        }

        @Override
        public void onHeaders(Metadata h) {
            sendResponse.writeHeaders(h);
            headersSent = true;
        }

        @Override
        public void onClose(Status s, Metadata t) {
            // TODO 这个函数会在 onCompleted 之前回调，这里有点奇怪
            if (!headersSent) {
                // seems, sometimes onHeaders() is not called before this method is called!
                // so far, they are the error cases. let onError() method in ClientListener
                // handle this call. Could ignore this.
                // TODO is this correct? what if onError() never gets called?
            } else {
                sendResponse.writeTrailer(s, t);
                latch.countDown();
            }
            super.onClose(s, t);
        }
    }
}
