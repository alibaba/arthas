package com.taobao.arthas.core.command.monitor200;

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
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.util.ArrayList;
import java.util.List;

// TODO: trace 可以看做是 watch 的超集, 所以或许可以合并 TraceCommand 链路与 WatchCommand 链路的重复代码.
/**
 * 调用跟踪命令<br/>
 * 负责输出一个类中的所有方法调用路径
 *
 * @author vlinux on 15/5/27.
 */
@Name("trace")
@Summary("Trace the execution time of specified method invocation.")
@Description(value = Constants.EXPRESS_DESCRIPTION + Constants.EXAMPLE +
        "  trace org.apache.commons.lang.StringUtils isBlank\n" +
        "  trace *StringUtils isBlank\n" +
        "  trace *StringUtils isBlank params[0].length==1\n" +
        "  trace *StringUtils isBlank '#cost>100'\n" +
        "  trace -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils isBlank\n" +
        "  trace -E com.test.ClassA|org.test.ClassB method1|method2|method3\n" +
        Constants.WIKI + Constants.WIKI_HOME + "trace")
public class TraceCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String conditionExpress;
    private String express;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isBefore = false;
    private boolean isFinish = false;
    private boolean isException = false;
    private boolean isSuccess = false;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;
    private List<String> pathPatterns;
    private boolean skipJDKTrace;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-pattern", index = 1)
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(argName = "condition-express", index = 2, required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Argument(argName = "express", index=3, required = false)
    @Description("the content you want to watch, written by ognl.\n" + Constants.EXPRESS_EXAMPLES)
    public void setExpress(String express) {
        this.express = express;
    }

    @Option(shortName = "b", longName = "before", flag = true)
    @Description("Watch before invocation")
    public void setBefore(boolean before) {
        isBefore = before;
    }

    @Option(shortName = "f", longName = "finish", flag = true)
    @Description("Watch after invocation, enable by default")
    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    @Option(shortName = "e", longName = "exception", flag = true)
    @Description("Watch after throw exception")
    public void setException(boolean exception) {
        isException = exception;
    }

    @Option(shortName = "s", longName = "success", flag = true)
    @Description("Watch after successful invocation")
    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Option(shortName = "p", longName = "path", acceptMultipleValues = true)
    @Description("path tracing pattern")
    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    @Option(shortName = "j", longName = "jdkMethodSkip")
    @Description("skip jdk method trace")
    public void setSkipJDKTrace(boolean skipJDKTrace) {
        this.skipJDKTrace = skipJDKTrace;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (10 * 1024 * 1024 by default)")
    public void setSizeLimit(Integer sizeLimit) {
    this.sizeLimit = sizeLimit;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public Integer getExpand() {
        return expand;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public boolean enableWatch () {
        return !StringUtils.isEmpty(express);
    }

    public String getExpress() {
        return express;
    }

    public boolean isSkipJDKTrace() {
        return skipJDKTrace;
    }

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public boolean isException() {
        return isException;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
            } else {
                classNameMatcher = getPathTracingClassMatcher();
            }
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            if (pathPatterns == null || pathPatterns.isEmpty()) {
                methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
            } else {
                methodNameMatcher = getPathTracingMethodMatcher();
            }
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        if (pathPatterns == null || pathPatterns.isEmpty()) {
            return new TraceAdviceListener(this, process);
        } else {
            return new PathTraceAdviceListener(this, process);
        }
    }

    /**
     * 构造追踪路径匹配
     */
    private Matcher<String> getPathTracingClassMatcher() {

        List<Matcher<String>> matcherList = new ArrayList<Matcher<String>>();
        matcherList.add(SearchUtils.classNameMatcher(getClassPattern(), isRegEx()));

        if (null != getPathPatterns()) {
            for (String pathPattern : getPathPatterns()) {
                if (isRegEx()) {
                    matcherList.add(new RegexMatcher(pathPattern));
                } else {
                    matcherList.add(new WildcardMatcher(pathPattern));
                }
            }
        }

        return new GroupMatcher.Or<String>(matcherList);
    }

    private Matcher<String> getPathTracingMethodMatcher() {
        return new TrueMatcher<String>();
    }
}
