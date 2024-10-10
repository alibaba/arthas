package unittest.grpc;

import arthas.unittest.ArthasUnittest;
import arthas.unittest.ArthasUnittestServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;

import java.util.concurrent.CountDownLatch;

/**
 * @author: FengYe
 * @date: 2024/9/24 00:17
 * @description: GrpcUnaryTest
 */
public class GrpcTest {
    private static final String target = "localhost:9090";
    private ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub blockingStub = null;
    private ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = null;

    @Disabled("跳过启动测试")
    @Test
    public void testUnary() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();

        blockingStub = ArthasUnittestServiceGrpc.newBlockingStub(channel);

        try {
            trace("trace");
        } finally {
            channel.shutdownNow();
        }
    }

    @Disabled("跳过启动测试")
    @Test
    public void testStream() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();

        stub = ArthasUnittestServiceGrpc.newStub(channel);

        try {
            watch("watch1", "watch2", "watch3");
        } finally {
            channel.shutdownNow();
        }
    }

    private void trace(String name) {
        ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setMessage(name).build();
        try {
            ArthasUnittest.ArthasUnittestResponse res = blockingStub.trace(request);
            System.out.println(res.getMessage());
        } catch (StatusRuntimeException e) {
            e.printStackTrace();
            System.out.println("RPC failed: " + e.getStatus());
        }
    }

    private void watch(String... names) {
        // 使用 CountDownLatch 来等待所有响应
        CountDownLatch finishLatch = new CountDownLatch(1);

        StreamObserver<ArthasUnittest.ArthasUnittestRequest> watch = stub.watch(new StreamObserver<ArthasUnittest.ArthasUnittestResponse>() {
            @Override
            public void onNext(ArthasUnittest.ArthasUnittestResponse value) {
                System.out.println("watch: " + value.getMessage());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                System.out.println("Finished sending watch.");
            }
        });


        try {
            for (String name : names) {
                ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setMessage(name).build();
                Thread.sleep(1000L);
                watch.onNext(request);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            watch.onCompleted();
        }

        // 等待服务器的响应
        try {
            finishLatch.await(); // 等待完成
        } catch (InterruptedException e) {
            System.out.println("Client interrupted: " + e.getMessage());
        }
    }
}
