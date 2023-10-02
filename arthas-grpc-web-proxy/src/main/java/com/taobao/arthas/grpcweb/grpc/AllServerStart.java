package com.taobao.arthas.grpcweb.grpc;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.SocketUtils;
import com.taobao.arthas.grpcweb.grpc.objectUtils.ComplexObject;
import com.taobao.arthas.grpcweb.grpc.server.GrpcServer;
import com.taobao.arthas.grpcweb.grpc.server.httpServer.NettyHttpServer;
import com.taobao.arthas.grpcweb.proxy.server.GrpcWebProxyServer;

import java.lang.invoke.MethodHandles;


public class AllServerStart {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getName());

    private int GRPC_WEB_PROXY_PORT;

    private int GRPC_PORT;

    private int HTTP_PORT;


    public void startAllServer() throws InterruptedException {
        ComplexObject ccc = createComplexObject();

        // 1. 启动grpc服务
        this.GRPC_PORT = SocketUtils.findAvailableTcpPort();
        GrpcServer grpcServer = new GrpcServer(GRPC_PORT);
        grpcServer.start();

        // 2. 启动grpc-web-proxy服务
        //this.GRPC_WEB_PROXY_PORT = SocketUtils.findAvailableTcpPort();
        this.GRPC_WEB_PROXY_PORT = 8567;
        Thread grpcWebProxyStart = new Thread(() -> {
            GrpcWebProxyServer grpcWebProxyServer = new GrpcWebProxyServer(GRPC_WEB_PROXY_PORT,GRPC_PORT);
            grpcWebProxyServer.start();
        });
        grpcWebProxyStart.start();
        Thread.sleep(100);

        // 3. 启动http服务
        this.HTTP_PORT = SocketUtils.findAvailableTcpPort();
        String STATIC_LOCATION = this.getClass().getResource("/dist").getPath().substring(1);
        NettyHttpServer nettyHttpServer = new NettyHttpServer(HTTP_PORT,STATIC_LOCATION);
        logger.info("start grpc server on port: {}, grpc web proxy server on port: {}, " +
                "http server server on port: {}", GRPC_PORT,GRPC_WEB_PROXY_PORT,HTTP_PORT);
        System.out.println("Open your web browser and navigate to " + "http" + "://127.0.0.1:" + HTTP_PORT + '/' + "index.html");
        nettyHttpServer.start();
    }

    public static ComplexObject createComplexObject() {
        // 创建一个 ComplexObject 对象
        ComplexObject complexObject = new ComplexObject();

        // 设置基本类型的值
        complexObject.setId(1);
        complexObject.setName("Complex Object");
        complexObject.setValue(3.14);

        // 设置基本类型的数组
        int[] numbers = { 1, 2, 3, 4, 5 };
        complexObject.setNumbers(numbers);

        // 创建并设置嵌套对象
        ComplexObject.NestedObject nestedObject = new ComplexObject.NestedObject();
        nestedObject.setNestedId(10);
        nestedObject.setNestedName("Nested Object");
        nestedObject.setFlag(true);
        complexObject.setNestedObject(nestedObject);

        // 创建并设置复杂对象数组
        ComplexObject[] complexArray = new ComplexObject[2];

        ComplexObject complexObject1 = new ComplexObject();
        complexObject1.setId(2);
        complexObject1.setName("Complex Object 1");
        complexObject1.setValue(2.71);

        ComplexObject complexObject2 = new ComplexObject();
        complexObject2.setId(3);
        complexObject2.setName("Complex Object 2");
        complexObject2.setValue(1.618);

        complexArray[0] = complexObject1;
        complexArray[1] = complexObject2;

        complexObject.setComplexArray(complexArray);

        // 创建并设置多维数组
        int[][] multiDimensionalArray = { { 1, 2, 3 }, { 4, 5, 6 } };
        complexObject.setMultiDimensionalArray(multiDimensionalArray);

        // 设置数组中的基本元素数组
        String[] stringArray = { "Hello", "World" };
        complexObject.setStringArray(stringArray);

        // 输出 ComplexObject 对象的信息
        System.out.println(complexObject);

        return complexObject;
    }

    public static void main(String[] args) throws InterruptedException {
        AllServerStart allServerStart = new AllServerStart();
        allServerStart.startAllServer();
    }
}
