package com.taobao.arthas.grpcweb.proxy.server.grpcService;

import grpc.gateway.testing.Echo.*;
import grpc.gateway.testing.EchoServiceGrpc.EchoServiceImplBase;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class EchoImpl extends EchoServiceImplBase {

    @Override
    public void echo(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        String message = request.getMessage();
        responseObserver.onNext(EchoResponse.newBuilder().setMessage(message).setMessageCount(1).build());
        responseObserver.onCompleted();
    }

    @Override
    public void echoAbort(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {
        // TODO Auto-generated method stub
        
        responseObserver.onNext(EchoResponse.newBuilder().setMessage(request.getMessage()).build());
        Metadata trailers = new Metadata();
        Key<String> customKey = Key.of("custom-key", Metadata.ASCII_STRING_MARSHALLER);
        // 添加自定义元数据
        trailers.put(customKey, "custom-value");
        responseObserver.onError(Status.ABORTED.withDescription("error desc").asException(trailers));
    }

    @Override
    public void noOp(Empty request, StreamObserver<Empty> responseObserver) {
        // TODO Auto-generated method stub
        super.noOp(request, responseObserver);
    }

    @Override
    public void serverStreamingEcho(ServerStreamingEchoRequest request,
            StreamObserver<ServerStreamingEchoResponse> responseObserver) {

        String message = request.getMessage();

        int messageCount = request.getMessageCount();

        System.err.println(message);

        for (int i = 0; i < messageCount; ++i) {
            responseObserver.onNext(ServerStreamingEchoResponse.newBuilder().setMessage(message).build());
        }

        responseObserver.onCompleted();

    }

    @Override
    public void serverStreamingEchoAbort(ServerStreamingEchoRequest request,
            StreamObserver<ServerStreamingEchoResponse> responseObserver) {
        // TODO Auto-generated method stub
        super.serverStreamingEchoAbort(request, responseObserver);
    }

    @Override
    public StreamObserver<ClientStreamingEchoRequest> clientStreamingEcho(
            StreamObserver<ClientStreamingEchoResponse> responseObserver) {
        // TODO Auto-generated method stub
        return super.clientStreamingEcho(responseObserver);
    }

    @Override
    public StreamObserver<EchoRequest> fullDuplexEcho(StreamObserver<EchoResponse> responseObserver) {
        // TODO Auto-generated method stub
        return super.fullDuplexEcho(responseObserver);
    }

    @Override
    public StreamObserver<EchoRequest> halfDuplexEcho(StreamObserver<EchoResponse> responseObserver) {
        // TODO Auto-generated method stub
        return super.halfDuplexEcho(responseObserver);
    }

}
