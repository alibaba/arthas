package com.alibaba.arthas.nat.agent.server.http;

import com.alibaba.arthas.nat.agent.core.JvmAttachmentHandler;
import com.alibaba.arthas.nat.agent.core.ListJvmProcessHandler;
import com.alibaba.arthas.nat.agent.core.MonitorTargetPidHandler;
import com.alibaba.arthas.nat.agent.server.dto.JavaProcessInfoDTO;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: hello world
 * @author：flzjkl
 * @date: 2024-08-01 7:32
 */
public class HttpNativeAgentHandler {

    private static final String OPERATION_KEY = "operation";
    private static final String PID_KEY = "pid";
    private static final String LIST_PROCESS_OPERATION = "listProcess";
    private static final String ATTACH_JVM_OPERATION = "attachJvm";
    private static final String MONITOR_OPERATION = "monitor";

    public FullHttpResponse handle(ChannelHandlerContext ctx, FullHttpRequest request) {
        String content = request.content().toString(StandardCharsets.UTF_8);
        FullHttpResponse resp = null;
        Map<String, Object> bodyMap = JSON.parseObject(content, new TypeReference<Map<String, Object>>() {
        });
        String operation = (String) bodyMap.get(OPERATION_KEY);
        Integer pid = (Integer) bodyMap.get(PID_KEY);

        if (LIST_PROCESS_OPERATION.equals(operation)) {
            resp = doListProcess(ctx, request);
        }

        if (ATTACH_JVM_OPERATION.equals(operation)) {
            resp = doAttachJvm(ctx, request, pid);
        }

        if (MONITOR_OPERATION.equals(operation)) {
            resp = doMonitor(ctx, request, pid);
        }

        return resp;
    }

    private FullHttpResponse doMonitor(ChannelHandlerContext ctx, FullHttpRequest request, Integer pid) {
        boolean monitorSuccess = MonitorTargetPidHandler.monitorTargetPid(pid);
        String attachSuccessPid = monitorSuccess ? pid + "" : -1 + "";
        DefaultFullHttpResponse response = buildHttpCorsResponse(attachSuccessPid);
        return response;
    }

    private FullHttpResponse doAttachJvm(ChannelHandlerContext ctx, FullHttpRequest request, Integer pid) {
        String httpPort = "";
        try {
            httpPort = JvmAttachmentHandler.attachJvmByPid(pid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String attachSuccessMsg = httpPort;

        DefaultFullHttpResponse response = buildHttpCorsResponse(attachSuccessMsg);
        return response;
    }

    private FullHttpResponse doListProcess(ChannelHandlerContext ctx, FullHttpRequest request) {
        Map<Long, String> processMap = null;
        try {
            processMap = ListJvmProcessHandler.listJvmProcessByInvoke();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<JavaProcessInfoDTO> javaProcessInfoList = new ArrayList<>();
        if (processMap != null) {
            processMap.forEach((pid, applicationName) -> {
                if (!"".equals(applicationName.replace(pid + " ", ""))) {
                    javaProcessInfoList.add(new JavaProcessInfoDTO(applicationName.replace(pid + " ", ""), pid.intValue()));
                }
            });
        }

        String processJson = JSON.toJSONString(javaProcessInfoList);

        DefaultFullHttpResponse response = buildHttpCorsResponse(processJson);
        return response;
    }


    public DefaultFullHttpResponse buildHttpCorsResponse (String msg) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(msg.getBytes(CharsetUtil.UTF_8)));

        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, 3600L);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization, X-Requested-With, Accept, Origin");

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        return response;
    }

}
