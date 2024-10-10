package unittest.protobuf;

import com.taobao.arthas.grpc.server.protobuf.ProtobufField;
import com.taobao.arthas.grpc.server.protobuf.annotation.ProtobufClass;
import com.taobao.arthas.grpc.server.service.req.ArthasUnittestRequest;

import java.util.List;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/9/19 22:38
 * @description: ProtoBufTestReq
 */
@ProtobufClass
public class ProtoBufTestReq {
    private String name;
    private double age;
    private long price;
    private ProtoBufTestReq.StatusEnum status;
    private List<TestClass> testList;
    private Map<String, String> testMap;

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        // 注意被 @ProtobufClass 注解的 class 必须添加无参构造函数
        public TestClass() {
        }

        public TestClass(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "TestClass{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    public List<TestClass> getTestList() {
        return testList;
    }

    public void setTestList(List<TestClass> testList) {
        this.testList = testList;
    }

    public ProtoBufTestReq.StatusEnum getStatus() {
        return status;
    }

    public void setStatus(ProtoBufTestReq.StatusEnum status) {
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

    public Map<String, String> getTestMap() {
        return testMap;
    }

    public void setTestMap(Map<String, String> testMap) {
        this.testMap = testMap;
    }

    @Override
    public java.lang.String toString() {
        return "ProtoBufTestReq{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", price=" + price +
                ", status=" + status +
                ", testList=" + testList +
                ", testMap=" + testMap +
                '}';
    }
}

