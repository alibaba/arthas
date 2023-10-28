package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.SessionModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.UserStatUtil;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import com.alibaba.arthas.tunnel.client.TunnelClient;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查看会话状态命令
 *
 * @author vlinux on 15/5/3.
 */
@Name("session")
@Summary("Display current session information")
@Description(value =Constants.EXAMPLE +
        "  session -s\n" +
        "  session -I *\n" +
        "  session -I '${arthas-cmd}-args=10'\n" +
        "  session -I 'history-args=10'\n" +
        "  session -I 'trace-args=-n 3 -v'\n" +
        Constants.WIKI + Constants.WIKI_HOME + "session")
public class SessionCommand extends AnnotatedCommand {

    private static List<String> RESERVED_WORDS = Arrays.asList( "id", "tty","pid","server","lastAccessedTime", 
        "createTime","subject", "instrumentation", "arthas-command-manager");

    private boolean showSessionData = true;

    private boolean showReservedNames = false;

    private List<String> customSesionData;

    @Option(shortName = "H", longName = "hiddenSessionData", flag = true)
    @Description("Hidden artahs session data")
    public void setHiddenSessionData(final boolean hiddenSessionData) {
        this.showSessionData = !hiddenSessionData;
    }

    @Option(shortName = "A", longName = "showAll", flag = true)
    @Description("Show all session data")
    public void setShowAll(final boolean showReservedNames) {
        this.showReservedNames = showReservedNames;
    }

    @Option(shortName = "I", longName = "input")
    @Description("Input cutom session data, ie: 'trace-args=-n 3' or * for cleanup")
    public void setCustomSessionData(final List<String> customSesionData) {
        this.customSesionData = customSesionData;
    }

    @Override
    public void process(CommandProcess process) {
        SessionModel result = new SessionModel();
        Session session = process.session();
        result.setJavaPid(session.getPid());
        result.setSessionId(session.getSessionId());
        if (customSesionData != null) {
            process.echoTips("customSesionData: " + customSesionData + "\n");
            if (customSesionData.size() == 1 && "*".equals(customSesionData.get(0))) {
                final Map<String, Object> sessionData = session.cloneSessionData();
                for (final Map.Entry<String, Object> entry : sessionData.entrySet()) {
                     if (!RESERVED_WORDS.contains(entry.getKey())) {
                        session.remove(entry.getKey());
                     }
                }
            } else {
                final Pattern pattern = Pattern.compile("\\s*([^:=]+)\\s*[:=]\\s*(.+)");
                for (final String pv : customSesionData) {
                    final Matcher matcher = pattern.matcher(pv);
                    if (matcher.find()) {
                        final int groupCount = matcher.groupCount();
                        if (groupCount >= 2) {
                            final String name = matcher.group(1);
                            if (RESERVED_WORDS.contains(name)){
                                process.echoTips("ignore: " + name);
                                return;
                            }
                            session.put(name, matcher.group(2));
                        }
                    }
                }
            }
            showSessionData = true;
        }
        if (showSessionData) {
            final Map<String, Object> sessionData = session.cloneSessionData();
            if (!this.showReservedNames) {
                final Iterator<Entry<String, Object>> iter = sessionData.entrySet().iterator();
                while(iter.hasNext()) {
                    if (RESERVED_WORDS.contains(iter.next().getKey())) {
                        iter.remove();
                        continue;
                    }
                }
            }
            result.setSessionData(sessionData);
        }
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
