package com.alibaba.arthas.tunnel.rpc.grpc;

import com.alibaba.arthas.tunnel.common.grpc.auto.Payload;
import com.alibaba.arthas.tunnel.common.grpc.auto.RequestGrpc;
import com.alibaba.arthas.tunnel.common.remote.request.SimpleRequest;
import com.alibaba.arthas.tunnel.common.remote.response.SimpleResponse;
import com.alibaba.arthas.tunnel.common.util.GrpcUtils;
import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author qiyue.zhang@aloudata.com
 * @description GrpcTest
 * @date 2023/6/16 16:51
 */
public class GrpcTest {
    
    @Test
    public void testGrpc() throws ExecutionException, InterruptedException {
        ManagedChannel managedChannel = ManagedChannelBuilder.forTarget("localhost:8999").usePlaintext().build();
        RequestGrpc.RequestFutureStub requestFutureStub = RequestGrpc.newFutureStub(managedChannel);
        SimpleRequest simpleRequest = new SimpleRequest();
        simpleRequest.setBody("hahhahaha");
        ListenableFuture<Payload> request = requestFutureStub.request(GrpcUtils.convert(simpleRequest));
        Payload payload = request.get();
        SimpleResponse response = (SimpleResponse) GrpcUtils.parse(payload, SimpleResponse.class);
    }

}
