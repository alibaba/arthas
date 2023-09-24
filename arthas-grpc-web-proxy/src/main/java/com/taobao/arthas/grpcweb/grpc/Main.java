package com.taobao.arthas.grpcweb.grpc;

import com.taobao.arthas.grpcweb.grpc.server.GrpcServer;
import com.taobao.arthas.grpcweb.grpc.server.httpServer.NettyHttpServer;
import com.taobao.arthas.grpcweb.proxy.server.GrpcWebProxyServer;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        GrpcServer grpcServer = new GrpcServer(8566);
        grpcServer.start();
        // 启动grpc-web-proxy服务
        Thread grpcWebProxyStart = new Thread(() -> {
            GrpcWebProxyServer grpcWebProxyServer = new GrpcWebProxyServer(8567,8566);
            grpcWebProxyServer.start();
        });
        grpcWebProxyStart.start();
        NettyHttpServer nettyHttpServer = new NettyHttpServer(8000);
        nettyHttpServer.start();
    }
}
