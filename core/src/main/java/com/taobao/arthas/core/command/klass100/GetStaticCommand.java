package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.arthas.core.view.ObjectView;
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

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;

import static com.taobao.text.ui.Element.label;

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

    private static final Logger logger = LogUtil.getArthasLogger();

    private String classPattern;
    private String fieldPattern;
    private String express;
    private String hashCode = null;
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
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, hashCode);

        try {
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                process.write("No class found for: " + classPattern + "\n");
            } else if (matchedClasses.size() > 1) {
                processMatches(process, matchedClasses);
            } else {
                processExactMatch(process, affect, inst, matchedClasses);
            }
        } finally {
            process.write(affect + "\n");
            process.end();
        }
    }

    private void processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst,
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

                String result = StringUtils.objectToString(expand >= 0 ? new ObjectView(value, expand).draw() : value);
                process.write("field: " + field.getName() + "\n" + result + "\n");

                affect.rCnt(1);
            } catch (IllegalAccessException e) {
                logger.warn("getstatic: failed to get static value, class: " + clazz + ", field: " + field.getName(),
                            e);
                process.write("Failed to get static, exception message: " + e.getMessage()
                              + ", please check $HOME/logs/arthas/arthas.log for more details. \n");
            } catch (ExpressException e) {
                logger.warn("getstatic: failed to get express value, class: " + clazz + ", field: " + field.getName()
                            + ", express: " + express, e);
                process.write("Failed to get static, exception message: " + e.getMessage()
                              + ", please check $HOME/logs/arthas/arthas.log for more details. \n");
            } finally {
                found = true;
            }
        }

        if (!found) {
            process.write("getstatic: no matched static field was found\n");
        }
    }

    private void processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {
        Element usage = new LabelElement("getstatic -c <hashcode> " + classPattern + " " + fieldPattern).style(
                Decoration.bold.fg(Color.blue));
        process.write("\n Found more than one class for: " + classPattern + ", Please use " + RenderUtil.render(usage,
                                                                                                                process.width()));

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

    private Matcher<String> fieldNameMatcher() {
        return isRegEx ? new RegexMatcher(fieldPattern) : new WildcardMatcher(fieldPattern);
    }
}
