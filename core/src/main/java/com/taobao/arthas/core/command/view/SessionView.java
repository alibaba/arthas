package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SessionModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

/**
 * Term / Tty view for session result
 *
 * @author gongdewei 2020/3/27
 */
public class SessionView extends ResultView<SessionModel> {

    @Override
    public void draw(CommandProcess process, SessionModel result) {
        //会话详情
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("Name").style(Decoration.bold.bold()), label("Value").style(Decoration.bold.bold()));
        table.row("JAVA_PID", "" + result.getJavaPid()).row("SESSION_ID", "" + result.getSessionId());
        if (result.getAgentId() != null) {
            table.row("AGENT_ID", "" + result.getAgentId());
        }
        if (result.getTunnelServer() != null) {
            table.row("TUNNEL_SERVER", "" + result.getTunnelServer());
            table.row("TUNNEL_CONNECTED", "" + result.isTunnelConnected());
        }
        if (result.getStatUrl() != null) {
            table.row("STAT_URL", result.getStatUrl());
        }
        process.write(RenderUtil.render(table, process.width()));
    }

}
