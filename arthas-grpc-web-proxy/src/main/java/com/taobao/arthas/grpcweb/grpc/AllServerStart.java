package com.taobao.arthas.grpcweb.grpc;

import com.taobao.arthas.grpcweb.grpc.objectUtils.ComplexObject;
import com.taobao.arthas.grpcweb.grpc.server.GrpcServer;
import com.taobao.arthas.grpcweb.grpc.server.httpServer.NettyHttpServer;
import com.taobao.arthas.grpcweb.proxy.server.GrpcWebProxyServer;

import static com.taobao.arthas.grpcweb.grpc.Test.ccc;

public class AllServerStart {

    public static void main(String[] args) throws InterruptedException {
        // 构造这个复杂对象
        ComplexObject ccc = ccc();

        GrpcServer grpcServer = new GrpcServer(8566);
        grpcServer.start();
        // 启动grpc-web-proxy服务
        Thread grpcWebProxyStart = new Thread(() -> {
            GrpcWebProxyServer grpcWebProxyServer = new GrpcWebProxyServer(8567,8566);
            grpcWebProxyServer.start();
        });
        grpcWebProxyStart.start();
        System.out.println("正在启动http服务器");
        NettyHttpServer nettyHttpServer = new NettyHttpServer(8000);
        nettyHttpServer.start();
    }
}
