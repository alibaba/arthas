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
 * 认证命令类
 *
 * 该类实现Arthas的用户认证功能，用于保护Arthas服务器的访问权限。
 * 目前支持基于用户名/密码的认证方式，用户在使用需要权限的命令前必须先通过认证。
 *
 * <h2>命令格式</h2>
 * <ul>
 * <li>auth - 查看当前认证状态</li>
 * <li>auth &lt;password&gt; - 使用默认用户名（arthas）和指定密码进行认证</li>
 * <li>auth --username &lt;username&gt; &lt;password&gt; - 使用指定的用户名和密码进行认证</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>
 * # 查看认证状态
 * auth
 *
 * # 使用默认用户名认证
 * auth your_password
 *
 * # 使用指定用户名认证
 * auth --username admin admin_password
 * </pre>
 *
 * TODO 支持更多的鉴权方式。目前只支持 username/password的方式
 *
 * @author hengyunabc 2021-03-03
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
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(AuthCommand.class);

    /**
     * 用户名
     * 从命令行参数或默认值获取
     */
    private String username;

    /**
     * 密码
     * 从命令行参数获取，为必填项
     */
    private String password;

    /**
     * 安全认证器
     * 从ArthasBootstrap获取，负责执行实际的认证逻辑
     */
    private SecurityAuthenticator authenticator = ArthasBootstrap.getInstance().getSecurityAuthenticator();

    /**
     * 设置密码
     * 从命令行参数接收密码值
     *
     * @param password 用户输入的密码
     */
    @Argument(argName = "password", index = 0, required = false)
    @Description("password")
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 设置用户名
     * 从命令行选项接收用户名，如果不指定则使用默认值"arthas"
     *
     * @param username 用户名，默认为"arthas"
     */
    @Option(shortName = "n", longName = "username")
    @Description("username, default value 'arthas'")
    @DefaultValue(ArthasConstants.DEFAULT_USERNAME)
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 处理认证命令
     * 根据用户输入的参数执行不同的操作：
     * 1. 如果只输入auth（没有密码参数）：查询当前认证状态
     * 2. 如果输入了密码：执行认证操作
     *
     * @param process 命令处理上下文，包含session等信息
     */
    @Override
    public void process(CommandProcess process) {
        // 命令执行状态码：0表示成功，非0表示失败
        int status = 0;
        // 命令执行结果消息
        String message = "";
        try {
            // 获取当前会话
            Session session = process.session();
            // 检查用户名是否为空
            if (username == null) {
                status = 1;
                message = "username can not be empty!";
                return;
            }
            // 没有传入password参数时，打印当前认证状态
            if (password == null) {
                // 检查session中是否已保存认证信息
                boolean authenticated = session.get(ArthasConstants.SUBJECT_KEY) != null;
                // 检查是否需要登录（由配置决定）
                boolean needLogin = this.authenticator.needLogin();

                // 构建状态消息
                message = "Authentication result: " + authenticated + ", Need authentication: " + needLogin;
                // 如果需要登录但未认证，设置状态码为失败
                if (needLogin && !authenticated) {
                    status = 1;
                }
                return;
            } else {
                // 传入了密码，尝试进行鉴权
                // 创建认证主体，包含用户名和密码
                BasicPrincipal principal = new BasicPrincipal(username, password);
                try {
                    // 调用认证器进行登录验证
                    Subject subject = authenticator.login(principal);
                    if (subject != null) {
                        // 认证成功：将subject保存到session中
                        // 后续其他命令可以通过session获取认证信息，判断是否有权限执行
                        session.put(ArthasConstants.SUBJECT_KEY, subject);
                        message = "Authentication result: " + true + ", username: " + username;
                    } else {
                        // 认证失败：返回null的情况
                        status = 1;
                        message = "Authentication result: " + false + ", username: " + username;
                    }
                } catch (LoginException e) {
                    // 认证过程中抛出异常（如密码错误）
                    logger.error("Authentication error, username: {}", username, e);
                }
            }
        } finally {
            // 结束命令处理，返回状态码和消息
            // 无论成功或失败，都会执行finally块确保命令正确结束
            process.end(status, message);
        }
    }

    /**
     * 命令自动补全功能
     * 当用户输入命令按Tab键时触发，用于提供智能提示
     *
     * @param completion 补全上下文，包含当前输入的命令信息
     */
    @Override
    public void complete(Completion completion) {
        // 尝试进行文件路径补全
        if (!CompletionUtils.completeFilePath(completion)) {
            // 如果不是文件路径，调用父类的默认补全逻辑
            super.complete(completion);
        }
    }

}
