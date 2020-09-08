package com.alibaba.arthas.channel.server.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * @author gongdewei 2020/8/14
 */
public class ChannelServer {

    private static final Logger logger = LoggerFactory.getLogger(ChannelServer.class);

    @Autowired
    private ArthasServiceGrpcImpl arthasServiceGrpc;

    private Server server;
    private int port = 7700;

    public void start() throws Exception  {
        try {
            if (server == null) {
                server = ServerBuilder.forPort(port)
                        .addService(arthasServiceGrpc)
                        //enable server-reflect
                        //.addService(ProtoReflectionService.newInstance())
                        .build();

                server.start();

                logger.info("Channel server started on port: {} (grpc)", port);

                //server.awaitTermination();
            }
        } catch (Exception e) {
            logger.error("Channel server start failure", e);
            throw e;
        }
    }

    public void stop() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
