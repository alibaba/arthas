package com.taobao.arthas.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

import com.taobao.arthas.grpc.server.temp.TempImpl;

/**
 * @author: 風楪
 * @date: 2024/6/30 上午1:22
 */
public class GrpcTest {

    private static Server server;

    public static void main(String[] args) throws Throwable {
        start();
        blockUntilShutdown();
    }
    public static void start() throws IOException {
        ServerBuilder builder = ServerBuilder.forPort(9090)
                .addService(new TempImpl());
        server = builder.build();
        server.start();
    }

    public static void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }
}