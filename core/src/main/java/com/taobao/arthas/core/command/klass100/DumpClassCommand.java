package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
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
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Map;
import java.util.Set;

import static com.taobao.text.ui.Element.label;

/**
 * Dump class byte array
 */
@Name("dump")
@Summary("Dump class byte array from JVM")
@Description(Constants.EXAMPLE +
        "  dump java.lang.String\n" +
        "  dump -d /tmp/output java.lang.String\n" +
        "  dump org/apache/commons/lang/StringUtils\n" +
        "  dump *StringUtils\n" +
        "  dump -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils\n" +
        Constants.WIKI + Constants.WIKI_HOME + "dump")
public class DumpClassCommand extends AnnotatedCommand {
    private static final Logger logger = LogUtil.getArthasLogger();

    private String classPattern;
    private String code = null;
    private boolean isRegEx = false;

    private String directory;

    @Argument(index = 0, argName = "class-pattern")
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Option(shortName = "c", longName = "code")
    @Description("The hash code of the special class's classLoader")
    public void setCode(String code) {
        this.code = code;
    }

    @Option(shortName = "E", longName = "regex")
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "d", longName = "directory")
    @Description("Sets the destination directory for class files")
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public void process(CommandProcess process) {
        RowAffect effect = new RowAffect();

        try {
            if (directory != null) {
                File dir = new File(directory);
                if (dir.isFile()) {
                    process.write(directory + " :is not a directory, please check it\n");
                    return;
                }
            }
            Instrumentation inst = process.session().getInstrumentation();
            Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, code);

            if (matchedClasses == null || matchedClasses.isEmpty()) {
                processNoMatch(process);
            } else if (matchedClasses.size() > 5) {
                processMatches(process, matchedClasses);
            } else {
                processMatch(process, effect, inst, matchedClasses);
            }
        } finally {
            process.write(effect + "\n");
            process.end();
        }
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeClassName(completion)) {
            super.complete(completion);
        }
    }

    private void processMatch(CommandProcess process, RowAffect effect, Instrumentation inst, Set<Class<?>> matchedClasses) {
        try {
            Map<Class<?>, File> classFiles = dump(inst, matchedClasses);
            TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
            table.row(new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                    new LabelElement("CLASSLOADER").style(Decoration.bold.bold()),
                    new LabelElement("LOCATION").style(Decoration.bold.bold()));

            for (Map.Entry<Class<?>, File> entry : classFiles.entrySet()) {
                Class<?> clazz = entry.getKey();
                File file = entry.getValue();
                table.row(label(StringUtils.classLoaderHash(clazz)).style(Decoration.bold.fg(Color.red)),
                        TypeRenderUtils.drawClassLoader(clazz),
                        label(file.getCanonicalPath()).style(Decoration.bold.fg(Color.red)));
            }

            process.write(RenderUtil.render(table, process.width()))
                    .write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
            effect.rCnt(classFiles.keySet().size());
        } catch (Throwable t) {
            logger.error(null, "dump: fail to dump classes: " + matchedClasses, t);
        }
    }

    private void processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {
        Element usage = new LabelElement("dump -c hashcode " + classPattern).style(Decoration.bold.fg(Color.blue));
        process.write("Found more than 5 class for: " + classPattern + ", Please use ");
        process.write(RenderUtil.render(usage, process.width()));

        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        table.row(new LabelElement("NAME").style(Decoration.bold.bold()),
                new LabelElement("HASHCODE").style(Decoration.bold.bold()),
                new LabelElement("CLASSLOADER").style(Decoration.bold.bold()));

        for (Class<?> c : matchedClasses) {
            table.row(label(c.getName()), label(StringUtils.classLoaderHash(c)).style(Decoration.bold.fg(Color.red)),
                    TypeRenderUtils.drawClassLoader(c));
        }

        process.write(RenderUtil.render(table, process.width()) + "\n");
    }

    private void processNoMatch(CommandProcess process) {
        process.write("No class found for: " + classPattern + "\n");
    }

    private Map<Class<?>, File> dump(Instrumentation inst, Set<Class<?>> classes) throws UnmodifiableClassException {
        ClassDumpTransformer transformer = null;
        if (directory != null) {
            transformer = new ClassDumpTransformer(classes, new File(directory));
        } else {
            transformer = new ClassDumpTransformer(classes);
        }
        Enhancer.enhance(inst, transformer, classes);
        return transformer.getDumpResult();
    }
}
