package com.alibaba.arthas.channel.server.utils;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

/**
 * @author gongdewei 2020/9/14
 */
public class PbJsonUtils {

    public static String convertToJson(MessageOrBuilder message) throws InvalidProtocolBufferException {
        return JsonFormat.printer().print(message);
    }

    public static ActionRequest parseRequest(String json) throws InvalidProtocolBufferException {
        ActionRequest.Builder builder = ActionRequest.newBuilder();
        JsonFormat.parser().merge(json, builder);
        return builder.build();
    }

}
