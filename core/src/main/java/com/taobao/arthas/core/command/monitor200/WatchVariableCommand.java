package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.matcher.EqualsMatcher;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.*;

import java.util.Arrays;
import java.util.List;

@Name("wv")
@Summary("watch variable of specified method invocation")
@Description(Constants.EXPRESS_DESCRIPTION + "\nExamples:\n" +
        "  wv org.apache.commons.lang.StringUtils isBlank \n" +
        "  wv org.apache.commons.lang.StringUtils isBlank 'var1,var2' \n" +
        "  wv org.apache.commons.lang.StringUtils isBlank 'var1,var2' -l 190 \n" +
        Constants.WIKI + Constants.WIKI_HOME + "wv")
public class WatchVariableCommand extends EnhancerCommand  {

    private String classPattern;
    private String methodPattern;
    private List<String> variableList;
    private int line = Integer.MAX_VALUE;

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

    @Argument(index = 2, argName = "variable-list", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setVariableList(String variableListStr) {
        this.variableList = Arrays.asList(variableListStr.split(","));
    }

    @Option(shortName = "l", longName = "line")
    @Description("return on this line")
    public void setLine(int line) {
        this.line = line;
    }



    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = new EqualsMatcher<String>(classPattern);
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = new EqualsMatcher<String>(methodPattern);
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new WatchVariableListener(this,process);
    }

    /**
     * 是否观察某个变量， 默认观察所有，所以当集合为空时，返回true
     * @param variableName 变量名
     * @return
     */
    public boolean variableListContains(String variableName) {
        if (variableList == null){
            return true;
        }
        return variableList.contains(variableName);
    }


    /**
     * 立即结束，不执行当前动作
     * @param line 当前行号
     * @return 是否立即结束
     */
    public boolean shouldFinishBefore(int line) {
        return line > this.line;
    }

    /**
     *
     * 执行完当前动作结束
     * @param line 当前行号
     * @return
     */
    public boolean shouldFinishAfter(int line) {

        return line >= this.line;
    }


}
