package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.DumpClassModel;
import com.taobao.arthas.core.command.model.DumpClassVO;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.CommandUtils;
import com.taobao.arthas.core.util.InstrumentationUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;


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
    private String classLoaderClass;
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

    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
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
    @DefaultValue("50")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public void process(CommandProcess process) {
        RowAffect effect = new RowAffect();
        try {
            if (directory != null) {
                File dir = new File(directory);
                if (dir.isFile()) {
                    process.end(-1, directory + " :is not a directory, please check it");
                    return;
                }
            }

            ExitStatus status = null;

            Instrumentation inst = process.session().getInstrumentation();
            if (code == null && classLoaderClass != null) {
                List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
                if (matchedClassLoaders.size() == 1) {
                    code = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                } else if (matchedClassLoaders.size() > 1) {
                    Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                    DumpClassModel dumpClassModel = new DumpClassModel()
                            .setClassLoaderClass(classLoaderClass)
                            .setMatchedClassLoaders(classLoaderVOList);
                    process.appendResult(dumpClassModel);
                    process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                    return;
                } else {
                    process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                    return;
                }
            }

            Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, code);
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                status = processNoMatch(process);
            } else if (matchedClasses.size() > limit) {
                status = processMatches(process, matchedClasses);
            } else {
                status = processMatch(process, effect, inst, matchedClasses);
            }
            process.appendResult(new RowAffectModel(effect));
            CommandUtils.end(process, status);
        } catch (Throwable e){
            logger.error("processing error", e);
            process.end(-1, "processing error");
        }
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeClassName(completion)) {
            super.complete(completion);
        }
    }

    private ExitStatus processMatch(CommandProcess process, RowAffect effect, Instrumentation inst, Set<Class<?>> matchedClasses) {
        try {
            Map<Class<?>, File> classFiles = dump(inst, matchedClasses);
            List<DumpClassVO> dumpedClasses = new ArrayList<DumpClassVO>(classFiles.size());
            for (Map.Entry<Class<?>, File> entry : classFiles.entrySet()) {
                Class<?> clazz = entry.getKey();
                File file = entry.getValue();
                DumpClassVO dumpClassVO = new DumpClassVO();
                dumpClassVO.setLocation(file.getCanonicalPath());
                ClassUtils.fillSimpleClassVO(clazz, dumpClassVO);
                dumpedClasses.add(dumpClassVO);
            }
            process.appendResult(new DumpClassModel().setDumpedClasses(dumpedClasses));

            effect.rCnt(classFiles.keySet().size());
            return ExitStatus.success();
        } catch (Throwable t) {
            logger.error("dump: fail to dump classes: " + matchedClasses, t);
            return ExitStatus.failure(-1, "dump: fail to dump classes: " + matchedClasses);
        }
    }

    private ExitStatus processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {
        String msg = String.format(
                "Found more than %d class for: %s, Please Try to specify the classloader with the -c option, or try to use --limit option.",
                limit, classPattern);
        process.appendResult(new MessageModel(msg));

        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        process.appendResult(new DumpClassModel().setMatchedClasses(classVOs));
        return ExitStatus.failure(-1, msg);
    }

    private ExitStatus processNoMatch(CommandProcess process) {
        return ExitStatus.failure(-1, "No class found for: " + classPattern);
    }

    private Map<Class<?>, File> dump(Instrumentation inst, Set<Class<?>> classes) throws UnmodifiableClassException {
        ClassDumpTransformer transformer = null;
        if (directory != null) {
            transformer = new ClassDumpTransformer(classes, new File(directory));
        } else {
            transformer = new ClassDumpTransformer(classes);
        }
        InstrumentationUtils.retransformClasses(inst, transformer, classes);
        return transformer.getDumpResult();
    }
}
