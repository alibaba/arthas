package com.taobao.arthas.grpcweb.proxy.server;

import com.taobao.arthas.grpcweb.proxy.server.grpcService.EchoImpl;
import com.taobao.arthas.grpcweb.proxy.server.grpcService.GreeterService;
import com.taobao.arthas.grpcweb.proxy.server.grpcService.HelloImpl;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class GrpcWebProxyServerTest {

    static final int GRPC_PORT = 50051;

    static final int GRPC_WEB_PORT = 8080;

    private GrpcWebProxyServer grpcWebProxyServer;


    @Test
    public void startGrpcWebProxy(){
        try {
            Server grpcServer = ServerBuilder.forPort(GRPC_PORT).addService((BindableService) new GreeterService())
                    .addService((BindableService) new HelloImpl()).addService(new EchoImpl()).build();
            grpcServer.start();
            System.out.println("started gRPC server on port # " + GRPC_PORT);
        } catch (IOException e) {
            System.out.println("fail to start gRPC server");
            throw new RuntimeException(e);
        }

        grpcWebProxyServer = new GrpcWebProxyServer(GRPC_WEB_PORT, GRPC_PORT);
        grpcWebProxyServer.start();
    }

}
