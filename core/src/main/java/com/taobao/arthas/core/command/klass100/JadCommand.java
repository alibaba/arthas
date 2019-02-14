package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.Decompiler;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.lang.LangRenderUtil;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.regex.Pattern;

import static com.taobao.text.ui.Element.label;

/**
 * @author diecui1202 on 15/11/24.
 * @author hengyunabc 2018-11-16
 */
@Name("jad")
@Summary("Decompile class")
@Description(Constants.EXAMPLE +
        "  jad java.lang.String\n" +
        "  jad java.lang.String toString\n" +
        "  jad --source-only java.lang.String\n" +
        "  jad -c 39eb305e org/apache/log4j/Logger\n" +
        "  jad -c 39eb305e -E org\\\\.apache\\\\.*\\\\.StringUtils\n" +
        Constants.WIKI + Constants.WIKI_HOME + "jad")
public class JadCommand extends AnnotatedCommand {
    private static final Logger logger = LogUtil.getArthasLogger();
    private static Pattern pattern = Pattern.compile("(?m)^/\\*\\s*\\*/\\s*$" + System.getProperty("line.separator"));

    private String classPattern;
    private String methodName;
    private String code = null;
    private boolean isRegEx = false;

    /**
     * jad output source code only
     */
    private boolean sourceOnly = false;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-name", index = 1, required = false)
    @Description("method name pattern, decompile a specific method instead of the whole class")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }


    @Option(shortName = "c", longName = "code")
    @Description("The hash code of the special class's classLoader")
    public void setCode(String code) {
        this.code = code;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(longName = "source-only", flag = true)
    @Description("Output source code only")
    public void setSourceOnly(boolean sourceOnly) {
        this.sourceOnly = sourceOnly;
    }

    @Override
    public void process(CommandProcess process) {
        RowAffect affect = new RowAffect();
        Instrumentation inst = process.session().getInstrumentation();
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, code);

        try {
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                processNoMatch(process);
            } else if (matchedClasses.size() > 1) {
                processMatches(process, matchedClasses);
            } else { // matchedClasses size is 1
                // find inner classes.
                Set<Class<?>> withInnerClasses = SearchUtils.searchClassOnly(inst,  matchedClasses.iterator().next().getName() + "$*", false, code);
                if(withInnerClasses.isEmpty()) {
                    withInnerClasses = matchedClasses;
                }
                processExactMatch(process, affect, inst, matchedClasses, withInnerClasses);
            }
        } finally {
            if (!this.sourceOnly) {
                process.write(affect + "\n");
            }
            process.end();
        }
    }

    private void processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst, Set<Class<?>> matchedClasses, Set<Class<?>> withInnerClasses) {
        Class<?> c = matchedClasses.iterator().next();
        Set<Class<?>> allClasses = new HashSet<Class<?>>(withInnerClasses);
        allClasses.add(c);

        try {
            ClassDumpTransformer transformer = new ClassDumpTransformer(allClasses);
            Enhancer.enhance(inst, transformer, allClasses);
            Map<Class<?>, File> classFiles = transformer.getDumpResult();
            File classFile = classFiles.get(c);

            String source = Decompiler.decompile(classFile.getAbsolutePath(), methodName);
            if (source != null) {
                source = pattern.matcher(source).replaceAll("");
            } else {
                source = "unknown";
            }

            if (this.sourceOnly) {
                process.write(LangRenderUtil.render(source) + "\n");
                return;
            }


            process.write("\n");
            process.write(RenderUtil.render(new LabelElement("ClassLoader: ").style(Decoration.bold.fg(Color.red)), process.width()));
            process.write(RenderUtil.render(TypeRenderUtils.drawClassLoader(c), process.width()) + "\n");
            process.write(RenderUtil.render(new LabelElement("Location: ").style(Decoration.bold.fg(Color.red)), process.width()));
            process.write(RenderUtil.render(new LabelElement(ClassUtils.getCodeSource(
                    c.getProtectionDomain().getCodeSource())).style(Decoration.bold.fg(Color.blue)), process.width()) + "\n");
            process.write(LangRenderUtil.render(source) + "\n");
            process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
            affect.rCnt(classFiles.keySet().size());
        } catch (Throwable t) {
            logger.error(null, "jad: fail to decompile class: " + c.getName(), t);
        }
    }

    private void processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {
        Element usage = new LabelElement("jad -c <hashcode> " + classPattern).style(Decoration.bold.fg(Color.blue));
        process.write("\n Found more than one class for: " + classPattern + ", Please use "
                + RenderUtil.render(usage, process.width()));

        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                new LabelElement("CLASSLOADER").style(Decoration.bold.bold()));

        for (Class<?> c : matchedClasses) {
            ClassLoader classLoader = c.getClassLoader();
            table.row(label(Integer.toHexString(classLoader.hashCode())).style(Decoration.bold.fg(Color.red)),
                    TypeRenderUtils.drawClassLoader(c));
        }

        process.write(RenderUtil.render(table, process.width()) + "\n");
    }

    private void processNoMatch(CommandProcess process) {
        process.write("No class found for: " + classPattern + "\n");
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeClassName(completion)) {
            super.complete(completion);
        }
    }
}
