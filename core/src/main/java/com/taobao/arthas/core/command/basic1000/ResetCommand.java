package com.taobao.arthas.core.command.basic1000;

import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ResetModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * 恢复所有增强类<br/>
 *
 * @author vlinux on 15/5/29.
 */
@Name("reset")
@Summary("Reset all the enhanced classes")
@Description(Constants.EXAMPLE +
        "  reset\n" +
        "  reset *List\n" +
        "  reset -E .*List\n")
public class ResetCommand extends AnnotatedCommand {
    private String classPattern;
    private boolean isRegEx = false;

    @Argument(index = 0, argName = "class-pattern", required = false)
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Override
    public void process(CommandProcess process) {
        Instrumentation inst = process.session().getInstrumentation();
        Matcher matcher = SearchUtils.classNameMatcher(classPattern, isRegEx);
        try {
            EnhancerAffect enhancerAffect = Enhancer.reset(inst, matcher);
            process.appendResult(new ResetModel(enhancerAffect));
            process.end();
        } catch (UnmodifiableClassException e) {
            // ignore
            process.end();
        }
    }
}
