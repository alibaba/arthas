package com.taobao.arthas.core.command.klass100;


import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.Collection;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.SearchMethodModel;
import com.taobao.arthas.core.command.model.MethodVO;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
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

/**
 * 展示方法信息
 *
 * @author vlinux
 * @author hengyunabc 2019-02-13
 */
@Name("sm")
@Summary("Search the method of classes loaded by JVM")
@Description(Constants.EXAMPLE +
        "  sm java.lang.String\n" +
        "  sm -d org.apache.commons.lang.StringUtils\n" +
        "  sm -d org/apache/commons/lang/StringUtils\n" +
        "  sm *StringUtils *\n" +
        "  sm -Ed org\\\\.apache\\\\.commons\\\\.lang\\.StringUtils .*\n" +
        Constants.WIKI + Constants.WIKI_HOME + "sm")
public class SearchMethodCommand extends AnnotatedCommand {
    private static final Logger logger = LoggerFactory.getLogger(SearchMethodCommand.class);

    private String classPattern;
    private String methodPattern;
    private String hashCode = null;
    private String classLoaderClass;
    private boolean isDetail = false;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(argName = "method-pattern", index = 1, required = false)
    @Description("Method name pattern")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display the details of method")
    public void setDetail(boolean detail) {
        isDetail = detail;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
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

    @Option(shortName = "n", longName = "limits")
    @Description("Maximum number of matching classes (100 by default)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Override
    public void process(CommandProcess process) {
        RowAffect affect = new RowAffect();

        Instrumentation inst = process.session().getInstrumentation();
        Matcher<String> methodNameMatcher = methodNameMatcher();
        
        if (hashCode == null && classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                SearchMethodModel searchmethodModel = new SearchMethodModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(searchmethodModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, hashCode);

        if (numberOfLimit > 0 && matchedClasses.size() > numberOfLimit) {
            process.end(-1, "The number of matching classes is greater than : " + numberOfLimit+". \n" +
                    "Please specify a more accurate 'class-patten' or use the parameter '-n' to change the maximum number of matching classes.");
            return;
        }
        for (Class<?> clazz : matchedClasses) {
            try {
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    if (!methodNameMatcher.matching("<init>")) {
                        continue;
                    }

                    MethodVO methodInfo = ClassUtils.createMethodInfo(constructor, clazz, isDetail);
                    process.appendResult(new SearchMethodModel(methodInfo, isDetail));
                    affect.rCnt(1);
                }

                for (Method method : clazz.getDeclaredMethods()) {
                    if (!methodNameMatcher.matching(method.getName())) {
                        continue;
                    }
                    MethodVO methodInfo = ClassUtils.createMethodInfo(method, clazz, isDetail);
                    process.appendResult(new SearchMethodModel(methodInfo, isDetail));
                    affect.rCnt(1);
                }
            } catch (Error e) {
                //print failed className
                String msg = String.format("process class failed: %s, error: %s", clazz.getName(), e.toString());
                logger.error(msg, e);
                process.end(1, msg);
                return;
            }
        }

        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    private Matcher<String> methodNameMatcher() {
        // auto fix default methodPattern
        if (StringUtils.isBlank(methodPattern)) {
            methodPattern = isRegEx ? ".*" : "*";
        }
        return isRegEx ? new RegexMatcher(methodPattern) : new WildcardMatcher(methodPattern);
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
