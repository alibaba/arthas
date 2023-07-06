package com.alibaba.arthas.tunnel.rpc.remote;

import com.alibaba.arthas.tunnel.common.grpc.auto.Payload;
import com.alibaba.arthas.tunnel.common.util.GrpcAddressFilter;
import com.alibaba.arthas.tunnel.rpc.grpc.GrpcRequestAcceptor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ServerCalls;
import io.grpc.util.MutableHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author qiyue.zhang@aloudata.com
 * @description RpcServer
 * @date 2023/6/16 16:18
 */
@Service
public class RpcServer {
    
    private Server server;
    
    @Autowired
    private GrpcRequestAcceptor grpcRequestAcceptor;
    
    @PostConstruct
    public void start() throws IOException {
        final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();
        // unary common call register.
        final MethodDescriptor<Payload, Payload> unaryPayloadMethod = MethodDescriptor.<Payload, Payload>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY).setFullMethodName(
                        MethodDescriptor.generateFullMethodName("Request",
                                "request"))
                .setRequestMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance()))
                .setResponseMarshaller(ProtoUtils.marshaller(Payload.getDefaultInstance())).build();
        
        final ServerCallHandler<Payload, Payload> payloadHandler = ServerCalls.asyncUnaryCall(
                (request, responseObserver) -> grpcRequestAcceptor.request(request, responseObserver));
        
        final ServerServiceDefinition serviceDefOfUnaryPayload = ServerServiceDefinition.builder(
                "Request").addMethod(unaryPayloadMethod, payloadHandler).build();
        handlerRegistry.addService(ServerInterceptors.intercept(serviceDefOfUnaryPayload));
        NettyServerBuilder builder = NettyServerBuilder.forPort(8999).executor(getRpcExecutor());
        
        server = builder.fallbackHandlerRegistry(handlerRegistry)
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                .addTransportFilter(new GrpcAddressFilter()).build();
        
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
        }));
    }
    
    private Executor getRpcExecutor() {
        return new ThreadPoolExecutor(16, 16,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1<<14),
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("nacos-grpc-executor-%d").build());
    }
}
