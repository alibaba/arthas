package unittest.grpc.service.res;

import com.taobao.arthas.grpc.server.protobuf.annotation.ProtobufClass;

/**
 * @author: FengYe
 * @date: 2024/8/11 22:11
 * @description: ArthasSampleResponse
 */
@ProtobufClass
public class ArthasUnittestResponse {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
