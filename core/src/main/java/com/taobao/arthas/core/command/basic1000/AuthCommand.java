package com.taobao.arthas.core.command.basic1000;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.security.BasicPrincipal;
import com.taobao.arthas.core.security.SecurityAuthenticator;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * TODO 支持更多的鉴权方式。目前只支持 username/password的方式
 * 
 * @author hengyunabc 2021-03-03
 *
 */
// @formatter:off
@Name(ArthasConstants.AUTH)
@Summary("Authenticates the current session")
@Description(Constants.EXAMPLE +
        "  auth\n" +
        "  auth <password>\n" +
        "  auth --username <username> <password>\n"
        + Constants.WIKI + Constants.WIKI_HOME + ArthasConstants.AUTH)
//@formatter:on
public class AuthCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(AuthCommand.class);

    private String username;
    private String password;
    private SecurityAuthenticator authenticator = ArthasBootstrap.getInstance().getSecurityAuthenticator();

    @Argument(argName = "password", index = 0, required = false)
    @Description("password")
    public void setPassword(String password) {
        this.password = password;
    }

    @Option(shortName = "n", longName = "username")
    @Description("username, default value 'arthas'")
    @DefaultValue(ArthasConstants.DEFAULT_USERNAME)
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void process(CommandProcess process) {
        int status = 0;
        String message = "";
        try {
            Session session = process.session();
            if (username == null) {
                status = 1;
                message = "username can not be empty!";
                return;
            }
            if (password == null) { // 没有传入passowrd参数时，打印当前结果
                boolean authenticated = session.get(ArthasConstants.SUBJECT_KEY) != null;
                boolean needLogin = this.authenticator.needLogin();

                message = "Authentication result: " + authenticated + ", Need authentication: " + needLogin;
                if (needLogin && !authenticated) {
                    status = 1;
                }
                return;
            } else {
                // 尝试进行鉴权
                BasicPrincipal principal = new BasicPrincipal(username, password);
                try {
                    Subject subject = authenticator.login(principal);
                    if (subject != null) {
                        // 把subject 保存到 session里，后续其它命令则可以正常执行
                        session.put(ArthasConstants.SUBJECT_KEY, subject);
                        message = "Authentication result: " + true + ", username: " + username;
                    } else {
                        status = 1;
                        message = "Authentication result: " + false + ", username: " + username;
                    }
                } catch (LoginException e) {
                    logger.error("Authentication error, username: {}", username, e);
                }
            }
        } finally {
            process.end(status, message);
        }
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

}
