package com.taobao.arthas.grpc.server.handler;

import com.taobao.arthas.grpc.server.handler.constant.GrpcInvokeTypeEnum;
import com.taobao.arthas.grpc.server.utils.ByteUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http2.Http2Headers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * gRPC 请求体封装类
 *
 * <p>该类用于封装和处理 gRPC 请求的所有信息，包括：
 * <ul>
 *   <li>HTTP/2 流标识符（streamId）</li>
 *   <li>服务名和方法名</li>
 *   <li>二进制请求数据（支持压缩）</li>
 *   <li>HTTP/2 头部信息</li>
 *   <li>流式调用相关标识</li>
 * </ul>
 *
 * <p>数据格式说明：
 * 每个 gRPC 消息体都带有 5 字节的前缀头：
 * <ul>
 *   <li>1 字节：压缩标志（boolean compressed）</li>
 *   <li>4 字节：消息长度（int length）</li>
 * </ul>
 *
 * @author FengYe
 * @date 2024/9/4 23:07
 * @param <T> 请求参数的泛型类型
 */
public class GrpcRequest<T> {

    /**
     * HTTP/2 流标识符
     * 用于标识一个 HTTP/2 流，在多路复用中唯一标识一个请求-响应序列
     */
    private Integer streamId;

    /**
     * 请求的目标服务名称
     * 格式通常为：package.serviceName，例如：com.example.ArthasService
     */
    private String service;

    /**
     * 请求的目标方法名称
     * 指定要调用的服务方法
     */
    private String method;

    /**
     * 二进制数据缓冲区
     * 存储请求的二进制数据，可能包含多个 gRPC 消息体
     * 每个消息体都带有 5 字节的前缀（1字节压缩标志 + 4字节数据长度）
     */
    private ByteBuf byteData;

    /**
     * 二进制数据的长度
     * 记录当前数据的总长度
     */
    private int length;

    /**
     * 请求参数的类型
     * 用于反序列化和类型转换
     */
    private Class<?> clazz;

    /**
     * 是否为 gRPC 流式请求
     * 标识当前请求是否属于流式调用（Server Streaming、Client Streaming 或 Bidirectional Streaming）
     */
    private boolean stream;

    /**
     * 是否为流式请求的首个数据块
     * 用于标识流式请求中的第一个消息，可能需要进行特殊处理
     */
    private boolean streamFirstData;

    /**
     * HTTP/2 请求头
     * 包含 gRPC 协议相关的所有元数据，如 content-type、grpc-encoding 等
     */
    private Http2Headers headers;

    /**
     * gRPC 调用类型枚举
     * 标识当前请求的调用类型（一元调用、服务端流、客户端流、双向流）
     */
    private GrpcInvokeTypeEnum grpcType;


    /**
     * 构造函数 - 创建一个新的 gRPC 请求对象
     *
     * @param streamId HTTP/2 流标识符，用于在多路复用中唯一标识请求
     * @param path 请求的服务路径（服务名）
     * @param method 请求的方法名
     */
    public GrpcRequest(Integer streamId, String path, String method) {
        this.streamId = streamId;
        this.service = path;
        this.method = method;
        // 初始化空的 ByteBuf 用于存储请求数据
        this.byteData = ByteUtil.newByteBuf();
    }

    /**
     * 写入请求数据到缓冲区
     *
     * <p>该方法处理以下逻辑：
     * <ol>
     *   <li>将 ByteBuf 转换为字节数组</li>
     *   <li>检查数据是否为空</li>
     *   <li>如果数据是 GZIP 压缩的，则进行解压</li>
     *   <li>将处理后的数据写入内部缓冲区</li>
     * </ol>
     *
     * @param byteBuf 包含请求数据的 ByteBuf 对象
     */
    public void writeData(ByteBuf byteBuf) {
        // 将 Netty 的 ByteBuf 转换为字节数组
        byte[] bytes = ByteUtil.getBytes(byteBuf);
        // 检查数据是否为空，空数据直接返回
        if (bytes.length == 0) {
            return;
        }
        // 尝试解压 GZIP 格式的数据
        byte[] decompressedData = decompressGzip(bytes);
        // 如果解压失败（返回 null），直接返回
        if (decompressedData == null) {
            return;
        }
        // 将解压后的数据写入内部缓冲区
        byteData.writeBytes(ByteUtil.newByteBuf(decompressedData));
    }

