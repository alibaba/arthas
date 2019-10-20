package com.taobao.arthas.core.command.basic1000;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
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
@Description(Constants.EXAMPLE + "  history\n" + "  history -c\n" + "  history 5\n"
   +"  history -c -p history\n"
   +"  history -u 10 \n"
   +"  history -c -p '(history|shutdown|stop|exit)' -u 10\n"
    )
public class HistoryCommand extends AnnotatedCommand {
    boolean clear = false;
    int n = -1;
    private boolean uniq = false;
    private Pattern  pattern;

    @Option( shortName = "p", longName = "pattern")
    @Description("command pattern to match")
    public void setPattern(String pattern) {
        this.pattern =  pattern == null || pattern.isEmpty() ? null :  Pattern.compile(pattern);
    }

    @Option(shortName = "c", longName = "clear")
    @Description("clear history")
    public void setClear(boolean clear) {
        this.clear = clear;
    }

    @Option(shortName = "u", longName = "uniq")
    @Description("uniq history")
    public void setUniq(boolean uniq) {
        this.uniq = uniq;
    }
    
    @Argument(index = 0, argName = "n", required = false)
    @Description("how many history commnads to display")
    public void setNumber(int n) {
        this.n = n;
    }

    @Override
    public void process(CommandProcess process) {
        Session session = process.session();
        Object termObject = session.get(Session.TTY);
        if (termObject != null && termObject instanceof TermImpl) {
            TermImpl term = (TermImpl) termObject;
            Readline readline = term.getReadline();
            List<int[]> history = readline.getHistory();
            if (clear) {
                final List<int[]> newHistory = new ArrayList<int[]>();
                int clearCount = 0;
                final int ulimit = uniq && n > 0 ? n  : 0;
                if (pattern != null || ( ulimit > 0) ) {
                  List<String> cmdList = new ArrayList<String>();
                  for(int[] line : history) {
                      final String cmd = toCmd(line).trim();
                      boolean flag = pattern != null ? pattern.matcher(cmd).find()  : false;
                      if (!flag && (ulimit > 0)) {
                           final int cSize = cmdList.size();
                           final List<String> list = cSize <= ulimit  ? cmdList :   cmdList.subList(cSize -ulimit ,cSize);
                           flag = list.contains(cmd);
                      }
                      if (flag) {
                        clearCount ++;
                      } else {
                        cmdList.add(cmd);
                        newHistory.add(line);
                      }
                  }
                } else {
                  clearCount = history.size();
                }
                process.write("clear history count:"+clearCount+"\n");
                readline.setHistory(newHistory); 
            } else if (uniq) {
              int size = history.size();
              if (n < 0) {
                  n = size;
              }
              Set<String> cmdList = new LinkedHashSet<String>();
              for (int i = 0; i < n; ++i) {
                  int[] line = history.get(n - i - 1);
                  final String cmd = toCmd(line);
                  cmdList.add(cmd);
              }
              StringBuilder sb = new StringBuilder();
              for (String c : cmdList) {
                sb.append(c).append('\n');
              }
              process.write(sb.toString());
            } else {
                StringBuilder sb = new StringBuilder();
                int size = history.size();
                if (n < 0) {
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
        }

        process.end();
    }

    protected String toCmd(int[] line) {
      StringBuilder sb = new StringBuilder();
      Helper.appendCodePoints(line, sb);
      final String cmd = sb.toString();
      return cmd;
    }
}
