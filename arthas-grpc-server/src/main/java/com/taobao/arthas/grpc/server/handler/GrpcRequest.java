package com.taobao.arthas.grpc.server.handler;/**
 * @author: 風楪
 * @date: 2024/9/4 23:07
 */

import com.taobao.arthas.grpc.server.utils.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * @author: FengYe
 * @date: 2024/9/4 23:07
 * @description: GrpcRequest grpc 请求体
 */
public class GrpcRequest {

    /**
     * 请求对应的 streamId
     */
    private Integer streamId;

    /**
     * 请求的 service
     */
    private String service;

    /**
     * 请求的 method
     */
    private String method;

    /**
     * 二进制数据，可能包含多个 grpc body，每个 body 都带有 5 个 byte 的前缀，分别是 boolean compressed - int length
     */
    private ByteBuf byteData;

    /**
     * 二进制数据的长度
     */
    private int length;

    /**
     * 请求class
     */
    private Class<?> clazz;

    /**
     * 是否是 grpc 流式请求
     */
    private boolean stream;

    /**
     * 是否是 grpc 流式请求的第一个data
     */
    private boolean streamFirstData;


    public GrpcRequest(Integer streamId, String path, String method) {
        this.streamId = streamId;
        this.service = path;
        this.method = method;
        this.byteData = ByteUtil.newByteBuf();
    }

    public void writeData(ByteBuf byteBuf) {
        byte[] bytes = ByteUtil.getBytes(byteBuf);
        if (bytes.length == 0) {
            return;
        }
        byte[] decompressedData = decompressGzip(bytes);
        if (decompressedData == null) {
            return;
        }
        byteData.writeBytes(ByteUtil.newByteBuf(decompressedData));
    }

    /**
     * 读取部分数据
     *
     * @return
     */
    public byte[] readData() {
        if (byteData.readableBytes() == 0) {
            return null;
        }
        boolean compressed = byteData.readBoolean();
        int length = byteData.readInt();
        byte[] bytes = new byte[length];
        byteData.readBytes(bytes);
        return bytes;
    }

    public void clearData() {
        byteData.clear();
    }

    // todo 后续优化gzip处理
    private byte[] decompressGzip(byte[] compressedData) {
        boolean isGzip = (compressedData.length > 2 && (compressedData[0] & 0xff) == 0x1f && (compressedData[1] & 0xff) == 0x8b);
        if (isGzip) {
            try {
                InputStream byteStream = new ByteArrayInputStream(compressedData);
                GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                byte[] buffer = new byte[1024];
                int len;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((len = gzipStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return out.toByteArray();
            } catch (IOException e) {
                System.err.println("Failed to decompress GZIP data: " + e.getMessage());
            }
            return null;
        } else {
            return compressedData;
        }
    }

    private ByteBuf decompressGzip(ByteBuf byteBuf) {
        byte[] compressedData = ByteUtil.getBytes(byteBuf);
        boolean isGzip = (compressedData.length > 2 && (compressedData[0] & 0xff) == 0x1f && (compressedData[1] & 0xff) == 0x8b);
        if (isGzip) {
            try {
                InputStream byteStream = new ByteArrayInputStream(compressedData);
                GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
                byte[] buffer = new byte[1024];
                int len;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((len = gzipStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return ByteUtil.newByteBuf(out.toByteArray());
            } catch (IOException e) {
                System.err.println("Failed to decompress GZIP data: " + e.getMessage());
            }
            return null;
        } else {
            return byteBuf;
        }
    }

    public Integer getStreamId() {
        return streamId;
    }

    public String getService() {
        return service;
    }

    public String getMethod() {
        return method;
    }

    public ByteBuf getByteData() {
        return byteData;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean isStreamFirstData() {
        return streamFirstData;
    }

    public void setStreamFirstData(boolean streamFirstData) {
        this.streamFirstData = streamFirstData;
    }
}
