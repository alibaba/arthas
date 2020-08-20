package com.taobao.arthas.core.command.klass100;


import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassDetailVO;
import com.taobao.arthas.core.command.model.SearchClassModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ResultUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 展示类信息
 *
 * @author vlinux
 */
@Name("sc")
@Summary("Search all the classes loaded by JVM")
@Description(Constants.EXAMPLE +
        "  sc -d org.apache.commons.lang.StringUtils\n" +
        "  sc -d org/apache/commons/lang/StringUtils\n" +
        "  sc -d *StringUtils\n" +
        "  sc -d -f org.apache.commons.lang.StringUtils\n" +
        "  sc -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils\n" +
        Constants.WIKI + Constants.WIKI_HOME + "sc")
public class SearchClassCommand extends AnnotatedCommand {
    private String classPattern;
    private boolean isDetail = false;
    private boolean isField = false;
    private boolean isRegEx = false;
    private String hashCode = null;
    private String classLoaderClass;
    private Integer expand;
    private int numberOfLimit = 100;

    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Option(shortName = "d", longName = "details", flag = true)
    @Description("Display the details of class")
    public void setDetail(boolean detail) {
        isDetail = detail;
    }

    @Option(shortName = "f", longName = "field", flag = true)
    @Description("Display all the member variables")
    public void setField(boolean field) {
        isField = field;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (0 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
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
    @Description("Maximum number of matching classes with details (100 by default)")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }

    @Override
    public void process(final CommandProcess process) {
        // TODO: null check
        RowAffect affect = new RowAffect();
        Instrumentation inst = process.session().getInstrumentation();

        if (hashCode == null && classLoaderClass != null) {
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            if (matchedClassLoaders.size() == 1) {
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                SearchClassModel searchclassModel = new SearchClassModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(searchclassModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        List<Class<?>> matchedClasses = new ArrayList<Class<?>>(SearchUtils.searchClass(inst, classPattern, isRegEx, hashCode));
        Collections.sort(matchedClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> c1, Class<?> c2) {
                return StringUtils.classname(c1).compareTo(StringUtils.classname(c2));
            }
        });

        if (isDetail) {
            if (numberOfLimit > 0 && matchedClasses.size() > numberOfLimit) {
                process.end(-1, "The number of matching classes is greater than : " + numberOfLimit+". \n" +
                        "Please specify a more accurate 'class-patten' or use the parameter '-n' to change the maximum number of matching classes.");
                return;
            }
            for (Class<?> clazz : matchedClasses) {
                ClassDetailVO classInfo = ClassUtils.createClassInfo(clazz, isField);
                process.appendResult(new SearchClassModel(classInfo, isDetail, isField, expand));
            }
        } else {
            int pageSize = 256;
            ResultUtils.processClassNames(matchedClasses, pageSize, new ResultUtils.PaginationHandler<List<String>>() {
                @Override
                public boolean handle(List<String> classNames, int segment) {
                    process.appendResult(new SearchClassModel(classNames, segment));
                    return true;
                }
            });
        }

        affect.rCnt(matchedClasses.size());
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeClassName(completion)) {
            super.complete(completion);
        }
    }
}
