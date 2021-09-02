package com.alibaba.arthas.channel.server;

import com.alibaba.arthas.channel.proto.ActionRequest;
import com.alibaba.arthas.channel.proto.ExecuteParams;
import com.alibaba.arthas.channel.proto.RequestAction;
import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
import com.alibaba.arthas.channel.server.service.impl.ApiActionDelegateServiceImpl;
import org.junit.Test;

/**
 * @author gongdewei 2021/7/22
 */
public class ApiActionDelegateServiceTest {

    @Test
    public void testSendRequests() throws Exception {

        ApiActionDelegateService apiActionDelegateService = new ApiActionDelegateServiceImpl();

        String agentId = "agent_123";
        ActionRequest request1 = ActionRequest.newBuilder()
                .setAction(RequestAction.EXECUTE)
                .setExecuteParams(ExecuteParams.newBuilder()
                        .setCommandLine("watch demo.MathGame primeFactors -x 2 -n 1")
                        .build())
                .build();
        apiActionDelegateService.execCommand(agentId, request1);

    }
}
