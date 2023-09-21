package com.taobao.arthas.grpcweb.proxy.server;

public class StartGrpcWebProxyTest {

    private int GRPC_WEB_PROXY_PORT;

    private int GRPC_PORT;


    public StartGrpcWebProxyTest(int grpcWebPort, int grpcPort){
        this.GRPC_WEB_PROXY_PORT = grpcWebPort;
        this.GRPC_PORT = grpcPort;
    }

    public void startGrpcWebProxy(){
        GrpcWebProxyServer grpcWebProxyServer = new GrpcWebProxyServer(GRPC_WEB_PROXY_PORT, GRPC_PORT);
        grpcWebProxyServer.start();
    }
}
