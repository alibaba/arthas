package com.taobao.arthas.grpcweb.proxy.server.grpcService;

import helloworld.GreeterGrpc.GreeterImplBase;
import helloworld.Helloworld.HelloReply;
import helloworld.Helloworld.HelloRequest;
import helloworld.Helloworld.RepeatHelloRequest;
import io.grpc.stub.StreamObserver;

public class HelloImpl extends GreeterImplBase{

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        // TODO Auto-generated method stub
//        super.sayHello(request, responseObserver);
        
        System.err.println("sayHello");
        
//        throw new RuntimeException("eeee");
        
        responseObserver.onNext(HelloReply.newBuilder().setMessage("xxxx").build());
        
        responseObserver.onCompleted();
    }

    @Override
    public void sayRepeatHello(RepeatHelloRequest request, StreamObserver<HelloReply> responseObserver) {
        // TODO Auto-generated method stub
//        super.sayRepeatHello(request, responseObserver);
        
        System.err.println("sayRepeatHello  eeee ");
        
//        throw new RuntimeException("eeee");
        
        responseObserver.onNext(HelloReply.newBuilder().setMessage("xxxx").build());
        
        responseObserver.onCompleted();
    }

    
}
