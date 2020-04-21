package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;

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
    private static final Logger logger = LoggerFactory.getLogger(DumpClassCommand.class);

    private String classPattern;
    private String code = null;
    private boolean isRegEx = false;

    private String directory;

    private int limit;

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

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "d", longName = "directory")
    @Description("Sets the destination directory for class files")
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Option(shortName = "l", longName = "limit")
    @Description("The limit of dump classes size, default value is 5")
    @DefaultValue("5")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void process(CommandProcess process) {
        RowAffect effect = new RowAffect();
        StatusModel statusModel = new StatusModel(-1, "unknown error");
        try {
            if (directory != null) {
                File dir = new File(directory);
                if (dir.isFile()) {
                    process.end(-1, directory + " :is not a directory, please check it");
                    return;
                }
            }
            Instrumentation inst = process.session().getInstrumentation();
            Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, code);
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                processNoMatch(process, statusModel);
            } else if (matchedClasses.size() > limit) {
                processMatches(process, matchedClasses, statusModel);
            } else {
                processMatch(process, effect, inst, matchedClasses, statusModel);
            }
        } finally {
            process.appendResult(new RowAffectModel(effect));
            process.end(statusModel.getStatusCode(), statusModel.getMessage());
        }
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeClassName(completion)) {
            super.complete(completion);
        }
    }

    private void processMatch(CommandProcess process, RowAffect effect, Instrumentation inst, Set<Class<?>> matchedClasses, StatusModel statusModel) {
        try {
            Map<Class<?>, File> classFiles = dump(inst, matchedClasses);
            List<ClassVO> dumpedClasses = new ArrayList<ClassVO>(classFiles.size());
            for (Map.Entry<Class<?>, File> entry : classFiles.entrySet()) {
                Class<?> clazz = entry.getKey();
                File file = entry.getValue();
                ClassVO classVO = ClassUtils.createSimpleClassInfo(clazz);
                classVO.setLocation(file.getCanonicalPath());
                dumpedClasses.add(classVO);
            }
            process.appendResult(new DumpClassModel(dumpedClasses));

            effect.rCnt(classFiles.keySet().size());
            statusModel.setStatus(0);
        } catch (Throwable t) {
            logger.error("dump: fail to dump classes: " + matchedClasses, t);
        }
    }

    private void processMatches(CommandProcess process, Set<Class<?>> matchedClasses, StatusModel statusModel) {
        String msg = String.format(
                "Found more than %d class for: %s, Please Try to specify the classloader with the -c option, or try to use --limit option.",
                limit, classPattern);
        process.appendResult(new MessageModel(msg));

        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        process.appendResult(new ClassMatchesModel(classVOs));
        statusModel.setStatus(-1, msg);
    }

    private void processNoMatch(CommandProcess process, StatusModel statusModel) {
        statusModel.setStatus(-1, "No class found for: " + classPattern);
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
