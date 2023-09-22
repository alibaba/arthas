package com.taobao.arthas.grpcweb.demo.server;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.SocketUtils;
import com.taobao.arthas.grpcweb.demo.service.ObjectService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class GrpcServer {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private int port;
    private Server grpcServer;

    public GrpcServer(int port) {
        if (port == 0) {
            this.port = SocketUtils.findAvailableTcpPort();
        } else {
            this.port = port;
        }
    }

    public void start() {
        try {
            grpcServer = ServerBuilder.forPort(port)
                    .addService(new ObjectService())
                    .build()
                    .start();
            logger.info("Server started, listening on " + port);
            Runtime.getRuntime().addShutdownHook(new Thread("grpc-server-shutdown") {
                @Override
                public void run() {
                    if (grpcServer != null) {
                        grpcServer.shutdown();
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
