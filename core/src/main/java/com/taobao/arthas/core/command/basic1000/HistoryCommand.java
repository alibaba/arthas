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
 *
 * @author hengyunabc 2018-11-18
 *
 */
@Name("history")
@Summary("Display command history")
@Description(Constants.EXAMPLE + "  history\n" + "  history -c\n" + "  history 5\n")
public class HistoryCommand extends AnnotatedCommand {
    boolean clear = false;
    int n = -1;

    @Option(shortName = "c", longName = "clear", flag = true , acceptValue = false)
    @Description("clear history")
    public void setClear(boolean clear) {
        this.clear = clear;
    }

    @Argument(index = 0, argName = "n", required = false)
    @Description("how many history commnads to display")
    public void setNumber(int n) {
        this.n = n;
    }

    @Override
    public void process(CommandProcess process) {
        Session session = process.session();
        //TODO 修改term history实现方式，统一使用HistoryManager
        Object termObject = session.get(Session.TTY);
        if (termObject instanceof TermImpl) {
            TermImpl term = (TermImpl) termObject;
            Readline readline = term.getReadline();
            List<int[]> history = readline.getHistory();

            if (clear) {
                readline.setHistory(new ArrayList<int[]>());
            } else {
                StringBuilder sb = new StringBuilder();

                int size = history.size();
                if (n < 0 || n > size) {
                    n = size;
                }

                for (int i = 0; i < n; ++i) {
                    int[] line = history.get(n - i - 1);
                    sb.append(String.format("%5s  ", size - (n - i - 1)));
                    Helper.appendCodePoints(line, sb);
                    sb.append('\n');
                }

                process.write(sb.toString());
            }
        } else {
            //http api
            HistoryManager historyManager = ArthasBootstrap.getInstance().getHistoryManager();
            if (clear) {
                historyManager.clearHistory();
            } else {
                List<String> history = historyManager.getHistory();
                process.appendResult(new HistoryModel(new ArrayList<String>(history)));
            }
        }

        process.end();
    }
}
