package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.Ansi;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.*;


@Name("line")
@Summary("Display the local variables, input parameter of method specified with LineNumber or LineCode(found in jad --lineCode)")
@Description(
        "  The express may be one of the following expression:\n" +
                "               target : the object\n" +
                "                clazz : the object's class\n" +
                "               method : the constructor or method\n" +
                "               params : the parameters array of method\n" +
                "         params[0..n] : the element of parameters array\n" +
                "               varMap : the local variables map\n" +
                "  varMap[\"varName\"] : the local variable value of varName\n" +
                "\nExamples:\n" +
                "  line org.apache.commons.lang.StringUtils isBlank -1 \n" +
                "  line org.apache.commons.lang.StringUtils isBlank -1 'varMap'\n" +
                "  line org.apache.commons.lang.StringUtils isBlank 3581 'varMap'  'varMap[\"strLen\"] == 3'\n" +
                "  line *StringUtils isBlank 128 '{params,varMap}'  \n" +
                "  line org.apache.commons.lang.StringUtils isBlank abcd-1 'varMap'\n" +
                Constants.WIKI + Constants.WIKI_HOME + "line"
)
public class LineCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String express;
    private String line;
    private String conditionExpress;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;

    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to watch in.")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to watch in.")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "location")
    @Description("The location will be watch before LineNumber(eg:108) or LineCode(eg:abcd-1, found in jad --lineCode).")
    public void setLine(String line) {
        this.line = line;
    }

    @Argument(index = 3, argName = "express", required = false)
    @DefaultValue("varMap")
    @Description("The express you want to evaluate, written by ognl. Default value is 'varMap'\n")
    public void setExpress(String express) {
        this.express = express;
    }

    @Argument(index = 4, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (10 * 1024 * 1024 by default)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default), the max value is " + ObjectView.MAX_DEEP)
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

    public String getLine() {
        return line;
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
        return new LineAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    @Override
    public void process(final CommandProcess process) {
        if (!LineHelper.hasSupportLineCommand()) {
            throw new IllegalArgumentException("this version not support line command!");
        }
        //check arg,只是简单的格式校验
        if (!LineHelper.validLocation(line)) {
            String helpCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("line -h").reset().toString();
            String jadCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("jad CLASS_NAME METHOD_NAME --lineCode").reset().toString();
            String msg = "Your location arg:" + line + " has a wrong format, it should be look like:\n"
                    + "1. LineNumber: `" + Ansi.ansi().fg(Ansi.Color.GREEN).a("108").reset().toString() + "` \n"
                    + "2. LineCode: `" + Ansi.ansi().fg(Ansi.Color.GREEN).a("abcd-1").reset().toString() + "` (use `"+jadCommand+"` to find out.)\n"
                    + "3. Use `" + helpCommand + "` to get more information.";
            process.end(-1, msg);
            return;
        }
        super.process(process);
    }
}
