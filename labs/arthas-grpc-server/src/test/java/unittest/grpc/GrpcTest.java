package unittest.grpc;

import arthas.grpc.unittest.ArthasUnittest;
import arthas.grpc.unittest.ArthasUnittestServiceGrpc;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.taobao.arthas.grpc.server.ArthasGrpcServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: FengYe
 * @date: 2024/9/24 00:17
 * @description: GrpcUnaryTest
 */
public class GrpcTest {
    private static final String HOST = "localhost";
    private static final int PORT = 9092;
    private static final String HOST_PORT = HOST + ":" + PORT;
    private static final String UNIT_TEST_GRPC_SERVICE_PACKAGE_NAME = "unittest.grpc.service.impl";
    private static final Logger log = (Logger) LoggerFactory.getLogger(GrpcTest.class);
    private ManagedChannel clientChannel;
    Random random = new Random();
    ExecutorService threadPool = Executors.newFixedThreadPool(10);


    @Before
    public void startServer() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("ROOT");

        rootLogger.setLevel(Level.INFO);

        Thread grpcWebProxyStart = new Thread(() -> {
            ArthasGrpcServer arthasGrpcServer = new ArthasGrpcServer(PORT, UNIT_TEST_GRPC_SERVICE_PACKAGE_NAME);
            arthasGrpcServer.start();
        });
        grpcWebProxyStart.start();

        clientChannel = ManagedChannelBuilder.forTarget(HOST_PORT)
                .usePlaintext()
                .build();
    }

    @Test
    public void testUnary() {
        log.info("testUnary start!");


        ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub stub = ArthasUnittestServiceGrpc.newBlockingStub(clientChannel);

        try {
            ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setMessage("unaryInvoke").build();
            ArthasUnittest.ArthasUnittestResponse res = stub.unary(request);
            System.out.println(res.getMessage());
        } finally {
            clientChannel.shutdownNow();
        }
        log.info("testUnary success!");
    }

    @Test
    public void testUnarySum() throws InterruptedException {
        log.info("testUnarySum start!");

        ArthasUnittestServiceGrpc.ArthasUnittestServiceBlockingStub stub = ArthasUnittestServiceGrpc.newBlockingStub(clientChannel);
        for (int i = 0; i < 10; i++) {
            AtomicInteger sum = new AtomicInteger(0);
            int finalId = i;
            for (int j = 0; j < 10; j++) {
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
        clientChannel.shutdown();
        log.info("testUnarySum success!");
    }

    // 用于测试客户端流
    @Test
    public void testClientStreamSum() throws Throwable {
        log.info("testClientStreamSum start!");

        ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = ArthasUnittestServiceGrpc.newStub(clientChannel);

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
                System.out.println("testClientStreamSum completed.");
                latch.countDown();
            }
        });

        for (int j = 0; j < 100; j++) {
            int num = random.nextInt(1001);
            sum.addAndGet(num);
            clientStreamObserver.onNext(ArthasUnittest.ArthasUnittestRequest.newBuilder().setNum(num).build());
        }

        clientStreamObserver.onCompleted();
        latch.await(20,TimeUnit.SECONDS);
        clientChannel.shutdown();
        log.info("testClientStreamSum success!");
    }

    // 用于测试请求数据隔离性
    @Test
    public void testDataIsolation() throws InterruptedException {
        log.info("testDataIsolation start!");

        ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = ArthasUnittestServiceGrpc.newStub(clientChannel);
        for (int i = 0; i < 10; i++) {
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
                        System.out.println("testDataIsolation completed.");
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
                    latch.await(20,TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                clientChannel.shutdown();
            });
        }
        Thread.sleep(10000L);
        log.info("testDataIsolation success!");
    }

    @Test
    public void testServerStream() throws InterruptedException {
        log.info("testServerStream start!");

        ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = ArthasUnittestServiceGrpc.newStub(clientChannel);

        ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setMessage("serverStream").build();

        stub.serverStream(request, new StreamObserver<ArthasUnittest.ArthasUnittestResponse>() {
            @Override
            public void onNext(ArthasUnittest.ArthasUnittestResponse value) {
                System.out.println("testServerStream client receive: " + value.getMessage());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                System.out.println("testServerStream completed");
            }
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            clientChannel.shutdown();
        }
        log.info("testServerStream success!");
    }

    // 用于测试双向流
    @Test
    public void testBiStream() throws Throwable {
        log.info("testBiStream start!");

        ArthasUnittestServiceGrpc.ArthasUnittestServiceStub stub = ArthasUnittestServiceGrpc.newStub(clientChannel);

        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<ArthasUnittest.ArthasUnittestRequest> biStreamObserver = stub.biStream(new StreamObserver<ArthasUnittest.ArthasUnittestResponse>() {
            @Override
            public void onNext(ArthasUnittest.ArthasUnittestResponse response) {
                System.out.println("testBiStream receive: "+response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error: " + t);
            }

            @Override
            public void onCompleted() {
                System.out.println("testBiStream completed.");
                latch.countDown();
            }
        });

        String[] messages = new String[]{"testBiStream1","testBiStream2","testBiStream3"};
        for (String msg : messages) {
            ArthasUnittest.ArthasUnittestRequest request = ArthasUnittest.ArthasUnittestRequest.newBuilder().setMessage(msg).build();
            biStreamObserver.onNext(request);
        }

        Thread.sleep(2000);
        biStreamObserver.onCompleted();
        latch.await(20, TimeUnit.SECONDS);
        clientChannel.shutdown();
        log.info("testBiStream success!");
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
