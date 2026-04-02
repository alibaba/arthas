package com.alibaba.arthas.tunnel.server.app.web;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Arthas Agent数据统计控制器
 * 提供Arthas Agent执行数据回报的接口
 *
 * @author hengyunabc 2019-09-24
 *
 */
@Controller
public class StatController {
    // 日志记录器
    private final static Logger logger = LoggerFactory.getLogger(StatController.class);

    /**
     * 接收并记录Arthas Agent的执行统计信息
     * 这是一个演示接口，用于接收Agent上报的命令执行数据
     *
     * @param ip Agent所在服务器的IP地址（必填）
     * @param version Arthas版本号（必填）
     * @param agentId Agent的唯一标识（可选）
     * @param command 执行的命令名称（必填）
     * @param arguments 命令执行参数（可选，默认为空字符串）
     * @return 包含执行结果的Map对象，成功时包含success=true
     */
    @RequestMapping(value = "/api/stat")
    @ResponseBody
    public Map<String, Object> execute(@RequestParam(value = "ip", required = true) String ip,
            @RequestParam(value = "version", required = true) String version,
            @RequestParam(value = "agentId", required = false) String agentId,
            @RequestParam(value = "command", required = true) String command,
            @RequestParam(value = "arguments", required = false, defaultValue = "") String arguments) {

        // 记录Agent上报的统计信息
        logger.info("arthas stat, ip: {}, version: {}, agentId: {}, command: {}, arguments: {}", ip, version, agentId, command, arguments);

        // 创建结果Map
        Map<String, Object> result = new HashMap<>();

        // 设置成功标志
        result.put("success", true);

        // 返回结果
        return result;
    }
}
