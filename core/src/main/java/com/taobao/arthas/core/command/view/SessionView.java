package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SessionModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

/**
 * 会话结果的 Term/Tty 视图
 * 用于显示当前 Arthas 会话的详细信息，包括进程 ID、会话 ID、代理信息等
 *
 * @author gongdewei 2020/3/27
 */
public class SessionView extends ResultView<SessionModel> {

    /**
     * 渲染会话信息
     * 将会话详细信息以表格形式展示
     *
     * @param process 命令处理进程，用于写入输出
     * @param result 会话信息模型
     */
    @Override
    public void draw(CommandProcess process, SessionModel result) {
        // 创建表格用于展示会话详情
        // 设置单元格左右内边距为1
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表头行，包含 "Name" 和 "Value" 两列，使用粗体样式
        table.row(true, label("Name").style(Decoration.bold.bold()), label("Value").style(Decoration.bold.bold()));

        // 添加 Java 进程 ID 行
        table.row("JAVA_PID", "" + result.getJavaPid())
                // 添加会话 ID 行
                .row("SESSION_ID", "" + result.getSessionId());

        // 如果存在 Agent ID，添加到表格
        if (result.getAgentId() != null) {
            table.row("AGENT_ID", "" + result.getAgentId());
        }

        // 如果存在隧道服务器信息，添加相关行
        if (result.getTunnelServer() != null) {
            // 添加隧道服务器地址
            table.row("TUNNEL_SERVER", "" + result.getTunnelServer());
            // 添加隧道连接状态
            table.row("TUNNEL_CONNECTED", "" + result.isTunnelConnected());
        }

        // 如果存在统计 URL，添加到表格
        if (result.getStatUrl() != null) {
            table.row("STAT_URL", result.getStatUrl());
        }

        // 如果存在用户 ID，添加到表格
        if (result.getUserId() != null) {
            table.row("USER_ID", result.getUserId());
        }

        // 渲染表格并输出，根据终端宽度自动调整
        process.write(RenderUtil.render(table, process.width()));
    }

}
