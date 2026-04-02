package com.taobao.arthas.core.command.basic1000;

import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.HistoryModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.history.HistoryManager;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.impl.TermImpl;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import io.termd.core.readline.Readline;
import io.termd.core.util.Helper;

/**
 * History命令类
 * 用于显示和管理命令历史记录
 * 支持显示历史命令、清空历史记录等操作
 *
 * @author hengyunabc 2018-11-18
 *
 */
@Name("history")
@Summary("Display command history")
@Description(Constants.EXAMPLE + "  history\n" + "  history -c\n" + "  history 5\n")
public class HistoryCommand extends AnnotatedCommand {
    // 是否清空历史记录
    boolean clear = false;
    // 要显示的历史命令数量，-1表示显示所有
    int n = -1;

    /**
     * 设置是否清空历史记录
     *
     * @param clear 是否清空历史记录
     */
    @Option(shortName = "c", longName = "clear", flag = true , acceptValue = false)
    @Description("clear history")
    public void setClear(boolean clear) {
        this.clear = clear;
    }

    /**
     * 设置要显示的历史命令数量
     *
     * @param n 历史命令数量
     */
    @Argument(index = 0, argName = "n", required = false)
    @Description("how many history commands to display")
    public void setNumber(int n) {
        this.n = n;
    }

    /**
     * 处理命令执行
     * 根据是否为Term终端或HTTP API调用，采用不同的历史记录处理方式
     *
     * @param process 命令处理进程对象
     */
    @Override
    public void process(CommandProcess process) {
        // 获取当前会话
        Session session = process.session();
        // TODO 修改term history实现方式，统一使用HistoryManager
        // 获取终端对象
        Object termObject = session.get(Session.TTY);
        // 如果是TermImpl终端实例
        if (termObject instanceof TermImpl) {
            TermImpl term = (TermImpl) termObject;
            // 获取Readline对象
            Readline readline = term.getReadline();
            // 获取历史记录
            List<int[]> history = readline.getHistory();

            // 如果需要清空历史记录
            if (clear) {
                // 设置为空列表，清空历史记录
                readline.setHistory(new ArrayList<int[]>());
            } else {
                // 构建历史记录输出字符串
                StringBuilder sb = new StringBuilder();

                // 获取历史记录总数
                int size = history.size();
                // 如果n小于0或大于总数，则显示所有历史记录
                if (n < 0 || n > size) {
                    n = size;
                }

                // 遍历历史记录（从最新到最旧）
                for (int i = 0; i < n; ++i) {
                    // 获取历史记录行（倒序获取）
                    int[] line = history.get(n - i - 1);
                    // 格式化输出行号
                    sb.append(String.format("%5s  ", size - (n - i - 1)));
                    // 将命令内容追加到字符串
                    Helper.appendCodePoints(line, sb);
                    // 添加换行符
                    sb.append('\n');
                }

                // 写入输出
                process.write(sb.toString());
            }
        } else {
            // HTTP API调用方式
            // 获取历史管理器
            HistoryManager historyManager = ArthasBootstrap.getInstance().getHistoryManager();
            if (clear) {
                // 清空历史记录
                historyManager.clearHistory();
            } else {
                // 获取历史记录列表
                List<String> history = historyManager.getHistory();
                // 将历史记录添加到处理结果中
                process.appendResult(new HistoryModel(new ArrayList<String>(history)));
            }
        }

        // 结束命令处理
        process.end();
    }
}
