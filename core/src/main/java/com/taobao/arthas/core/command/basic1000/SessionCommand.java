package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.model.SessionModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

import com.alibaba.arthas.tunnel.client.TunnelClient;

/**
 * 查看会话状态命令类，用于显示当前Arthas会话的详细信息<br/>
 *
 * 该命令展示当前会话的各种信息，包括：
 * - Java进程ID
 * - 会话ID
 * - Agent ID（如果连接了Tunnel服务器）
 * - Tunnel服务器地址
 * - Tunnel连接状态
 * - 统计URL
 * - 用户ID
 *
 * @author vlinux on 15/5/3.
 */
@Name("session")
@Summary("Display current session information")
public class SessionCommand extends AnnotatedCommand {

    /**
     * 处理session命令的执行逻辑
     * 收集并返回当前会话的详细信息
     *
     * @param process 命令处理进程对象，包含会话信息和执行上下文
     */
    @Override
    public void process(CommandProcess process) {
        // 创建SessionModel对象用于存储会话信息
        SessionModel result = new SessionModel();

        // 获取当前会话对象
        Session session = process.session();

        // 设置Java进程ID
        result.setJavaPid(session.getPid());

        // 设置会话ID
        result.setSessionId(session.getSessionId());

        // 处理Tunnel客户端相关信息
        TunnelClient tunnelClient = ArthasBootstrap.getInstance().getTunnelClient();
        if (tunnelClient != null) {
            // 获取Agent ID（Tunnel客户端的唯一标识）
            String id = tunnelClient.getId();
            if (id != null) {
                result.setAgentId(id);
            }
            // 设置Tunnel服务器地址
            result.setTunnelServer(tunnelClient.getTunnelServerUrl());

            // 设置Tunnel连接状态
            result.setTunnelConnected(tunnelClient.isConnected());
        }

        // 处理统计URL信息
        String statUrl = UserStatUtil.getStatUrl();
        result.setStatUrl(statUrl);

        // 处理用户ID信息
        String userId = session.getUserId();
        result.setUserId(userId);

        // 将会话信息结果附加到命令处理进程中
        process.appendResult(result);

        // 结束命令处理
        process.end();
    }

}
