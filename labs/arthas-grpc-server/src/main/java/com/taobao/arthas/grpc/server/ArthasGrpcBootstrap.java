package com.taobao.arthas.grpc.server;

/**
 * @author: FengYe
 * @date: 2024/10/13 02:40
 * @description: ArthasGrpcServerBootstrap
 */
public class ArthasGrpcBootstrap {
    public static void main(String[] args) {
        ArthasGrpcServer arthasGrpcServer = new ArthasGrpcServer(9091, null);
        arthasGrpcServer.start();
    }
}
