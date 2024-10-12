package unittest.grpc.service.req;

import com.taobao.arthas.grpc.server.protobuf.annotation.ProtobufClass;

/**
 * @author: FengYe
 * @date: 2024/7/14 上午4:28
 * @description: ArthasSampleRequest
 */
@ProtobufClass
public class ArthasUnittestRequest {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
