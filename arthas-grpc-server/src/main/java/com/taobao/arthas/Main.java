package com.taobao.arthas;

import arthasSample.ArthasSample;
import com.taobao.arthas.protobuf.*;
import com.taobao.arthas.protobuf.utils.MiniTemplator;
import com.taobao.arthas.service.ArthasSampleService;
import com.taobao.arthas.service.impl.ArthasSampleServiceImpl;
import com.taobao.arthas.temp.TempImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author: 風楪
 * @date: 2024/6/30 上午1:22
 */
public class Main {

    private static final String TEMPLATE_FILE = "/class_template.tpl";

    private static Server server;

    public static void main(String[] args) throws Exception {
//        Main service = new Main();
//        service.start();
//        service.blockUntilShutdown();

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(1);
        buffer.writeBytes("hello".getBytes(StandardCharsets.UTF_8));

        buffer.readInt();
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);

        System.out.println(new String(bytes, StandardCharsets.UTF_8));
    }

    public static void start() throws IOException {
        ServerBuilder builder = ServerBuilder.forPort(9090)
                .addService(new TempImpl());
        server = builder.build();
        server.start();
    }

    public void blockUntilShutdown() throws InterruptedException {
        server.awaitTermination();
    }
}