    /**
     * 读取一个完整的 gRPC 消息体
     *
     * <p>该方法按照 gRPC 消息格式读取数据：
     * <ol>
     *   <li>读取 1 字节的压缩标志</li>
     *   <li>读取 4 字节的消息长度</li>
     *   <li>根据长度读取实际的消息内容</li>
     * </ol>
     *
     * @return 读取到的消息字节数组，如果缓冲区无数据则返回 null
     */
    public synchronized byte[] readData() {
        // 检查缓冲区是否还有可读数据
        if (byteData.readableBytes() == 0) {
            return null;
        }
        // 读取压缩标志（1 字节）
        boolean compressed = byteData.readBoolean();
        // 读取消息长度（4 字节）
        int length = byteData.readInt();
        // 根据长度读取消息体
        byte[] bytes = new byte[length];
        byteData.readBytes(bytes);
        return bytes;
    }

    /**
     * 清空内部数据缓冲区
     * 释放已读取的数据，准备接收新的请求
     */
    public void clearData() {
        byteData.clear();
    }

    /**
     * 解压 GZIP 格式的压缩数据
     *
     * <p>该方法首先检测数据是否为 GZIP 格式，通过检查魔数（magic number）：
     * <ul>
     *   <li>GZIP 魔数：0x1f 0x8b</li>
     * </ul>
     * 如果是 GZIP 格式，则使用 GZIPInputStream 进行解压；
     * 否则直接返回原始数据。
     *
     * @param compressedData 可能被压缩的数据字节数组
     * @return 解压后的数据字节数组，如果解压失败返回 null
     */
    private byte[] decompressGzip(byte[] compressedData) {
        // 检测是否为 GZIP 格式（通过魔数 0x1f 0x8b 判断）
        boolean isGzip = (compressedData.length > 2 && (compressedData[0] & 0xff) == 0x1f && (compressedData[1] & 0xff) == 0x8b);
        if (isGzip) {
            // 使用 try-with-resources 确保流正确关闭
            try (InputStream byteStream = new ByteArrayInputStream(compressedData);
                 GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                // 创建缓冲区用于读取解压数据
                byte[] buffer = new byte[1024];
                int len;
                // 循环读取解压后的数据并写入输出流
                while ((len = gzipStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                // 返回解压后的完整数据
                return out.toByteArray();
            } catch (IOException e) {
                // 解压失败时打印错误信息并返回 null
                System.err.println("Failed to decompress GZIP data: " + e.getMessage());
                // Optionally rethrow the exception or return an Optional<byte[]>
                return null; // or throw new RuntimeException(e);
            }
        } else {
            // 不是 GZIP 格式，直接返回原始数据
            return compressedData;
        }
    }

    /**
     * 生成 gRPC 方法的唯一标识键
     *
     * <p>该键用于方法路由和查找，格式为：service.method
     * 例如：com.example.ArthasService.executeCommand
     *
     * @return 方法的唯一标识字符串
     */
    public String getGrpcMethodKey() {
        return service + "." + method;
    }

    /**
     * 获取 HTTP/2 流标识符
     *
     * @return 流标识符
     */
    public Integer getStreamId() {
        return streamId;
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
     * 获取方法名称
     *
     * @return 方法名
     */
    public String getMethod() {
        return method;
    }

    /**
     * 获取二进制数据缓冲区
     *
     * @return 包含请求数据的 ByteBuf 对象
     */
    public ByteBuf getByteData() {
        return byteData;
    }

    /**
     * 获取请求参数的类型
     *
     * @return 请求参数的 Class 对象
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * 设置请求参数的类型
     *
     * @param clazz 请求参数的 Class 对象
     */
    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * 判断是否为流式请求
     *
     * @return 如果是流式请求返回 true，否则返回 false
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * 设置是否为流式请求
     *
     * @param stream true 表示流式请求，false 表示一元请求
     */
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    /**
     * 判断是否为流式请求的首个数据块
     *
     * @return 如果是首个数据块返回 true，否则返回 false
     */
    public boolean isStreamFirstData() {
        return streamFirstData;
    }

    /**
     * 设置是否为流式请求的首个数据块
     *
     * @param streamFirstData true 表示首个数据块，false 表示后续数据块
     */
    public void setStreamFirstData(boolean streamFirstData) {
        this.streamFirstData = streamFirstData;
    }

    /**
     * 获取 HTTP/2 请求头
     *
     * @return Http2Headers 对象，包含所有请求头信息
     */
    public Http2Headers getHeaders() {
        return headers;
    }

    /**
     * 设置 HTTP/2 请求头
     *
     * @param headers Http2Headers 对象
     */
    public void setHeaders(Http2Headers headers) {
        this.headers = headers;
    }

    /**
     * 获取 gRPC 调用类型
     *
     * @return GrpcInvokeTypeEnum 枚举值，表示调用的类型
     */
    public GrpcInvokeTypeEnum getGrpcType() {
        return grpcType;
    }

    /**
     * 设置 gRPC 调用类型
     *
     * @param grpcType GrpcInvokeTypeEnum 枚举值
     */
    public void setGrpcType(GrpcInvokeTypeEnum grpcType) {
        this.grpcType = grpcType;
    }
}
