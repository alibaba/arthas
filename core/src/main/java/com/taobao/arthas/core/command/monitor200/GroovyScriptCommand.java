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
 * Groovy support has been completed dropped in Arthas 3.0 because of severer memory leak.
 * 脚本增强命令
 *
 * @author vlinux on 15/5/31.
 */
@Name("groovy")
@Hidden
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
    private String classPattern;
    private String methodPattern;
    private String scriptFilepath;
    private boolean isRegEx = false;

    @Argument(index = 0, argName = "class-pattern")
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern")
    @Description("Method of Pattern Matching")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "script-filepath")
    @Description("Filepath of Groovy script")
    public void setScriptFilepath(String scriptFilepath) {
        this.scriptFilepath = scriptFilepath;
    }

    @Option(shortName = "E", longName = "regex")
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getScriptFilepath() {
        return scriptFilepath;
    }

    public boolean isRegEx() {
        return isRegEx;
    }

    @Override
    protected Matcher getClassNameMatcher() {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        throw new UnsupportedOperationException("groovy command is not supported yet!");
    }
}
