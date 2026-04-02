package com.taobao.arthas.grpc.server;

/**
 * Arthas gRPC 服务器启动类
 *
 * 这是 Arthas gRPC 服务器的入口类，负责启动和初始化 gRPC 服务器。
 * 该类创建服务器实例并启动监听指定的端口，默认端口为 9091。
 *
 * @author: FengYe
 * @date: 2024/10/13 02:40
 * @description: ArthasGrpcServerBootstrap
 */
public class ArthasGrpcBootstrap {

    /**
     * 主入口方法
     *
     * 该方法是整个 Arthas gRPC 服务器的启动入口。
     * 它创建一个 ArthasGrpcServer 实例，并启动服务器。
     *
     * @param args 命令行参数（当前未使用）
     */
    public static void main(String[] args) {
        // 创建 Arthas gRPC 服务器实例
        // 参数1: 9091 - 服务器监听的端口号
        // 参数2: null - gRPC 服务包名（null 表示不扫描特定包）
        ArthasGrpcServer arthasGrpcServer = new ArthasGrpcServer(9091, null);

        // 启动 gRPC 服务器，开始监听客户端请求
        arthasGrpcServer.start();
    }
}
