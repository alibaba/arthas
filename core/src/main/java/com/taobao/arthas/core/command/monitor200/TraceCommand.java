package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.matcher.GroupMatcher;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.TrueMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.util.ArrayList;
import java.util.List;

/**
 * 调用跟踪命令<br/>
 * 负责输出一个类中的所有方法调用路径，用于追踪方法调用的完整链路
 *
 * @author vlinux on 15/5/27.
 */
// @formatter:off
@Name("trace")
@Summary("Trace the execution time of specified method invocation.")
@Description(value = Constants.EXPRESS_DESCRIPTION + Constants.EXAMPLE +
        "  trace org.apache.commons.lang.StringUtils isBlank\n" +
        "  trace *StringUtils isBlank\n" +
        "  trace *StringUtils isBlank params[0].length==1\n" +
        "  trace *StringUtils isBlank '#cost>100'\n" +
        "  trace -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils isBlank\n" +
        "  trace -E com.test.ClassA|org.test.ClassB method1|method2|method3\n" +
        "  trace demo.MathGame run -n 5\n" +
        "  trace demo.MathGame run --skipJDKMethod false\n" +
        "  trace javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter\n" +
        "  trace OuterClass$InnerClass *\n" +
        Constants.WIKI + Constants.WIKI_HOME + "trace")
//@formatter:on
public class TraceCommand extends EnhancerCommand {

    /**
     * 类名匹配模式
     */
    private String classPattern;

    /**
     * 方法名匹配模式
     */
    private String methodPattern;

    /**
     * 条件表达式
     */
    private String conditionExpress;

    /**
     * 是否使用正则表达式匹配
     */
    private boolean isRegEx = false;

    /**
     * 执行次数限制阈值
     */
    private int numberOfLimit = 100;

    /**
     * 路径追踪模式列表
     */
    private List<String> pathPatterns;

    /**
     * 是否跳过JDK方法的追踪
     */
    private boolean skipJDKTrace;

    /**
     * 设置类名匹配模式
     * 支持使用'.'或'/'作为分隔符
     *
     * @param classPattern 类名匹配模式
     */
    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = StringUtils.normalizeClassName(classPattern);
    }

    /**
     * 设置方法名匹配模式
     *
     * @param methodPattern 方法名匹配模式
     */
    @Argument(argName = "method-pattern", index = 1)
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    /**
     * 设置条件表达式
     * 用于过滤满足特定条件的方法调用
     *
     * @param conditionExpress 条件表达式
     */
    @Argument(argName = "condition-express", index = 2, required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    /**
     * 设置是否使用正则表达式匹配
     * 默认使用通配符匹配
     *
     * @param regEx 是否启用正则表达式
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置执行次数限制阈值
     * 达到此阈值后停止追踪
     *
     * @param numberOfLimit 执行次数限制
     */
    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    /**
     * 设置路径追踪模式列表
     * 用于指定需要追踪的调用路径
     *
     * @param pathPatterns 路径模式列表
     */
    @Option(shortName = "p", longName = "path", acceptMultipleValues = true)
    @Description("path tracing pattern")
    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    /**
     * 设置是否跳过JDK方法的追踪
     * 默认值为true，即跳过JDK内部方法的追踪
     *
     * @param skipJDKTrace 是否跳过JDK方法追踪
     */
    @Option(longName = "skipJDKMethod")
    @DefaultValue("true")
    @Description("skip jdk method trace, default value true.")
    public void setSkipJDKTrace(boolean skipJDKTrace) {
        this.skipJDKTrace = skipJDKTrace;
    }

    /**
     * 设置指定类的类加载器哈希码
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
     * @return 类名匹配模式
     */
    public String getClassPattern() {
        return classPattern;
    }

    /**
     * 获取方法名匹配模式
     *
     * @return 方法名匹配模式
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
     * 判断是否跳过JDK方法追踪
     *
     * @return 是否跳过JDK方法追踪
     */
    public boolean isSkipJDKTrace() {
        return skipJDKTrace;
    }

    /**
     * 判断是否使用正则表达式
     *
     * @return 是否使用正则表达式
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    /**
     * 获取执行次数限制阈值
     *
     * @return 执行次数限制阈值
     */
    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    /**
     * 获取路径追踪模式列表
     *
     * @return 路径追踪模式列表
     */
    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    /**
     * 获取类名匹配器
     * 根据是否指定了路径追踪模式，返回不同的匹配器
     *
     * @return 类名匹配器
     */
    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            // 如果没有指定路径追踪模式，使用普通的类名匹配器
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
            } else {
                // 否则使用路径追踪的类名匹配器
                classNameMatcher = getPathTracingClassMatcher();
            }
        }
        return classNameMatcher;
    }

    /**
     * 获取类名排除匹配器
     * 用于排除不需要追踪的类
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
     * 根据是否指定了路径追踪模式，返回不同的匹配器
     *
     * @return 方法名匹配器
     */
    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            // 如果没有指定路径追踪模式，使用普通的方法名匹配器
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
            } else {
                // 否则使用路径追踪的方法名匹配器（匹配所有方法）
                methodNameMatcher = getPathTracingMethodMatcher();
            }
        }
        return methodNameMatcher;
    }

    /**
     * 获取增强监听器
     * 根据是否指定了路径追踪模式，返回不同的监听器实现
     *
     * @param process 命令处理进程
     * @return 增强监听器
     */
    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        // 如果没有指定路径追踪模式，使用普通的Trace监听器
        if (pathPatterns == null || pathPatterns.isEmpty()) {
            return new TraceAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
        } else {
            // 否则使用路径追踪的监听器
            return new PathTraceAdviceListener(this, process);
        }
    }

    /**
     * 构造路径追踪的类名匹配器
     * 将主类名模式和所有路径模式组合成一个OR匹配器
     *
     * @return 类名匹配器
     */
    private Matcher<String> getPathTracingClassMatcher() {

        List<Matcher<String>> matcherList = new ArrayList<Matcher<String>>();
        // 添加主类名模式的匹配器
        matcherList.add(SearchUtils.classNameMatcher(getClassPattern(), isRegEx()));

        // 如果指定了路径模式，为每个路径创建对应的匹配器
        if (null != getPathPatterns()) {
            for (String pathPattern : getPathPatterns()) {
                // 根据是否启用正则表达式，创建不同类型的匹配器
                if (isRegEx()) {
                    matcherList.add(new RegexMatcher(pathPattern));
                } else {
                    matcherList.add(new WildcardMatcher(pathPattern));
                }
            }
        }

        // 使用OR逻辑组合所有匹配器，满足任一条件即匹配成功
        return new GroupMatcher.Or<String>(matcherList);
    }

    /**
     * 构造路径追踪的方法名匹配器
     * 在路径追踪模式下，匹配所有方法
     *
     * @return 方法名匹配器（总是返回true）
     */
    private Matcher<String> getPathTracingMethodMatcher() {
        return new TrueMatcher<String>();
    }
}
