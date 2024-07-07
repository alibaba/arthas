package com.taobao.arthas;

import com.taobao.arthas.service.impl.HelloServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;

/**
 * @author: 風楪
 * @date: 2024/6/30 上午1:22
 */
public class Main {
    public static void main(String[] args) {
        int port = 9090;
        Server server = ServerBuilder.forPort(port)
                .addService(new HelloServiceImpl())
                .build();
        ByteBuf buffer = Unpooled.buffer();
        try {
            server.start();
            System.out.println("Server started, listening on " + port);
            server.awaitTermination();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}