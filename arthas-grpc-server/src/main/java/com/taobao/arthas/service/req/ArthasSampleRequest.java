package com.taobao.arthas.service.req;/**
 * @author: 風楪
 * @date: 2024/7/14 上午4:28
 */

import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;
import com.google.protobuf.*;
import com.taobao.arthas.protobuf.annotation.ProtobufClass;
import com.taobao.arthas.protobuf.annotation.ProtobufCustomizedField;
import com.taobao.arthas.protobuf.annotation.ProtobufIgnore;
import lombok.ToString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/7/14 上午4:28
 * @description: ArthasSampleRequest
 */

@com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass
@ProtobufClass
@ToString
public class ArthasSampleRequest {

    private String name;
    private double age;
    private long price;
    private StatusEnum status;
    private List<TestClass> testList;

    @com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass
    @ProtobufClass
    public enum StatusEnum {
        START(1, "开始"),
        STOP(2, "结束");

        StatusEnum(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        private int code;
        private String desc;
    }

    @com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass
    @ProtobufClass
    public static class TestClass {
        private String name;
    }

    public List<TestClass> getTestList() {
        return testList;
    }

    public void setTestList(List<TestClass> testList) {
        this.testList = testList;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public double getAge() {
        return age;
    }

    public void setAge(double age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
