package com.taobao.arthas.service.req;/**
 * @author: 風楪
 * @date: 2024/7/14 上午4:28
 */

import com.google.protobuf.*;
import com.taobao.arthas.service.ArthasSampleService;
import helloworld.Test;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/7/14 上午4:28
 * @description: ArthasSampleRequest
 */
public class ArthasSampleRequest{

    private String name;

    public ArthasSampleRequest(ByteBuffer byteBuffer){
        CodedInputStream codedInputStream = CodedInputStream.newInstance(byteBuffer);
        try {
            // 读取标签
            int tag;
            while ((tag = codedInputStream.readTag()) != 0) {
                int fieldNumber = WireFormat.getTagFieldNumber(tag);
                int wireType = WireFormat.getTagWireType(tag);

                System.out.println("Field Number: " + fieldNumber);
                System.out.println("Wire Type: " + wireType);

                // 根据字段编号和类型读取对应的数据
                switch (wireType) {
                    case WireFormat.WIRETYPE_VARINT:
                        long varintValue = codedInputStream.readInt64();
                        System.out.println("Varint Value: " + varintValue);
                        break;
                    case WireFormat.WIRETYPE_FIXED32:
                        int fixed32Value = codedInputStream.readFixed32();
                        System.out.println("Fixed32 Value: " + fixed32Value);
                        break;
                    case WireFormat.WIRETYPE_FIXED64:
                        long fixed64Value = codedInputStream.readFixed64();
                        System.out.println("Fixed64 Value: " + fixed64Value);
                        break;
                    case WireFormat.WIRETYPE_LENGTH_DELIMITED:
                        int length = codedInputStream.readRawVarint32();
                        byte[] bytes = codedInputStream.readRawBytes(length);
                        System.out.println("Length-delimited Value: " + new String(bytes));
                        break;
                    default:
                        throw new IOException("Unsupported wire type: " + wireType);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
