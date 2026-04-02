package com.taobao.arthas.grpc.server.handler;


import arthas.grpc.common.ArthasGrpc;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcMethod;
import com.taobao.arthas.grpc.server.handler.annotation.GrpcService;
import com.taobao.arthas.grpc.server.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * gRPC 响应体封装类
 *
 * <p>该类用于封装和处理 gRPC 响应的所有信息，包括：
 * <ul>
 *   <li>HTTP/2 响应头（status、content-type 等）</li>
 *   <li>服务名和方法名</li>
 *   <li>二进制响应数据（支持 Protocol Buffers 序列化）</li>
 *   <li>响应类型信息</li>
 * </ul>
 *
 * <p>响应数据格式：
 * 每个 gRPC 响应消息都带有 5 字节的前缀头：
 * <ul>
 *   <li>1 字节：压缩标志（false = 0x00 表示未压缩）</li>
 *   <li>4 字节：消息长度（int length）</li>
 *   <li>N 字节：Protocol Buffers 编码的消息体</li>
 * </ul>
 *
 * @author FengYe
 * @date 2024/9/5 02:05
 * @param <T> 响应结果的泛型类型
 */
public class GrpcResponse<T> {

    /**
     * HTTP/2 响应头映射
     * 存储所有需要返回给客户端的 HTTP/2 头部信息
     */
    private Map<String, String> headers;

    /**
     * 响应对应的服务名称
     * 标识该响应属于哪个 gRPC 服务
     */
    private String service;

    /**
     * 响应对应的方法名称
     * 标识该响应是哪个方法的调用结果
     */
    private String method;

    /**
     * 二进制响应数据缓冲区
     * 存储经过序列化后的响应数据
     */
    private ByteBuf byteData;

    /**
     * 响应结果的类型
     * 用于序列化和类型转换
     */
    private Class<?> clazz;

    /**
     * 实例初始化块
     * 在构造函数执行前初始化默认的 HTTP/2 响应头
     */
    {
        headers = new HashMap<>();
        // 设置内容类型为 gRPC
        headers.put("content-type", "application/grpc");
        // 设置编码方式为 identity（表示不压缩）
        headers.put("grpc-encoding", "identity");
        // 设置接受的编码方式（支持无压缩、deflate 和 gzip）
        headers.put("grpc-accept-encoding", "identity,deflate,gzip");
    }

    /**
     * 默认构造函数
     * 创建一个空的 gRPC 响应对象
     */
    public GrpcResponse() {
    }

    /**
     * 基于反射方法的构造函数
     * 自动从方法的注解中提取服务名和方法名
     *
     * @param method 带有 @GrpcService 和 @GrpcMethod 注解的方法对象
     */
    public GrpcResponse(Method method) {
        // 从方法所在类的 @GrpcService 注解中获取服务名
        this.service = method.getDeclaringClass().getAnnotation(GrpcService.class).value();
        // 从方法的 @GrpcMethod 注解中获取方法名
        this.method = method.getAnnotation(GrpcMethod.class).value();
    }

    /**
     * 获取 HTTP/2 结束响应头
     *
     * <p>该方法创建一个包含状态码 "200" 和所有自定义头部的 HTTP/2 头部对象，
     * 用于在响应结束时发送给客户端。
     *
     * @return 包含状态码和自定义头部的 Http2Headers 对象
     */
    public Http2Headers getEndHeader() {
        // 创建默认的 HTTP/2 头部并设置状态码为 200（成功）
        Http2Headers endHeader = new DefaultHttp2Headers().status("200");
        // 将所有自定义头部添加到响应头中
        headers.forEach(endHeader::set);
        return endHeader;
    }

    /**
     * 获取 HTTP/2 流结束响应头
     *
     * <p>该方法创建一个包含 gRPC 状态码的 HTTP/2 头部对象，
     * 用于标识流的正常结束（grpc-status: 0 表示 OK）。
     *
     * @return 包含 grpc-status 的 Http2Headers 对象
     */
    public Http2Headers getEndStreamHeader() {
        // grpc-status: 0 表示请求成功完成
        return new DefaultHttp2Headers().set("grpc-status", "0");
    }

    /**
     * 获取默认的流结束响应头（静态方法）
     *
     * <p>静态工具方法，返回一个标准的 gRPC 成功状态头。
     *
     * @return 包含 grpc-status: 0 的 Http2Headers 对象
     */
    public static Http2Headers getDefaultEndStreamHeader() {
        return new DefaultHttp2Headers().set("grpc-status", "0");
    }

    /**
     * 获取响应数据缓冲区
     *
     * @return 包含序列化后的响应数据的 ByteBuf 对象
     */
    public ByteBuf getResponseData() {
        return byteData;
    }

    /**
     * 将响应对象序列化为 gRPC 格式的二进制数据
     *
     * <p>该方法处理以下逻辑：
     * <ol>
     *   <li>判断响应类型是否为错误响应（ErrorRes）</li>
     *   <li>如果是错误响应，直接使用 Protocol Buffers 序列化</li>
     *   <li>如果是正常响应，通过反射调用对应的 toByteArray() 方法</li>
     *   <li>将序列化后的数据封装为 gRPC 消息格式（5字节前缀 + 数据体）</li>
     * </ol>
     *
     * @param response 响应对象，可以是任意类型的响应结果
     * @throws RuntimeException 如果序列化过程中发生异常
     */
    public void writeResponseData(Object response) {
        byte[] encode = null;
        try {
            // 检查是否为错误响应类型
            if (ArthasGrpc.ErrorRes.class.equals(clazz)) {
                // 错误响应直接使用 Protocol Buffers 序列化
                encode = ((ArthasGrpc.ErrorRes) response).toByteArray();
            } else {
                // 正常响应通过反射查找并调用对应的序列化方法
                // 根据 service 和 method 生成唯一键，从映射表中获取序列化方法
                encode = (byte[]) GrpcDispatcher.responseToByteArrayMap.get(GrpcDispatcher.generateGrpcMethodKey(service, method)).invoke(response);
            }
        } catch (Throwable e) {
            // 序列化失败时抛出运行时异常
            throw new RuntimeException(e);
        }
        // 创建新的 ByteBuf 用于存储响应数据
        this.byteData = ByteUtil.newByteBuf();
        // 写入压缩标志（false = 0x00，表示不压缩）
        this.byteData.writeBoolean(false);
        // 写入消息长度（4 字节）
        this.byteData.writeInt(encode.length);
        // 写入序列化后的消息体
        this.byteData.writeBytes(encode);
    }

    /**
     * 设置响应结果的类型
     *
     * @param clazz 响应结果的 Class 对象
     */
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * 获取服务名称
     *
     * @return 服务名（如：com.example.ArthasService）
     */
    public String getService() {
        return service;
    }

    /**
     * 设置服务名称
     *
     * @param service 服务名
     */
    public void setService(String service) {
        this.service = service;
    }

    /**
     * 获取方法名称
     *
     * @return 方法名
     */
    public String getMethod() {
        return method;
    }

    /**
     * 设置方法名称
     *
     * @param method 方法名
     */
    public void setMethod(String method) {
        this.method = method;
    }
}
