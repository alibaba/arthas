package com.taobao.arthas.core.command.monitor200;

import java.util.Arrays;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

@Name("watch")
@Summary("Display the input/output parameter, return object, and thrown exception of specified method invocation")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  watch -b org.apache.commons.lang.StringUtils isBlank params\n" +
        "  watch -f org.apache.commons.lang.StringUtils isBlank returnObj\n" +
        "  watch org.apache.commons.lang.StringUtils isBlank '{params, target, returnObj}' -x 2\n" +
        "  watch -bf *StringUtils isBlank params\n" +
        "  watch *StringUtils isBlank params[0]\n" +
        "  watch *StringUtils isBlank params[0] params[0].length==1\n" +
        "  watch *StringUtils isBlank params '#cost>100'\n" +
        "  watch -E -b org\\.apache\\.commons\\.lang\\.StringUtils isBlank params[0]\n" +
        "  watch javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter\n" +
        Constants.WIKI + Constants.WIKI_HOME + "watch")
public class WatchCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String express;
    private String conditionExpress;
    private boolean isBefore = false;
    private boolean isFinish = false;
    private boolean isException = false;
    private boolean isSuccess = false;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;
    
    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to watch")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to watch")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "express", required = false)
    @DefaultValue("{params, target, returnObj}")
    @Description("the content you want to watch, written by ognl.\n" + Constants.EXPRESS_EXAMPLES)
    public void setExpress(String express) {
        this.express = express;
    }

    @Argument(index = 3, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
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

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (10 * 1024 * 1024 by default)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
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

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getExpress() {
        return express;
    }

    public String getConditionExpress() {
        return conditionExpress;
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

    public Integer getExpand() {
        return expand;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new WatchAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }
}
