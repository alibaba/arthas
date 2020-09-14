package com.alibaba.arthas.channel.server.json;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ConsoleParams;
import com.alibaba.arthas.channel.proto.ExecuteParams;
import com.alibaba.arthas.channel.proto.RequestAction;
import com.alibaba.arthas.channel.proto.ResultFormat;
import com.alibaba.arthas.channel.server.utils.PbJsonUtils;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author gongdewei 2020/9/14
 */
public class PbJsonTest {

    @Test
    public void testExecuteRequestJson() throws InvalidProtocolBufferException {
        ActionRequest request = ActionRequest.newBuilder()
                .setAgentId("agentid-123423")
                .setRequestId("requestid-12342")
                .setAction(RequestAction.EXECUTE)
                .setExecuteParams(ExecuteParams.newBuilder()
                        .setResultFormat(ResultFormat.JSON)
                        .setCommandLine("session")
                )
                .build();

        String json = PbJsonUtils.convertToJson(request);
        System.out.println(json);

        ActionRequest request2 = PbJsonUtils.parseRequest(json);
        Assert.assertEquals("ActionRequest json parse failure", request, request2);
    }

    @Test
    public void testConsoleRequestJson() throws InvalidProtocolBufferException {
        ActionRequest request = ActionRequest.newBuilder()
                .setAgentId("agentid-123423")
                .setRequestId("requestid-12342")
                .setAction(RequestAction.CONSOLE_INPUT)
                .setConsoleParams(ConsoleParams.newBuilder()
                        .setConsoleId("consoleid-12343")
                        .setInputData("session\n")
                )
                .build();

        String json = PbJsonUtils.convertToJson(request);
        System.out.println(json);

        ActionRequest request2 = PbJsonUtils.parseRequest(json);
        Assert.assertEquals("ActionRequest json parse failure", request, request2);
    }


}
