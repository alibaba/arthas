package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.JadModel;
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
import com.taobao.arthas.core.util.Decompiler;
import com.taobao.arthas.core.util.InstrumentationUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.regex.Pattern;

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
    private static final Logger logger = LoggerFactory.getLogger(JadCommand.class);
    private static Pattern pattern = Pattern.compile("(?m)^/\\*\\s*\\*/\\s*$" + System.getProperty("line.separator"));

    private String classPattern;
    private String methodName;
    private String code = null;
    private String classLoaderClass;
    private boolean isRegEx = false;
    private boolean hideUnicode = false;

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

    @Option(longName = "hideUnicode", flag = true)
    @Description("hide unicode, default value false")
    public void setHideUnicode(boolean hideUnicode) {
        this.hideUnicode = hideUnicode;
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

        if (code == null && classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                code = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                JadModel jadModel = new JadModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(jadModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }
        
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, code);

        try {
            ExitStatus status = null;
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                status = processNoMatch(process);
            } else if (matchedClasses.size() > 1) {
                status = processMatches(process, matchedClasses);
            } else { // matchedClasses size is 1
                // find inner classes.
                Set<Class<?>> withInnerClasses = SearchUtils.searchClassOnly(inst,  matchedClasses.iterator().next().getName() + "$*", false, code);
                if(withInnerClasses.isEmpty()) {
                    withInnerClasses = matchedClasses;
                }
                status = processExactMatch(process, affect, inst, matchedClasses, withInnerClasses);
            }
            if (!this.sourceOnly) {
                process.appendResult(new RowAffectModel(affect));
            }
            CommandUtils.end(process, status);
        } catch (Throwable e){
            logger.error("processing error", e);
            process.end(-1, "processing error");
        }
    }

    private ExitStatus processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst, Set<Class<?>> matchedClasses, Set<Class<?>> withInnerClasses) {
        Class<?> c = matchedClasses.iterator().next();
        Set<Class<?>> allClasses = new HashSet<Class<?>>(withInnerClasses);
        allClasses.add(c);

        try {
            ClassDumpTransformer transformer = new ClassDumpTransformer(allClasses);
            InstrumentationUtils.retransformClasses(inst, transformer, allClasses);

            Map<Class<?>, File> classFiles = transformer.getDumpResult();
            File classFile = classFiles.get(c);

            String source = Decompiler.decompile(classFile.getAbsolutePath(), methodName, hideUnicode);
            if (source != null) {
                source = pattern.matcher(source).replaceAll("");
            } else {
                source = "unknown";
            }

            JadModel jadModel = new JadModel();
            jadModel.setSource(source);
            if (!this.sourceOnly) {
                jadModel.setClassInfo(ClassUtils.createSimpleClassInfo(c));
                jadModel.setLocation(ClassUtils.getCodeSource(c.getProtectionDomain().getCodeSource()));
            }
            process.appendResult(jadModel);

            affect.rCnt(classFiles.keySet().size());
            return ExitStatus.success();
        } catch (Throwable t) {
            logger.error("jad: fail to decompile class: " + c.getName(), t);
            return ExitStatus.failure(-1, "jad: fail to decompile class: " + c.getName());
        }
    }

    private ExitStatus processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {

        String usage = "jad -c <hashcode> " + classPattern;
        String msg = " Found more than one class for: " + classPattern + ", Please use " + usage;
        process.appendResult(new MessageModel(msg));

        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        JadModel jadModel = new JadModel();
        jadModel.setMatchedClasses(classVOs);
        process.appendResult(jadModel);

        return ExitStatus.failure(-1, msg);
    }

    private ExitStatus processNoMatch(CommandProcess process) {
        return ExitStatus.failure(-1, "No class found for: " + classPattern);
    }

    @Override
    public void complete(Completion completion) {
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);

        if (argumentIndex == 1) {
            if (!CompletionUtils.completeClassName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 2) {
            if (!CompletionUtils.completeMethodName(completion)) {
                super.complete(completion);
            }
            return;
        }

        super.complete(completion);
    }
}
