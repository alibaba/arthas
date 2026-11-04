/*
 * Copyright 2020 The gRPC Authors
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

package com.taobao.arthas.grpcweb.proxy.server.grpcService;

import grpcweb.examples.greeter.GreeterGrpc;
import grpcweb.examples.greeter.GreeterOuterClass.HelloReply;
import grpcweb.examples.greeter.GreeterOuterClass.HelloRequest;
import io.grpc.stub.StreamObserver;

public class GreeterService extends GreeterGrpc.GreeterImplBase {
    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
        System.out.println("Greeter Service responding in sayhello() method");

//    throw new RuntimeException("xxxxxx");
        HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
