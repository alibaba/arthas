package com.taobao.arthas.core.command.monitor200.curl;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.monitor200.EnhancerCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * @author zhaoyuening
 */
@Name("getcurl")
@Summary("Capture Http request and convert to Curl command, support Spring web framework.")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  getcurl -b org.apache.commons.lang.StringUtils isBlank \n" +
        "  getcurl -f org.apache.commons.lang.StringUtils isBlank \n" +
        "  getcurl org.apache.commons.lang.StringUtils isBlank \n" +
        "  getcurl -bf *StringUtils isBlank \n" +
        "  getcurl *StringUtils isBlank \n" +
        "  getcurl *StringUtils isBlank params[0].length==1\n" +
        "  getcurl *StringUtils isBlank '#cost>100'\n" +
        "  getcurl -E -b org\\.apache\\.commons\\.lang\\.StringUtils isBlank \n" +
        "  getcurl javax.servlet.Filter * --exclude-class-pattern com.demo.TestFilter\n" +
        Constants.WIKI + Constants.WIKI_HOME + "getcurl")
public class GetCurlCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String conditionExpress;
    private boolean isRegEx = false;
    private int numberOfLimit;
    private boolean isBefore = false;
    private boolean isFinish = false;
    private boolean isException = false;
    private boolean isSuccess = false;

    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to getcurl")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to getcurl")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
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

    @Option(shortName = "b", longName = "before", flag = true)
    @Description("GetCurl before invocation")
    public void setBefore(boolean before) {
        isBefore = before;
    }

    @Option(shortName = "f", longName = "finish", flag = true)
    @Description("GetCurl after invocation, enable by default")
    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    @Option(shortName = "e", longName = "exception", flag = true)
    @Description("GetCurl after throw exception")
    public void setException(boolean exception) {
        isException = exception;
    }

    @Option(shortName = "s", longName = "success", flag = true)
    @Description("GetCurl after successful invocation")
    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(this.classPattern, this.isRegEx);
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), this.isRegEx);
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(this.methodPattern, this.isRegEx);
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new GetCurlAdviceListener(this, process);
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    public boolean isException() {
        return isException;
    }

    public boolean isBefore() {
        return isBefore;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
