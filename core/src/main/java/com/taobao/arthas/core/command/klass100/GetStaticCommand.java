package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.GetStaticModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.CommandUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.Collection;

/**
 * @author diecui1202 on 2017/9/27.
 */

@Name("getstatic")
@Summary("Show the static field of a class")
@Description(Constants.EXAMPLE +
             "  getstatic demo.MathGame random\n" +
             "  getstatic -c 39eb305e org.apache.log4j.LogManager DEFAULT_CONFIGURATION_FILE\n" +
             Constants.WIKI + Constants.WIKI_HOME + "getstatic")
public class GetStaticCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(GetStaticCommand.class);

    private String classPattern;
    private String fieldPattern;
    private String express;
    private String hashCode = null;
    private String classLoaderClass;
    private boolean isRegEx = false;
    private int expand = 1;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "field-pattern", index = 1)
    @Description("Field name pattern")
    public void setFieldPattern(String fieldPattern) {
        this.fieldPattern = fieldPattern;
    }

    @Argument(argName = "express", index = 2, required = false)
    @Description("the content you want to watch, written by ognl")
    public void setExpress(String express) {
        this.express = express;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
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

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Override
    public void process(CommandProcess process) {
        RowAffect affect = new RowAffect();
        Instrumentation inst = process.session().getInstrumentation();

        if (hashCode == null && classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                GetStaticModel getStaticModel = new GetStaticModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(getStaticModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, hashCode);
        try {
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                process.end(-1, "No class found for: " + classPattern);
                return;
            }
            ExitStatus status = null;
            if (matchedClasses.size() > 1) {
                status = processMatches(process, matchedClasses);
            } else {
                status = processExactMatch(process, affect, inst, matchedClasses);
            }
            process.appendResult(new RowAffectModel(affect));
            CommandUtils.end(process, status);
        } catch (Throwable e){
            logger.error("processing error", e);
            process.appendResult(new RowAffectModel(affect));
            process.end(-1, "processing error");
        }
    }

    private ExitStatus processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst,
                                   Set<Class<?>> matchedClasses) {
        Matcher<String> fieldNameMatcher = fieldNameMatcher();

        Class<?> clazz = matchedClasses.iterator().next();

        boolean found = false;

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !fieldNameMatcher.matching(field.getName())) {
                continue;
            }
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            try {
                Object value = field.get(null);

                if (!StringUtils.isEmpty(express)) {
                    value = ExpressFactory.threadLocalExpress(value).get(express);
                }

                process.appendResult(new GetStaticModel(field.getName(), value, expand));

                affect.rCnt(1);
            } catch (IllegalAccessException e) {
                logger.warn("getstatic: failed to get static value, class: {}, field: {} ", clazz, field.getName(), e);
                process.appendResult(new MessageModel("Failed to get static, exception message: " + e.getMessage()
                              + ", please check $HOME/logs/arthas/arthas.log for more details. "));
            } catch (ExpressException e) {
                logger.warn("getstatic: failed to get express value, class: {}, field: {}, express: {}", clazz, field.getName(), express, e);
                process.appendResult(new MessageModel("Failed to get static, exception message: " + e.getMessage()
                              + ", please check $HOME/logs/arthas/arthas.log for more details. "));
            } finally {
                found = true;
            }
        }

        if (!found) {
            return ExitStatus.failure(-1, "getstatic: no matched static field was found");
        } else {
            return ExitStatus.success();
        }
    }

    private ExitStatus processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {

//        Element usage = new LabelElement("getstatic -c <hashcode> " + classPattern + " " + fieldPattern).style(
//                Decoration.bold.fg(Color.blue));
//        process.write("\n Found more than one class for: " + classPattern + ", Please use " + RenderUtil.render(usage, process.width()));
        //TODO support message style
        String usage = "getstatic -c <hashcode> " + classPattern + " " + fieldPattern;
        process.appendResult(new MessageModel("Found more than one class for: " + classPattern + ", Please use: "+usage));

        List<ClassVO> matchedClassVOs = ClassUtils.createClassVOList(matchedClasses);
        process.appendResult(new GetStaticModel(matchedClassVOs));
        return ExitStatus.failure(-1, "Found more than one class for: " + classPattern + ", Please use: "+usage);
    }

    private Matcher<String> fieldNameMatcher() {
        return isRegEx ? new RegexMatcher(fieldPattern) : new WildcardMatcher(fieldPattern);
    }
}
