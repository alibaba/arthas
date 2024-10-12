package unittest.grpc;

import arthas.unittest.ArthasUnittest;
import arthas.unittest.ArthasUnittestServiceGrpc;
import com.taobao.arthas.grpc.server.ArthasGrpcServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

import java.util.concurrent.CountDownLatch;

/**
 * @author: FengYe
 * @date: 2024/9/24 00:17
 * @description: GrpcUnaryTest
 */
public class GrpcTest {
    private static final String HOST = "localhost";
    private static final int PORT = 9090;
    private static final String HOST_PORT = HOST + ":" + PORT;
    private static final String UNIT_TEST_GRPC_SERVICE_PACKAGE_NAME = "unittest.grpc.service.impl";
    private ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub blockingStub = null;
    private ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = null;

    @Before
    public void startServer() {
        Thread grpcWebProxyStart = new Thread(() -> {
            ArthasGrpcServer arthasGrpcServer = new ArthasGrpcServer(PORT, UNIT_TEST_GRPC_SERVICE_PACKAGE_NAME);
            arthasGrpcServer.start();
        });
        grpcWebProxyStart.start();
    }

    @Test
    public void testUnary() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(HOST_PORT)
                .usePlaintext()
                .build();

        blockingStub = ArthasUnittestServiceGrpc.newBlockingStub(channel);

        try {
            trace("trace");
        } finally {
            channel.shutdownNow();
        }
    }

    @Test
    public void testStream() {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(HOST_PORT)
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
        ArthasUnittest.ArthasUnittestResponse res = blockingStub.trace(request);
        System.out.println(res.getMessage());
    }

    private void watch(String... names){
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            watch.onCompleted();
            finishLatch.countDown();
        }

        // 等待服务器的响应
        try {
            finishLatch.await(); // 等待完成
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
