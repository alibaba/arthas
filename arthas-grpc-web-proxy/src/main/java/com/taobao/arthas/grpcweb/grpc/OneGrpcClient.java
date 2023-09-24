package com.taobao.arthas.grpcweb.grpc;


import arthas.grpc.api.*;
import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OneGrpcClient {
    private static final Logger logger = Logger.getLogger(OneGrpcClient.class.getName());

    private final ManagedChannel channel;

    private ObjectServiceGrpc.ObjectServiceBlockingStub objectServiceBlockingStub;

    private final PwdGrpc.PwdBlockingStub pwdBlockingStub;

    private final SystemPropertyGrpc.SystemPropertyBlockingStub systemPropertyBlockingStub;

    private final WatchGrpc.WatchBlockingStub watchBlockingStub;


    /**
     * Construct client connecting to HelloWorld server at {@code host:port}.
     */
    public OneGrpcClient(String host, int port) {

        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        objectServiceBlockingStub = ObjectServiceGrpc.newBlockingStub(channel);

        pwdBlockingStub =  PwdGrpc.newBlockingStub(channel);
        systemPropertyBlockingStub = SystemPropertyGrpc.newBlockingStub(channel);
        watchBlockingStub = WatchGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void objectGet(String express, int expand){
        ArthasService.ObjectRequest request = ArthasService.ObjectRequest.newBuilder()
                .setExpress(express)
                .setExpand(expand).build();
        ArthasService.StringValue response;
        try{
            response = objectServiceBlockingStub.getInstance(request);
        }catch (StatusRuntimeException e){
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        System.out.println("获取到的结果: \n");
        System.out.println(response.getValue());
    }


    public void syspropGet() throws InvalidProtocolBufferException {
        Empty empty = Empty.newBuilder().build();
//        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        ArthasService.ResponseBody response;
        try {
            response = systemPropertyBlockingStub.get(empty);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        System.out.println("获取到的结果: \n");
        System.out.println(response.getBody().unpack(ArthasService.SimpleResponse.class).getResultsMap());
    }

    public void syspropGetByKey() throws InvalidProtocolBufferException {
        ArthasService.StringKey stringKey = ArthasService.StringKey.newBuilder().setKey("java.version").build();
//        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        ArthasService.ResponseBody response;
        try {
            response = systemPropertyBlockingStub.getByKey(stringKey);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        System.out.println("获取到的结果: \n");
        System.out.println(response.getBody().unpack(ArthasService.SimpleResponse.class));    }

    public void syspropSetByKey() throws InvalidProtocolBufferException {
        ArthasService.Properties properties = ArthasService.Properties.newBuilder().putProperties("java.version","hello").build();
//        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        ArthasService.ResponseBody response;
        try {
            response = systemPropertyBlockingStub.update(properties);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        System.out.println("获取到的结果: \n");
        System.out.println(response.getBody().unpack(ArthasService.SimpleResponse.class));
    }


    public void pwdCommand() throws InvalidProtocolBufferException {
        Empty empty = Empty.newBuilder().build();
//        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        ArthasService.ResponseBody response;
        try {
            response = pwdBlockingStub.pwd(empty);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        System.out.println("获取到的结果: \n");
        System.out.println(response.getSessionId());
        ArthasService.SimpleResponse unpack = response.getBody().unpack(ArthasService.SimpleResponse.class);
        System.out.println(unpack.getResultsMap());
        System.out.println(unpack.getJobId());
    }

    public void watchTest(){
        ArthasService.WatchRequest watchRequest = ArthasService.WatchRequest.newBuilder()
                .setClassPattern("MathGame")
                .setMethodPattern("primeFactors")
                .setExpress("{params, target, returnObj}")
                .setNumberOfLimit(10)
                .setExpand(1)
//                .setJobId(4)
                .build();
        Iterator<ArthasService.ResponseBody> response;

        try {
            System.out.println("再次请求执行");
            response = watchBlockingStub.watch(watchRequest);
            System.out.println("执行了");
            while (response.hasNext()){
                ArthasService.ResponseBody next = response.next();
                System.out.println(next.getSessionId());
                System.out.println(next.getStatusCode());
                System.out.println(next.getMessage());
                System.out.println(next.getBody());
                try {
                    System.out.println(next.getBody().unpack(ArthasService.WatchResponse.class));
                } catch (Exception e){
                    System.out.println("不是watctResponse");
                }

//                System.out.println("获取到的jobId: " + next.getJobId());
////                System.out.println("获取到的ts: " + next.getTs());
//                Instant instant = Instant.ofEpochSecond(next.getTs().getSeconds(), next.getTs().getNanos());
//                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
//                System.out.println("获取到的ts(格式化后): " + dateTime);
//                System.out.println("获取到的cost: " + next.getCost());
//                System.out.println("获取到的value: " + next.getValue());
//                System.out.println("获取到的sizeLimit: " + next.getSizeLimit());
//                System.out.println("获取到的className: " + next.getClassName());
//                System.out.println("获取到的methodName: " + next.getMethodName());
//                System.out.println("获取到的accessPoint: " + next.getAccessPoint());
//                System.out.println("获取到的message: " + next.getMessage());
//                System.out.println("\n\n\n");
            }
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
//        logger.info("ExecStreamCommand: 执行结束");
    }



    /**
     * Greet server. If provided, the first element of {@code args} is the name to use in the
     * greeting.
     */
    public static void main(String[] args) throws Exception {
        OneGrpcClient client = new OneGrpcClient("127.0.0.1", 8566);
        try {
//            client.syspropGet();
//            client.syspropGetByKey();
//            client.syspropSetByKey();
//            client.syspropGetByKey();
//            client.watchTest();
//            client.pwdCommand();

            client.objectGet("instances[0].mapExample#{656}",1);
        } finally {
            client.shutdown();
        }
    }
}
