package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.SystemEnvModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 系统环境变量查看命令类，用于显示JVM进程的系统环境变量<br/>
 *
 * 该命令可以：
 * - 显示所有系统环境变量（不带参数时）
 * - 显示指定的单个环境变量（带环境变量名参数）
 * - 支持Tab键自动补全环境变量名
 *
 * 使用示例：
 * - sysenv: 显示所有环境变量
 * - sysenv USER: 显示USER环境变量的值
 *
 * @author hengyunabc 2018-11-09
 *
 */
@Name("sysenv")
@Summary("Display the system env.")
@Description(Constants.EXAMPLE + "  sysenv\n" + "  sysenv USER\n" + Constants.WIKI + Constants.WIKI_HOME + "sysenv")
public class SystemEnvCommand extends AnnotatedCommand {

    /**
     * 环境变量名称
     * 如果为空，则显示所有环境变量
     * 如果指定了值，则只显示指定的环境变量
     */
    private String envName;

    /**
     * 设置要查看的环境变量名称
     *
     * @param envName 环境变量名称，如果为null或空字符串则显示所有环境变量
     */
    @Argument(index = 0, argName = "env-name", required = false)
    @Description("env name")
    public void setOptionName(String envName) {
        this.envName = envName;
    }

    /**
     * 处理sysenv命令的执行逻辑
     * 根据参数显示所有或指定的环境变量
     *
     * @param process 命令处理进程对象，包含会话信息和执行上下文
     */
    @Override
    public void process(CommandProcess process) {
        try {
            // 创建SystemEnvModel对象用于存储环境变量信息
            SystemEnvModel result = new SystemEnvModel();

            // 判断是否指定了环境变量名称
            if (StringUtils.isBlank(envName)) {
                // 如果未指定环境变量名称，则显示所有系统环境变量
                result.putAll(System.getenv());
            } else {
                // 如果指定了环境变量名称，则只显示该环境变量的值
                String value = System.getenv(envName);
                result.put(envName, value);
            }

            // 将结果附加到命令处理进程中
            process.appendResult(result);

            // 正常结束命令处理
            process.end();
        } catch (Throwable t) {
            // 如果在获取环境变量过程中发生异常，以错误状态结束命令处理
            process.end(-1, "Error during setting system env: " + t.getMessage());
        }
    }

    /**
     * 提供命令行自动补全功能
     * 当用户输入sysenv命令并按Tab键时，自动补全环境变量名称
     *
     * 首先尝试在sysenv命令作用域内完成补全。
     * 如果补全失败，则委托给父类处理。
     *
     * @param completion 补全对象，包含补全上下文信息
     */
    @Override
    public void complete(Completion completion) {
        // 调用CompletionUtils工具类完成自动补全
        // 从所有系统环境变量的键集合中进行匹配补全
        CompletionUtils.complete(completion, System.getenv().keySet());
    }

}
