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
     * 请求的 path
     */
    private String path;

    /**
     * 请求的方法
     */
    private String method;

    /**
     * 数据
     */
    private ByteBuf data;

    private int dataLength;

    public GrpcRequest(Integer streamId, String path,String method) {
        this.streamId = streamId;
        this.path = path;
        this.method = method;
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
