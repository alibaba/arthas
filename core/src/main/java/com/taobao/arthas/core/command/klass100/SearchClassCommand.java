package com.taobao.arthas.core.command.klass100;


import java.lang.instrument.Instrumentation;
import java.util.*;

import com.taobao.arthas.core.command.Constants;
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
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.text.ui.Element.label;

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
    private Integer expand;
    private String hashCode = null;

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


    @Override
    public void process(CommandProcess process) {
        // TODO: null check
        RowAffect affect = new RowAffect();
        Instrumentation inst = process.session().getInstrumentation();
        List<Class<?>> matchedClasses = new ArrayList<Class<?>>(SearchUtils.searchClassOnly(inst, classPattern, isRegEx, hashCode));
        Collections.sort(matchedClasses, new Comparator<Class<?>>() {
            @Override
            public int compare(Class<?> c1, Class<?> c2) {
                return StringUtils.classname(c1).compareTo(StringUtils.classname(c2));
            }
        });

        try {
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                process.write("No class found for: " + classPattern + "\n");
            }else if(matchedClasses.size() > 1){
                processMatches(process, matchedClasses);
                affect.rCnt(matchedClasses.size());
            }else{
                processClass(process, matchedClasses.iterator().next());
                affect.rCnt(1);
            }
        }finally {
            process.write(affect + "\n");
            process.end();
        }

    }

    private void processClass(CommandProcess process, Class<?> clazz) {
        if (isDetail) {
            process.write(RenderUtil.render(ClassUtils.renderClassInfo(clazz, isField, expand), process.width()) + "\n");
        } else {
            process.write(clazz.getName() + "\n");
        }
    }

    private void processMatches(CommandProcess process, List<Class<?>> matchedClasses) {
        Element usage = new LabelElement("sc -c <hashcode> " + classPattern).style(
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


    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeClassName(completion)) {
            super.complete(completion);
        }
    }
}
