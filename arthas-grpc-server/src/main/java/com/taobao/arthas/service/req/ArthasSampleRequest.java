package com.taobao.arthas.service.req;/**
 * @author: 風楪
 * @date: 2024/7/14 上午4:28
 */

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.google.protobuf.*;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author: FengYe
 * @date: 2024/7/14 上午4:28
 * @description: ArthasSampleRequest
 */
@ProtobufClass
public class ArthasSampleRequest{
    @Override
    public String toString() {
        return "ArthasSampleRequest{" +
//                "name='" + name + '\'' +
                ", age=" + age +
                ", price=" + price +
                ", man=" + man +
                '}';
    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }

    public Double getAge() {
        return age;
    }

    public void setAge(Double age) {
        this.age = age;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Float getMan() {
        return man;
    }

    public void setMan(Float man) {
        this.man = man;
    }

//    private String name;
    private Double age;
    private Long price;
    private Float man;

//    public ArthasSampleRequest(ByteBuffer byteBuffer){
//        CodedInputStream codedInputStream = CodedInputStream.newInstance(byteBuffer);
//        try {
//            // 读取标签
//            int tag;
//            while ((tag = codedInputStream.readTag()) != 0) {
//                int fieldNumber = WireFormat.getTagFieldNumber(tag);
//                int wireType = WireFormat.getTagWireType(tag);
//
//                System.out.println("Field Number: " + fieldNumber);
//                System.out.println("Wire Type: " + wireType);
//
//
//                // 根据字段编号和类型读取对应的数据
//                switch (wireType) {
//                    case WireFormat.WIRETYPE_VARINT:
//                        long varintValue = codedInputStream.readInt64();
//                        System.out.println("Varint Value: " + varintValue);
//                        break;
//                    case WireFormat.WIRETYPE_FIXED32:
//                        int fixed32Value = codedInputStream.readFixed32();
//                        System.out.println("Fixed32 Value: " + fixed32Value);
//                        break;
//                    case WireFormat.WIRETYPE_FIXED64:
//                        long fixed64Value = codedInputStream.readFixed64();
//                        System.out.println("Fixed64 Value: " + fixed64Value);
//                        break;
//                    case WireFormat.WIRETYPE_LENGTH_DELIMITED:
//                        int length = codedInputStream.readRawVarint32();
//                        byte[] bytes = codedInputStream.readRawBytes(length);
//                        System.out.println("Length-delimited Value: " + new String(bytes));
//                        break;
//                    default:
//                        throw new IOException("Unsupported wire type: " + wireType);
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
