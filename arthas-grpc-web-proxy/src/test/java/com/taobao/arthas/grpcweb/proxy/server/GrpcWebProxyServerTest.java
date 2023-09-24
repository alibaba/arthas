package com.taobao.arthas.grpcweb.proxy.server;

import grpc.gateway.testing.Echo;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import com.taobao.arthas.common.SocketUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;


public class GrpcWebProxyServerTest {

    private int GRPC_WEB_PROXY_PORT;
    private int GRPC_PORT;
    private String hostName;
    private CloseableHttpClient httpClient;
    @Before
    public void startServer(){
        GRPC_WEB_PROXY_PORT = SocketUtils.findAvailableTcpPort();
        GRPC_PORT = SocketUtils.findAvailableTcpPort();
        // 启动grpc服务
        Thread grpcStart = new Thread(() -> {
            StartGrpcTest startGrpcTest = new StartGrpcTest(GRPC_PORT);
            startGrpcTest.startGrpcService();
        });
        grpcStart.start();
        // 启动grpc-web-proxy服务
        Thread grpcWebProxyStart = new Thread(() -> {
            StartGrpcWebProxyTest startGrpcWebProxyTest = new StartGrpcWebProxyTest(GRPC_WEB_PROXY_PORT,GRPC_PORT);
            startGrpcWebProxyTest.startGrpcWebProxy();
        });
        grpcWebProxyStart.start();
        try {
            // waiting for the server to start
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        hostName = "http://127.0.0.1:" + GRPC_WEB_PROXY_PORT;
        httpClient = HttpClients.createDefault();
    }

    @Test
    public void simpleReqTest()  {
        // 单个response
        String url = hostName +"/grpc.gateway.testing.EchoService/Echo";

        String requestStr = "hello world!!!";
        Echo.EchoRequest request = Echo.EchoRequest.newBuilder().setMessage(requestStr).build();
        System.out.println("request message--->" + requestStr);
        byte[] requestData = request.toByteArray();
        requestData = ByteArrayWithLengthExample(requestData);
        // 编码请求载荷为gRPC-Web格式
        String encodedPayload = Base64.getEncoder().encodeToString(requestData);
        try {
            String result = "";
            String encoding = "utf-8";
            HttpPost httpPost = getPost(url, encodedPayload, encoding);
            //发送请求，并拿到结果（同步阻塞）
            CloseableHttpResponse response = httpClient.execute(httpPost);
            //获取返回结果
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                //按指定编码转换结果实体为String类型
                result = EntityUtils.toString(entity, encoding);
            }
            EntityUtils.consume(entity);
            //释放Http请求链接
            response.close();

            System.out.println("result-->" + result);
            System.out.println("after decode...");
            // gAAAAA9ncnBjLXN0YXR1czowDQo= 是结尾字符
            int endStartIndex = result.indexOf("gAAAAA");
            String data = result.substring(0,endStartIndex);
            String end = result.substring(endStartIndex,result.length());
            byte[] decodedData = Base64.getDecoder().decode(data);
            byte[] decodedEnd = Base64.getDecoder().decode(end);
            // 去掉前5个byte
            decodedData = RemoveBytesExample(decodedData);
            decodedEnd = RemoveBytesExample(decodedEnd);
            Echo.EchoResponse echoResponse = Echo.EchoResponse.parseFrom(decodedData);
            System.out.println("response message--->" + echoResponse.getMessage());
            String endStr = new String(decodedEnd);
            System.out.println(endStr);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void streamReqTest() {
        // stream response
        String url = hostName + "/grpc.gateway.testing.EchoService/ServerStreamingEcho";
        String requestStr = "hello world!!!";
        Echo.ServerStreamingEchoRequest request = Echo.ServerStreamingEchoRequest.newBuilder().setMessage(requestStr)
                .setMessageCount(5)
                .build();
        byte[] requestData = request.toByteArray();
        requestData = ByteArrayWithLengthExample(requestData);
        // 编码请求载荷为gRPC-Web格式
        String encodedPayload = Base64.getEncoder().encodeToString(requestData);
        try {
            String encoding = "utf-8";
            HttpPost httpPost = getPost(url, encodedPayload, encoding);
            //发送请求
            CloseableHttpResponse response = httpClient.execute(httpPost);
            //获取返回结果
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                try (InputStream inputStream = entity.getContent()) {
                    // 在这里使用 inputStream 流式处理响应内容
                    // 例如，逐行读取响应内容
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        // 处理读取的数据
                        String result = new String(buffer, 0, bytesRead);
                        System.out.println("result-->" + result);
                        System.out.println("after decode...");
                        // gAAAAA9ncnBjLXN0YXR1czowDQo= 是结尾字符

                        byte[] decodedData = Base64.getDecoder().decode(result);
                        // 去掉前5个byte
                        decodedData = RemoveBytesExample(decodedData);
                        if(result.startsWith("gAAAAA")){
                            String end = new String(decodedData);
                            System.out.println(end);
                        }else {
                            Echo.ServerStreamingEchoResponse echoResponse = Echo.ServerStreamingEchoResponse.parseFrom(decodedData);
                            System.out.println("response message--->" + echoResponse.getMessage());
                        }
                    }
                }
            }
            EntityUtils.consume(entity);
            //释放Http请求链接
            response.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HttpPost getPost(String url, String param, String encoding) throws IOException {
        System.out.println("request param(encode)--->" + param);
        //创建post方式请求对象
        HttpPost httpPost = new HttpPost (url);
        //设置请求参数实体
        StringEntity reqParam = new StringEntity(param,encoding);
        reqParam.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/grpc-web-text"));
//        将请求参数放到请求对象中
        httpPost.setEntity(reqParam);
        //设置请求报文头信息
        httpPost.setHeader("Connection","keep-alive");
        httpPost.setHeader("Accept", "application/grpc-web-text");
        httpPost.setHeader("Content-type", "application/grpc-web-text");//设置发送表单请求
        httpPost.setHeader("X-Grpc-Web","1");
        httpPost.setHeader("X-User-Agent", "grpc-web-javascript/0.1");
        httpPost.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36");
        return httpPost;
    }

    public byte[] ByteArrayWithLengthExample(byte[] data){
        // 添加长度信息,用于编码过程
        int length = data.length;
        byte[] newData = {0,0,0,0,(byte) length};
        byte[] combineArray = new byte[newData.length + data.length];
        System.arraycopy(newData, 0, combineArray, 0, newData.length);
        System.arraycopy(data, 0, combineArray, newData.length, data.length);
        return combineArray;
    }

    public byte[] RemoveBytesExample(byte[] data){
        // 去掉长度信息,用于解码过程
        byte[] result = Arrays.copyOfRange(data, 5, data.length);
        return result;
    }

}
