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
 * 查看会话状态命令
 *
 * @author vlinux on 15/5/3.
 */
@Name("session")
@Summary("Display current session information")
public class SessionCommand extends AnnotatedCommand {

    @Override
    public void process(CommandProcess process) {
        SessionModel result = new SessionModel();
        Session session = process.session();
        result.setJavaPid(session.getPid());
        result.setSessionId(session.getSessionId());

        //tunnel
        TunnelClient tunnelClient = ArthasBootstrap.getInstance().getTunnelClient();
        if (tunnelClient != null) {
            String id = tunnelClient.getId();
            if (id != null) {
                result.setAgentId(id);
            }
            result.setTunnelServer(tunnelClient.getTunnelServerUrl());
            result.setTunnelConnected(tunnelClient.isConnected());
        }

        //statUrl
        String statUrl = UserStatUtil.getStatUrl();
        result.setStatUrl(statUrl);

        process.appendResult(result);
        process.end();
    }

}
