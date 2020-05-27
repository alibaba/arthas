package com.taobao.arthas.core.command.klass100;

import static com.taobao.text.Decoration.bold;
import static com.taobao.text.ui.Element.label;
import static java.lang.String.format;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

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
    private boolean isDetail = false;
    private boolean isRegEx = false;

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

    @Override
    public void process(CommandProcess process) {
        RowAffect affect = new RowAffect();

        Instrumentation inst = process.session().getInstrumentation();
        Matcher<String> methodNameMatcher = methodNameMatcher();
        Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, hashCode);

        for (Class<?> clazz : matchedClasses) {
            try {
                for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                    String methodNameWithDescriptor = com.alibaba.arthas.deps.org.objectweb.asm.commons.Method.getMethod(constructor).toString();
                    if (!methodNameMatcher.matching("<init>")) {
                        continue;
                    }

                    if (isDetail) {
                        process.write(RenderUtil.render(renderConstructor(constructor, clazz), process.width()) + "\n");
                    } else {
                        String line = format("%s %s%n", clazz.getName(), methodNameWithDescriptor);
                        process.write(line);
                    }
                    affect.rCnt(1);
                }

                for (Method method : clazz.getDeclaredMethods()) {
                    String methodNameWithDescriptor = com.alibaba.arthas.deps.org.objectweb.asm.commons.Method.getMethod(method).toString();
                    if (!methodNameMatcher.matching(method.getName())) {
                        continue;
                    }

                    if (isDetail) {
                        process.write(RenderUtil.render(renderMethod(method, clazz), process.width()) + "\n");
                    } else {
                        String line = format("%s %s%n", clazz.getName(), methodNameWithDescriptor);
                        process.write(line);
                    }
                    affect.rCnt(1);
                }
            } catch (Error e) {
                //print failed className
                String msg = String.format("process class failed: %s, error: %s", clazz.getName(), e.toString());
                logger.error(msg, e);
                process.write(msg).write("\n");
                throw e;
            }
        }

        process.write(affect + "\n");
        process.end();
    }

    private Matcher<String> methodNameMatcher() {
        // auto fix default methodPattern
        if (StringUtils.isBlank(methodPattern)) {
            methodPattern = isRegEx ? ".*" : "*";
        }
        return isRegEx ? new RegexMatcher(methodPattern) : new WildcardMatcher(methodPattern);
    }

    private Element renderMethod(Method method, Class<?> clazz) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);

        table.row(label("declaring-class").style(bold.bold()), label(method.getDeclaringClass().getName()))
                .row(label("method-name").style(bold.bold()), label(method.getName()).style(bold.bold()))
                .row(label("modifier").style(bold.bold()), label(StringUtils.modifier(method.getModifiers(), ',')))
                .row(label("annotation").style(bold.bold()), label(TypeRenderUtils.drawAnnotation(method)))
                .row(label("parameters").style(bold.bold()), label(TypeRenderUtils.drawParameters(method)))
                .row(label("return").style(bold.bold()), label(TypeRenderUtils.drawReturn(method)))
                .row(label("exceptions").style(bold.bold()), label(TypeRenderUtils.drawExceptions(method)))
                .row(label("classLoaderHash").style(bold.bold()), label(StringUtils.classLoaderHash(clazz)));
        return table;
    }

    private Element renderConstructor(Constructor<?> constructor, Class<?> clazz) {
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);

        table.row(label("declaring-class").style(bold.bold()), label(constructor.getDeclaringClass().getName()))
             .row(label("constructor-name").style(bold.bold()), label("<init>").style(bold.bold()))
             .row(label("modifier").style(bold.bold()), label(StringUtils.modifier(constructor.getModifiers(), ',')))
             .row(label("annotation").style(bold.bold()), label(TypeRenderUtils.drawAnnotation(constructor.getDeclaredAnnotations())))
             .row(label("parameters").style(bold.bold()), label(TypeRenderUtils.drawParameters(constructor)))
             .row(label("exceptions").style(bold.bold()), label(TypeRenderUtils.drawExceptions(constructor)))
             .row(label("classLoaderHash").style(bold.bold()), label(StringUtils.classLoaderHash(clazz)));
        return table;
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
