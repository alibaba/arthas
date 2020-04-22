package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.Decompiler;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.lang.LangRenderUtil;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.util.RenderUtil;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        StatusModel statusModel = new StatusModel(-1, "unknown error");
        try {
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                statusModel = processNoMatch(process);
            } else if (matchedClasses.size() > 1) {
                statusModel = processMatches(process, matchedClasses);
            } else { // matchedClasses size is 1
                // find inner classes.
                Set<Class<?>> withInnerClasses = SearchUtils.searchClassOnly(inst,  matchedClasses.iterator().next().getName() + "$*", false, code);
                if(withInnerClasses.isEmpty()) {
                    withInnerClasses = matchedClasses;
                }
                statusModel = processExactMatch(process, affect, inst, matchedClasses, withInnerClasses);
            }
        } finally {
            if (!this.sourceOnly) {
                process.appendResult(new RowAffectModel(affect));
            }
            process.end(statusModel.getStatusCode(), statusModel.getMessage());
        }
    }


    public static void retransformClasses(Instrumentation inst, ClassFileTransformer transformer, Set<Class<?>> classes) {
        try {
            inst.addTransformer(transformer, true);

            for(Class<?> clazz : classes) {
                try{
                    inst.retransformClasses(clazz);
                }catch(Throwable e) {
                    String errorMsg = "retransformClasses class error, name: " + clazz.getName();
                    if(ClassUtils.isLambdaClass(clazz) && e instanceof VerifyError) {
                        errorMsg += ", Please ignore lambda class VerifyError: https://github.com/alibaba/arthas/issues/675";
                    }
                    logger.error(errorMsg, e);
                }
            }
        } finally {
            inst.removeTransformer(transformer);
        }
    }

    private StatusModel processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst, Set<Class<?>> matchedClasses, Set<Class<?>> withInnerClasses) {
        StatusModel statusModel = new StatusModel();
        Class<?> c = matchedClasses.iterator().next();
        Set<Class<?>> allClasses = new HashSet<Class<?>>(withInnerClasses);
        allClasses.add(c);

        try {
            ClassDumpTransformer transformer = new ClassDumpTransformer(allClasses);
            retransformClasses(inst, transformer, allClasses);

            Map<Class<?>, File> classFiles = transformer.getDumpResult();
            File classFile = classFiles.get(c);

            String source = Decompiler.decompile(classFile.getAbsolutePath(), methodName);
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
            statusModel.setStatus(0);
        } catch (Throwable t) {
            logger.error("jad: fail to decompile class: " + c.getName(), t);
            statusModel.setStatus(-1, "jad: fail to decompile class: " + c.getName());
        }
        return statusModel;
    }

    private StatusModel processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {
        //Element usage = new LabelElement("jad -c <hashcode> " + classPattern).style(Decoration.bold.fg(Color.blue));
        //process.write("\n Found more than one class for: " + classPattern + ", Please use "
        //        + RenderUtil.render(usage, process.width()));

        String usage = "jad -c <hashcode> " + classPattern;
        String msg = " Found more than one class for: " + classPattern + ", Please use " + usage;
        process.appendResult(new MessageModel(msg));

        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        process.appendResult(new ClassMatchesModel(classVOs));

        return new StatusModel(-1, msg);
    }

    private StatusModel processNoMatch(CommandProcess process) {
        return new StatusModel().setStatus(-1, "No class found for: " + classPattern);
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
