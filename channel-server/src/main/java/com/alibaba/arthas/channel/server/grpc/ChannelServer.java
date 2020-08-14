package com.alibaba.arthas.channel.server.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author gongdewei 2020/8/14
 */
public class ChannelServer {

    @Autowired
    private ArthasServiceGrpcImpl arthasServiceGrpc;

    @Autowired
    private ScheduledExecutorService executorService;

    private Server server;

    public void start() throws Exception  {

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (server == null) {
                        server = ServerBuilder.forPort(7700)
                                .addService(arthasServiceGrpc)
                                //enable server-reflect
                                //.addService(ProtoReflectionService.newInstance())
                                .build();

                        server.start();
                        //server.awaitTermination();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }
}
