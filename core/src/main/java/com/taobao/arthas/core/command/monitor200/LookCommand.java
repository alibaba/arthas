package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.look.LookUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.*;

import java.arthas.SpyAPI;
import java.lang.reflect.Method;
import java.util.Arrays;

@Name("look")
@Summary("Display the local variables, input parameter of method specified with LineNumber or LocationCode(found in jad command)")
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
                "  look com.arthas.controller.Controller doSomething -1 \n" +
                "  look com.arthas.controller.Controller doSomething -1 'varMap'\n" +
                "  look com.arthas.controller.Controller doSomething 35 'varMap'  'varMap[\"varName\"].equals(\"123\")'\n" +
                "  look OuterClass$InnerClass doSomething 128 '{params,varMap}'  \n" +
                "  look com.arthas.controller.Controller doSomething abcd-1 'varMap'\n" +
                Constants.WIKI + Constants.WIKI_HOME + "look")

public class LookCommand extends EnhancerCommand {

    private String classPattern;
    private String methodPattern;
    private String express;
    private String location;
    private String conditionExpress;
    private Integer expand = 1;
    private Integer sizeLimit = 10 * 1024 * 1024;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;

    @Argument(index = 0, argName = "class-pattern")
    @Description("The full qualified class name you want to look")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("The method name you want to look in")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "location")
    @Description("The location will be look before(LineNumber) or after(LocationCode).")
    public void setLocation(String location) {
        this.location = location;
    }

    @Argument(index = 3, argName = "express", required = false)
    @DefaultValue("varMap")
    @Description("The content you want to look, written by ognl. Default value is 'varMap'\n")
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

    public String getLocation() {
        return location;
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
        return new LookAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    /**
     * 命令补齐
     */
    @Override
    protected void completeArgument3(Completion completion) {
        CompletionUtils.complete(completion, Arrays.asList(EnhancerCommand.LOOK_EXPRESS_EXAMPLES));
    }

    @Override
    public void process(final CommandProcess process) {
        //兼容旧版，如果没有那些方法，就不要用这个命令
        boolean hasLookMethod = false;
        try {
            Class clazz = Class.forName("java.arthas.SpyAPI"); // 加载不到会抛异常
            for (Method declaredMethod : clazz.getDeclaredMethods()) {
                if (declaredMethod.getName().equals("atLookLocationCode")){
                    hasLookMethod = true;
                    break;
                }
            }
        } catch (Throwable e) {
            // ignore
        }
        if (!hasLookMethod){
            throw new IllegalArgumentException("this version not support look command!");
        }
        //check arg,只是简单的格式校验
        if (!LookUtils.validLocation(location)) {
            throw new IllegalArgumentException("location is invalid! " + location);
        }
        super.process(process);
    }
}
