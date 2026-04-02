package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.ScriptSupportCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Hidden;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Groovy脚本增强命令
 *
 * 该命令允许用户使用Groovy脚本来增强和监控Java方法。
 * 由于严重的内存泄漏问题，Arthas 3.0中完全放弃了对Groovy的支持。
 *
 * 使用示例：
 *   groovy -E org\.apache\.commons\.lang\.StringUtils isBlank /tmp/watch.groovy
 *   groovy org.apache.commons.lang.StringUtils isBlank /tmp/watch.groovy
 *   groovy *StringUtils isBlank /tmp/watch.groovy
 *
 * @author vlinux on 15/5/31.
 * @deprecated 已废弃，Groovy支持因内存泄漏问题被移除
 */
@Name("groovy")
@Hidden // 隐藏命令，不在帮助列表中显示
@Summary("Enhanced Groovy")
@Description("Examples:\n" +
        "  groovy -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank /tmp/watch.groovy\n" +
        "  groovy org.apache.commons.lang.StringUtils isBlank /tmp/watch.groovy\n" +
        "  groovy *StringUtils isBlank /tmp/watch.groovy\n" +
        "\n" +
        "WIKI:\n" +
        "  middleware-container/arthas/wikis/cmds/groovy")
@Deprecated
public class GroovyScriptCommand extends EnhancerCommand implements ScriptSupportCommand {

    // 类名匹配模式
    private String classPattern;

    // 方法名匹配模式
    private String methodPattern;

    // Groovy脚本文件路径
    private String scriptFilepath;

    // 是否使用正则表达式匹配（默认为false，使用通配符匹配）
    private boolean isRegEx = false;

    /**
     * 设置类名匹配模式
     *
     * @param classPattern 类名匹配模式，支持通配符或正则表达式
     */
    @Argument(index = 0, argName = "class-pattern")
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置方法名匹配模式
     *
     * @param methodPattern 方法名匹配模式，支持通配符或正则表达式
     */
    @Argument(index = 1, argName = "method-pattern")
    @Description("Method of Pattern Matching")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    /**
     * 设置Groovy脚本文件路径
     *
     * @param scriptFilepath Groovy脚本的文件路径
     */
    @Argument(index = 2, argName = "script-filepath")
    @Description("Filepath of Groovy script")
    public void setScriptFilepath(String scriptFilepath) {
        this.scriptFilepath = scriptFilepath;
    }

    /**
     * 设置是否使用正则表达式匹配
     *
     * @param regEx true表示使用正则表达式，false表示使用通配符匹配（默认）
     */
    @Option(shortName = "E", longName = "regex")
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 获取类名匹配模式
     *
     * @return 类名匹配模式字符串
     */
    public String getClassPattern() {
        return classPattern;
    }

    /**
     * 获取方法名匹配模式
     *
     * @return 方法名匹配模式字符串
     */
    public String getMethodPattern() {
        return methodPattern;
    }

    /**
     * 获取Groovy脚本文件路径
     *
     * @return 脚本文件路径字符串
     */
    public String getScriptFilepath() {
        return scriptFilepath;
    }

    /**
     * 判断是否使用正则表达式匹配
     *
     * @return true表示使用正则表达式，false表示使用通配符
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    /**
     * 获取类名匹配器
     *
     * @return 类名匹配器对象
     * @throws UnsupportedOperationException 因为Groovy命令已废弃，此方法抛出不支持异常
     */
    @Override
    protected Matcher getClassNameMatcher() {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }

    /**
     * 获取类名排除匹配器
     *
     * @return 类名排除匹配器对象
     * @throws UnsupportedOperationException 因为Groovy命令已废弃，此方法抛出不支持异常
     */
    @Override
    protected Matcher getClassNameExcludeMatcher() {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }

    /**
     * 获取方法名匹配器
     *
     * @return 方法名匹配器对象
     * @throws UnsupportedOperationException 因为Groovy命令已废弃，此方法抛出不支持异常
     */
    @Override
    protected Matcher getMethodNameMatcher() {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }

    /**
     * 获取建议监听器
     *
     * @param process 命令进程对象
     * @return 建议监听器对象
     * @throws UnsupportedOperationException 因为Groovy命令已废弃，此方法抛出不支持异常
     */
    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }
}
