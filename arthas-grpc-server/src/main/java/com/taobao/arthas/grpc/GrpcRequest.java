package com.taobao.arthas.grpc;/**
 * @author: 風楪
 * @date: 2024/9/4 23:07
 */

import com.taobao.arthas.utils.ByteUtil;
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
     * 请求的方法
     */
    private String method;

    /**
     * 请求的类名（非全限定类名）
     */
    private String className;

    /**
     * protobuf的名称
     */
    private String protoName;

    /**
     * 全限定类名
     */
    private String fullyQualifiedClassName;

    /**
     * 数据
     */
    private ByteBuf data;

    int dataLength;

    private static final String fullyQualifiedClassNamePrefix = "com.taobao.arthas.service.";

    public GrpcRequest(Integer streamId, String method, String className, String protoName) {
        this.streamId = streamId;
        this.method = method;
        this.className = className;
        this.protoName = protoName;
        this.fullyQualifiedClassName = fullyQualifiedClassNamePrefix + "protoName." + className;
        this.data = ByteUtil.getByteBuf();
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
        ByteBuf operateByteBuf = ByteUtil.getByteBuf(decompressedData);
        boolean compressed = operateByteBuf.readBoolean();
        int length = operateByteBuf.readInt();
        dataLength += length;
        System.out.println(length);
        System.out.println(operateByteBuf.readableBytes());
        data.writeBytes(operateByteBuf);
    }

    public byte[] readData() {
        System.out.println(dataLength);
        byte[] res = new byte[dataLength];
        data.readBytes(res);
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
}
