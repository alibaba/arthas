package com.taobao.arthas.grpcweb.grpc.server;

import arthas.VmTool;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.SocketUtils;
import com.taobao.arthas.core.advisor.TransformerManager;
import com.taobao.arthas.grpcweb.grpc.service.*;
import com.taobao.arthas.grpcweb.grpc.view.GrpcResultViewResolver;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandles;

public class GrpcServer {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private int port;

    private Server grpcServer;

    private Instrumentation instrumentation;

    private TransformerManager transformerManager;

    public GrpcServer(int port, Instrumentation instrumentation, TransformerManager transformerManager) {
        if (port == 0) {
            this.port = SocketUtils.findAvailableTcpPort();
        } else {
            this.port = port;
        }
        this.instrumentation = instrumentation;
        this.transformerManager = transformerManager;
    }

    public void start() {
        GrpcResultViewResolver grpcResultViewResolver = new GrpcResultViewResolver();
        GrpcJobController grpcJobController = new GrpcJobController(this.instrumentation, this.transformerManager, grpcResultViewResolver);
        File path = new File(VmTool.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile();
        String libPath = path.getAbsolutePath();

        try {
            grpcServer = ServerBuilder.forPort(port)
                    .addService(new ObjectService(grpcJobController,libPath))
                    .addService(new PwdCommandService(grpcJobController))
                    .addService(new SystemPropertyCommandService(grpcJobController))
                    .addService(new WatchCommandService(grpcJobController))
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
