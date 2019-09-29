package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

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
        process.write(RenderUtil.render(sessionTable(process.session()), process.width())).end();
    }

    /*
     * 会话详情
     */
    private Element sessionTable(Session session) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("Name").style(Decoration.bold.bold()), label("Value").style(Decoration.bold.bold()));
        table.row("JAVA_PID", "" + session.getPid()).row("SESSION_ID", "" + session.getSessionId());
        TunnelClient tunnelClient = ArthasBootstrap.getInstance().getTunnelClient();
        if (tunnelClient != null) {
            String id = tunnelClient.getId();
            if (id != null) {
                table.row("AGENT_ID", "" + id);
            }
            table.row("TUNNEL_SERVER", "" + tunnelClient.getTunnelServerUrl());
        }
        String statUrl = UserStatUtil.getStatUrl();
        if (statUrl != null) {
            table.row("STAT_URL", statUrl);
        }
        return table;
    }
}
