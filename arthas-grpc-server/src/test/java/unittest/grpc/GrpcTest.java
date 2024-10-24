package unittest.grpc;

import arthas.grpc.unittest.ArthasUnittest;
import arthas.grpc.unittest.ArthasUnittestServiceGrpc;
import com.taobao.arthas.grpc.server.ArthasGrpcServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    Random random = new Random();
    ExecutorService threadPool = Executors.newFixedThreadPool(10);

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

        ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub blockingStub = ArthasUnittestServiceGrpc.newBlockingStub(channel);

        try {
            trace(blockingStub, "trace");
        } finally {
            channel.shutdownNow();
        }
    }

    @Test
    public void testUnarySum() throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(HOST_PORT)
                .usePlaintext()
                .build();

        ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub stub = ArthasUnittestServiceGrpc.newBlockingStub(channel);
        for (int i = 0; i < 10; i++) {
            AtomicInteger sum = new AtomicInteger(0);
            int finalId = i;
            for (int j = 0; j < 100; j++) {
                int num = random.nextInt(101);
                sum.addAndGet(num);
                threadPool.submit(() -> {
                    addSum(stub, finalId, num);
                });
            }
            Thread.sleep(2000L);
            int grpcSum = getSum(stub, finalId);
            System.out.println("id:" + finalId + ",sum:" + sum.get() + ",grpcSum:" + grpcSum);
            Assert.assertEquals(sum.get(), grpcSum);
        }
        channel.shutdown();
    }

    // 用于测试客户端流
    @Test
    public void testClientStreamSum() throws Throwable {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = ArthasUnittestServiceGrpc.newStub(channel);

        AtomicInteger sum = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<ArthasUnittest.ArthasUnittestRequest> clientStreamObserver = stub.clientStreamSum(new StreamObserver<ArthasUnittest.ArthasUnittestResponse>() {
            @Override
            public void onNext(ArthasUnittest.ArthasUnittestResponse response) {
                System.out.println("local sum:" + sum + ", grpc sum:" + response.getNum());
                Assert.assertEquals(sum.get(), response.getNum());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t);
            }

            @Override
            public void onCompleted() {
                System.out.println("Client streaming completed.");
                latch.countDown();
            }
        });

        for (int j = 0; j < 1000; j++) {
            int num = random.nextInt(1001);
            sum.addAndGet(num);
            clientStreamObserver.onNext(ArthasUnittest.ArthasUnittestRequest.newBuilder().setNum(num).build());
        }

        clientStreamObserver.onCompleted();
        latch.await();
        channel.shutdown();
    }

    // 用于测试请求数据隔离性
    @Test
    public void testDataIsolation() throws InterruptedException {
        //todo 待完善
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = ArthasUnittestServiceGrpc.newStub(channel);
        for (int i = 0; i < 2; i++) {
            threadPool.submit(() -> {
                AtomicInteger sum = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(1);
                StreamObserver<ArthasUnittest.ArthasUnittestRequest> clientStreamObserver = stub.clientStreamSum(new StreamObserver<ArthasUnittest.ArthasUnittestResponse>() {
                    @Override
                    public void onNext(ArthasUnittest.ArthasUnittestResponse response) {
                        System.out.println("local sum:" + sum + ", grpc sum:" + response.getNum());
                        Assert.assertEquals(sum.get(), response.getNum());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("Error: " + t);
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Client streaming completed.");
                        latch.countDown();
                    }
                });

                for (int j = 0; j < 5; j++) {
                    int num = random.nextInt(101);
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    sum.addAndGet(num);
                    clientStreamObserver.onNext(ArthasUnittest.ArthasUnittestRequest.newBuilder().setNum(num).build());
                }

                clientStreamObserver.onCompleted();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                channel.shutdown();
            });
        }
        Thread.sleep(7000L);
    }

    private void trace(ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub stub, String name) {
        ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setMessage(name).build();
        ArthasUnittest.ArthasUnittestResponse res = stub.trace(request);
        System.out.println(res.getMessage());
    }

    private void watch(ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub, String... names) {
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

    private void addSum(ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub stub, int id, int num) {
        ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setId(id).setNum(num).build();
        ArthasUnittest.ArthasUnittestResponse res = stub.unaryAddSum(request);
    }

    private int getSum(ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub stub, int id) {
        ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setId(id).build();
        ArthasUnittest.ArthasUnittestResponse res = stub.unaryGetSum(request);
        return res.getNum();
    }
}
