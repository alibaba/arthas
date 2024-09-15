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
 * @description: GrpcRequest
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
     * 二进制数据
     */
    private ByteBuf byteData;

    /**
     * 请求class
     */
    private Class<?> clazz;

    /**
     * 是否是 grpc 流式请求
     */
    private boolean stream;

    public GrpcRequest(Integer streamId, String path,String method) {
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
        ByteBuf operateByteBuf = ByteUtil.newByteBuf(decompressedData);

        // 必须要先把这5个字节读出来，后续才是真正的data
        boolean compressed = operateByteBuf.readBoolean();
        int length = operateByteBuf.readInt();
        byteData.writeBytes(operateByteBuf);
    }

    public byte[] readData() {
        byte[] res = new byte[byteData.readableBytes()];
        byteData.readBytes(res);
        return res;
    }

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
}
