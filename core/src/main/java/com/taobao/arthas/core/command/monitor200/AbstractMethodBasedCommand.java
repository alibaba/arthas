package com.taobao.arthas.core.command.monitor200;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;

/**
 * 基于方法命令抽像类.
 */
public abstract class AbstractMethodBasedCommand extends EnhancerCommand {


    protected String classPattern;
    protected String methodPattern;
  
    @Argument(argName = "class-pattern", index = 0)
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(final String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-pattern", index = 1)
    @Description("Method of Pattern Matching, ~ means take from class-pattern com.a.C:method or com.a.C.method")
    public void setMethodPattern(final String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Override
    protected void enhance(final CommandProcess process) {
        if ("~".equals(methodPattern)) { //smart way
          final Pattern smartPattern = Pattern.compile("([.][A-Z][^.]*)([.:])([a-z][^. ]*)$");
          final Matcher matcher = smartPattern.matcher(classPattern);
          if (matcher.find()) {
              methodPattern = matcher.group(3);
              classPattern = matcher.replaceAll("$1");
              process.echoTips("smart way input ==> " + classPattern + " " + methodPattern + "\n");
          }
        }
        super.enhance(process);
    }
}
