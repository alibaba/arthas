package unittest.protobuf;

import com.taobao.arthas.grpc.server.protobuf.ProtobufCodec;
import com.taobao.arthas.grpc.server.protobuf.ProtobufProxy;
import com.taobao.arthas.grpc.server.service.req.ArthasUnittestRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: FengYe
 * @date: 2024/9/19 22:35
 * @description: protobuf.ProtoBufTest
 */
public class ProtoBufTest {
    @Test
    public void testEncodeAndDecode() {
        ProtoBufTestReq protoBufTestReq = new ProtoBufTestReq();
        protoBufTestReq.setName("test");
        protoBufTestReq.setAge(18);
        protoBufTestReq.setPrice(100L);
        protoBufTestReq.setStatus(ProtoBufTestReq.StatusEnum.START);
        List<ProtoBufTestReq.TestClass> list = new ArrayList<>();
        list.add(new ProtoBufTestReq.TestClass("test1"));
        list.add(new ProtoBufTestReq.TestClass("test2"));
        list.add(new ProtoBufTestReq.TestClass("test3"));
        Map<String,String> map = new HashMap<>();
        map.put("key1","value1");
        map.put("key2","value2");
        map.put("key3","value3");
        protoBufTestReq.setTestList(list);
        protoBufTestReq.setTestMap(map);

        try {
            ProtobufCodec<ProtoBufTestReq> protobufCodec = ProtobufProxy.getCodecCacheSide(ProtoBufTestReq.class);
            byte[] encode = protobufCodec.encode(protoBufTestReq);
            ProtoBufTestReq decode = protobufCodec.decode(encode);
            Assert.assertEquals(protoBufTestReq.toString(), decode.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
