package com.taobao.arthas.grpc.server.service.req;/**
 * @author: 風楪
 * @date: 2024/7/14 上午4:28
 */

import com.taobao.arthas.grpc.server.protobuf.annotation.ProtobufClass;
import lombok.ToString;

import java.util.List;

/**
 * @author: FengYe
 * @date: 2024/7/14 上午4:28
 * @description: ArthasSampleRequest
 */

@ProtobufClass
@ToString
public class ArthasSampleRequest {

    private String name;
    private double age;
    private long price;
    private StatusEnum status;
    private List<TestClass> testList;

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
