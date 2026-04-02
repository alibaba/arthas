package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ResetModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * 重置命令类，用于恢复所有被Arthas增强的类<br/>
 *
 * 该命令可以重置（恢复）被Arthas增强过的类，将其恢复到原始状态。
 * 支持通过类名模式匹配来批量重置特定的类。
 *
 * 使用示例：
 * - reset: 重置所有被增强的类
 * - reset *List: 重置所有类名以List结尾的类
 * - reset -E .*List: 使用正则表达式重置匹配的类
 *
 * @author vlinux on 15/5/29.
 */
@Name("reset")
@Summary("Reset all the enhanced classes")
@Description(Constants.EXAMPLE +
        "  reset\n" +
        "  reset *List\n" +
        "  reset -E .*List\n")
public class ResetCommand extends AnnotatedCommand {
    /**
     * 类名模式匹配字符串
     * 可以是通配符模式或正则表达式，用于指定要重置的类
     */
    private String classPattern;

    /**
     * 是否使用正则表达式进行匹配
     * 默认为false，使用通配符匹配
     */
    private boolean isRegEx = false;

    /**
     * 设置类名模式匹配字符串
     * 该参数用于指定要重置的类名模式
     *
     * @param classPattern 类名模式，可以是通配符模式（如 *List）或正则表达式
     */
    @Argument(index = 0, argName = "class-pattern", required = false)
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置是否使用正则表达式进行匹配
     * 默认情况下使用通配符匹配，设置该选项为true后启用正则表达式匹配
     *
     * @param regEx 是否使用正则表达式，true为启用，false为使用通配符
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 处理reset命令的执行逻辑
     * 该方法负责重置所有被增强的类，将其恢复到原始状态
     *
     * @param process 命令处理进程对象，包含会话信息和执行上下文
     */
    @Override
    public void process(CommandProcess process) {
        // 获取Java Instrumentation实例，用于类的重定义操作
        Instrumentation inst = process.session().getInstrumentation();

        // 根据类名模式和匹配方式创建Matcher对象
        // 如果未指定类名模式，则匹配所有被增强的类
        Matcher matcher = SearchUtils.classNameMatcher(classPattern, isRegEx);

        try {
            // 调用Enhancer.reset方法执行类重置操作
            // enhancerAffect包含重置操作的统计信息（如影响类数量、耗时等）
            EnhancerAffect enhancerAffect = Enhancer.reset(inst, matcher);

            // 将重置结果附加到命令处理进程中，用于输出展示
            process.appendResult(new ResetModel(enhancerAffect));

            // 正常结束命令处理
            process.end();
        } catch (UnmodifiableClassException e) {
            // 如果遇到不可修改的类异常（例如类已经被其他transformer锁定）
            // 则忽略异常，继续结束命令处理
            process.end();
        }
    }
}
