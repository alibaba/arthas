package com.taobao.arthas.grpcweb.proxy.server;

import com.taobao.arthas.grpcweb.proxy.server.grpcService.EchoImpl;
import com.taobao.arthas.grpcweb.proxy.server.grpcService.GreeterService;
import com.taobao.arthas.grpcweb.proxy.server.grpcService.HelloImpl;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class startGrpcTest {

    static final int GRPC_PORT = 50051;

    public static void main(String[] args) {
        try {
            Server grpcServer = ServerBuilder.forPort(GRPC_PORT).addService((BindableService) new GreeterService())
                    .addService((BindableService) new HelloImpl()).addService(new EchoImpl()).build();
            grpcServer.start();
            System.out.println("started gRPC server on port # " + GRPC_PORT);
            System.in.read();
        } catch (IOException e) {
            System.out.println("fail to start gRPC server");
            throw new RuntimeException(e);
        }
    }

}
