package com.taobao.arthas.service.res;/**
 * @author: 風楪
 * @date: 2024/8/11 22:11
 */

import com.taobao.arthas.protobuf.annotation.ProtobufClass;

/**
 * @author: FengYe
 * @date: 2024/8/11 22:11
 * @description: ArthasSampleResponse
 */
@ProtobufClass
public class ArthasSampleResponse {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
