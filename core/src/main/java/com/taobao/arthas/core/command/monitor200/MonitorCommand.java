package com.taobao.arthas.core.command.monitor200;


import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 方法监控统计命令
 * 用于监控方法调用的统计信息，包括调用总次数、成功次数、失败次数、平均响应时间、失败率等
 *
 * @author vlinux
 */
@Name("monitor")
@Summary("Monitor method execution statistics, e.g. total/success/failure count, average rt, fail rate, etc. ")
@Description("\nExamples:\n" +
        "  monitor org.apache.commons.lang.StringUtils isBlank\n" +
        "  monitor org.apache.commons.lang.StringUtils isBlank -c 5\n" +
        "  monitor org.apache.commons.lang.StringUtils isBlank params[0]!=null\n" +
        "  monitor -b org.apache.commons.lang.StringUtils isBlank params[0]!=null\n" +
        "  monitor -E org\\.apache\\.commons\\.lang\\.StringUtils isBlank\n" +
        Constants.WIKI + Constants.WIKI_HOME + "monitor")
public class MonitorCommand extends EnhancerCommand {

    /** 类名匹配模式 */
    private String classPattern;
    /** 方法名匹配模式 */
    private String methodPattern;
    /** 条件表达式 */
    private String conditionExpress;
    /** 统计周期（秒），默认60秒 */
    private int cycle = 60;
    /** 是否使用正则表达式匹配 */
    private boolean isRegEx = false;
    /** 执行次数上限，默认100次 */
    private int numberOfLimit = 100;
    /** 是否在方法调用前判断条件表达式 */
    private boolean isBefore = false;

    /**
     * 设置类名匹配模式
     * @param classPattern 类名匹配模式
     */
    @Argument(argName = "class-pattern", index = 0)
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = StringUtils.normalizeClassName(classPattern);
    }

    /**
     * 设置方法名匹配模式
     * @param methodPattern 方法名匹配模式
     */
    @Argument(argName = "method-pattern", index = 1)
    @Description("Method of Pattern Matching")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    /**
     * 设置条件表达式
     * 用于过滤方法调用，只有满足条件的方法调用才会被统计
     * @param conditionExpress 条件表达式
     */
    @Argument(argName = "condition-express", index = 2, required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    /**
     * 设置统计周期
     * @param cycle 统计周期（秒），默认60秒
     */
    @Option(shortName = "c", longName = "cycle")
    @Description("The monitor interval (in seconds), 60 seconds by default")
    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    /**
     * 设置是否使用正则表达式匹配
     * @param regEx true表示使用正则表达式，false表示使用通配符（默认）
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置执行次数上限
     * @param numberOfLimit 执行次数上限
     */
    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    /**
     * 设置是否在方法调用前判断条件表达式
     * @param before true表示在方法调用前判断，false表示在方法调用后判断
     */
    @Option(shortName = "b", longName = "before", flag = true)
    @Description("Evaluate the condition-express before method invoke")
    public void setBefore(boolean before) {
        isBefore = before;
    }

    /**
     * 获取类名匹配模式
     * @return 类名匹配模式
     */
    public String getClassPattern() {
        return classPattern;
    }

    /**
     * 获取方法名匹配模式
     * @return 方法名匹配模式
     */
    public String getMethodPattern() {
        return methodPattern;
    }

    /**
     * 获取条件表达式
     * @return 条件表达式
     */
    public String getConditionExpress() {
        return conditionExpress;
    }

    /**
     * 获取统计周期
     * @return 统计周期（秒）
     */
    public int getCycle() {
        return cycle;
    }

    /**
     * 是否使用正则表达式
     * @return true表示使用正则表达式
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    /**
     * 获取执行次数上限
     * @return 执行次数上限
     */
    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    /**
     * 是否在方法调用前判断条件
     * @return true表示在方法调用前判断
     */
    public boolean isBefore() {
        return isBefore;
    }

    /**
     * 获取类名匹配器
     * 懒加载方式创建，只有在需要时才创建
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
     * 懒加载方式创建，只有在需要时才创建
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
     * 懒加载方式创建，只有在需要时才创建
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
     * 获取建议监听器
     * 创建监控建议监听器，并设置suspend/resume处理器
     * @param process 命令处理进程
     * @return 建议监听器
     */
    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        // 创建监控建议监听器
        final AdviceListener listener = new MonitorAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
        /*
         * 通过handle回调，在suspend时停止timer，resume时重启timer
         */
        // 设置暂停处理器：当命令暂停时，停止定时器
        process.suspendHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                listener.destroy();
            }
        });
        // 设置恢复处理器：当命令恢复时，重启定时器
        process.resumeHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                listener.create();
            }
        });
        return listener;
    }
}
