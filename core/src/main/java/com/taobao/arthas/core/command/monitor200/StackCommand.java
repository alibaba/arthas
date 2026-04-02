package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 栈追踪命令
 * <p>
 * 用于输出指定类和方法的调用栈信息。
 * 当目标方法被调用时，会输出当前线程的完整调用栈，帮助分析方法调用路径。
 * 支持条件表达式过滤，可以在满足特定条件时才输出调用栈。
 * <p>
 * 使用场景：
 * 1. 查看某个方法是从哪里被调用的
 * 2. 分析方法调用链路
 * 3. 排查调用关系问题
 *
 * @author vlinux
 * @author hengyunabc 2016-10-31
 */
@Name("stack")
@Summary("Display the stack trace for the specified class and method")
@Description(Constants.EXPRESS_DESCRIPTION + Constants.EXAMPLE +
        "  stack org.apache.commons.lang.StringUtils isBlank\n" +
        "  stack *StringUtils isBlank\n" +
        "  stack *StringUtils isBlank params[0].length==1\n" +
        "  stack *StringUtils isBlank '#cost>100'\n" +
        "  stack -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank\n" +
        Constants.WIKI + Constants.WIKI_HOME + "stack")
public class StackCommand extends EnhancerCommand {
    /** 类名匹配模式，支持通配符 */
    private String classPattern;
    /** 方法名匹配模式，支持通配符 */
    private String methodPattern;
    /** 条件表达式，用于过滤调用 */
    private String conditionExpress;
    /** 是否使用正则表达式匹配 */
    private boolean isRegEx = false;
    /** 输出次数限制，默认100次，避免输出过多数据 */
    private int numberOfLimit = 100;

    /**
     * 设置类名匹配模式
     *
     * @param classPattern 类名模式，支持通配符*
     */
    @Argument(index = 0, argName = "class-pattern")
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置方法名匹配模式
     *
     * @param methodPattern 方法名模式，支持通配符*
     */
    @Argument(index = 1, argName = "method-pattern", required = false)
    @Description("Method of Pattern Matching")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    /**
     * 设置条件表达式
     * 只有满足条件时才输出调用栈
     *
     * @param conditionExpress 条件表达式，如：params[0].length==1 或 #cost>100
     */
    @Argument(index = 2, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    /**
     * 设置是否使用正则表达式匹配
     * 默认使用通配符匹配，开启此选项后使用正则表达式
     *
     * @param regEx true表示使用正则表达式
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置输出次数限制
     * 达到此限制后命令会自动终止，避免产生过多数据
     *
     * @param numberOfLimit 最大输出次数
     */
    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    /**
     * 设置类加载器的哈希码
     * 用于指定在哪个类加载器中查找类
     *
     * @param hashCode 类加载器的哈希码
     */
    @Override
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        super.setHashCode(hashCode);
    }

    /**
     * 获取类名匹配模式
     *
     * @return 类名模式
     */
    public String getClassPattern() {
        return classPattern;
    }

    /**
     * 获取方法名匹配模式
     *
     * @return 方法名模式
     */
    public String getMethodPattern() {
        return methodPattern;
    }

    /**
     * 获取条件表达式
     *
     * @return 条件表达式
     */
    public String getConditionExpress() {
        return conditionExpress;
    }

    /**
     * 是否使用正则表达式匹配
     *
     * @return true表示使用正则表达式
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    /**
     * 获取输出次数限制
     *
     * @return 最大输出次数
     */
    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    /**
     * 获取类名匹配器
     * 使用懒加载模式，第一次调用时创建
     *
     * @return 类名匹配器
     */
    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    /**
     * 获取类名排除匹配器
     * 用于排除某些不需要匹配的类
     *
     * @return 类名排除匹配器
     */
    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    /**
     * 获取方法名匹配器
     * 使用懒加载模式，第一次调用时创建
     *
     * @return 方法名匹配器
     */
    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    /**
     * 创建监听器
     * 监听目标方法的调用，并在满足条件时输出调用栈
     *
     * @param process 命令处理进程
     * @return 栈追踪监听器
     */
    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new StackAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

}
