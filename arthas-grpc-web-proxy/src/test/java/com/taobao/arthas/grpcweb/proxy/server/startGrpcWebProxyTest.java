package com.taobao.arthas.grpcweb.proxy.server;

public class startGrpcWebProxyTest {

    static final int GRPC_WEB_PORT = 8080;

    public static void main(String[] args) {
        GrpcWebProxyServer grpcWebProxyServer = new GrpcWebProxyServer(GRPC_WEB_PORT, 50051);
        grpcWebProxyServer.start();
    }
}
