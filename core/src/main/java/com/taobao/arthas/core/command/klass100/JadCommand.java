package com.taobao.arthas.core.command.klass100;

import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.TypeRenderUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.logger.Logger;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.lang.LangRenderUtil;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;
import org.benf.cfr.reader.Main;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import static com.taobao.text.ui.Element.label;

/**
 * @author diecui1202 on 15/11/24.
 */
@Name("jad")
@Summary("Decompile class")
@Description(Constants.EXAMPLE +
        "  jad -c 39eb305e org.apache.log4j.Logger\n" +
        "  jad -c 39eb305e org/apache/log4j/Logger\n" +
        "  jad -c 39eb305e -E org\\\\.apache\\\\.*\\\\.StringUtils\n" +
        Constants.WIKI + Constants.WIKI_HOME + "jad")
public class JadCommand extends AnnotatedCommand {
    private static final Logger logger = LogUtil.getArthasLogger();
    private static Pattern pattern = Pattern.compile("(?m)^/\\*\\s*\\*/\\s*$" + System.getProperty("line.separator"));
    private static final String OUTPUTOPTION = "--outputdir";
    private static final String COMMENTS = "--comments";
    private static final String DecompilePath = new File(LogUtil.LOGGER_FILE).getParent() + File.separator + "decompile";

    private String classPattern;
    private String methodName;
    private String code = null;
    private boolean isRegEx = false;
    private boolean showLineNumbers = false;

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
        this.isRegEx = regEx;
    }

    @Option(shortName = "l",longName = "show-LN",flag = true)
    @Description("Show line numbers")
    public void setShowLineNumbers(boolean showLineNumbers){
        this.showLineNumbers = showLineNumbers;
    }


    @Override
    public void process(CommandProcess process) {
        RowAffect affect = new RowAffect();
        Instrumentation inst = process.session().getInstrumentation();
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, code);

        try {
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                processNoMatch(process);
            } else if (matchedClasses.size() > 1) {
                processMatches(process, matchedClasses);
            } else {
                Set<Class<?>> withInnerClasses = SearchUtils.searchClassOnly(inst,  classPattern + "(?!.*\\$\\$Lambda\\$).*", true, code);
                processExactMatch(process, affect, inst, matchedClasses, withInnerClasses);
            }
        } finally {
            process.write(affect + "\n");
            process.end();
        }
    }

    private void processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst, Set<Class<?>> matchedClasses, Set<Class<?>> withInnerClasses) {
        Class<?> c = matchedClasses.iterator().next();
        matchedClasses = withInnerClasses;

        try {
            ClassDumpTransformer transformer = new ClassDumpTransformer(matchedClasses);
            Enhancer.enhance(inst, transformer, matchedClasses);
            Map<Class<?>, File> classFiles = transformer.getDumpResult();
            File classFile = classFiles.get(c);

            String source;
            source = decompileWithCFR(classFile.getAbsolutePath(), c, methodName);
            if (source != null) {
                source = pattern.matcher(source).replaceAll("");
                if (showLineNumbers) {
                    source = addLineNumbers(source);
                }
            } else {
                source = "unknown";
            }


            process.write("\n");
            process.write(RenderUtil.render(new LabelElement("ClassLoader: ").style(Decoration.bold.fg(Color.red)), process.width()));
            process.write(RenderUtil.render(TypeRenderUtils.drawClassLoader(c), process.width()) + "\n");
            process.write(RenderUtil.render(new LabelElement("Location: ").style(Decoration.bold.fg(Color.red)), process.width()));
            process.write(RenderUtil.render(new LabelElement(SearchClassCommand.getCodeSource(
                    c.getProtectionDomain().getCodeSource())).style(Decoration.bold.fg(Color.blue)), process.width()) + "\n");
            process.write(LangRenderUtil.render(source) + "\n");
            process.write(com.taobao.arthas.core.util.Constants.EMPTY_STRING);
            affect.rCnt(classFiles.keySet().size());
        } catch (Throwable t) {
            logger.error(null, "jad: fail to decompile class: " + c.getName(), t);
        }
    }

    private void processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {
        Element usage = new LabelElement("jad -c <hashcode> " + classPattern).style(Decoration.bold.fg(Color.blue));
        process.write("\n Found more than one class for: " + classPattern + ", Please use "
                + RenderUtil.render(usage, process.width()));

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

    private void processNoMatch(CommandProcess process) {
        process.write("No class found for: " + classPattern + "\n");
    }

    private String decompileWithCFR(String classPath, Class<?> clazz, String methodName) {
        List<String> options = new ArrayList<String>();
        options.add(classPath);
//        options.add(clazz.getName());
        if (methodName != null) {
            options.add(methodName);
        }
        options.add(OUTPUTOPTION);
        options.add(DecompilePath);
        options.add(COMMENTS);
        options.add("false");
        String args[] = new String[options.size()];
        options.toArray(args);
        Main.main(args);
        String outputFilePath = DecompilePath + File.separator + Type.getInternalName(clazz) + ".java";
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            try {
                return FileUtils.readFileToString(outputFile, Charset.defaultCharset());
            } catch (IOException e) {
                logger.error(null, "error read decompile result in: " + outputFilePath, e);
            }
        }

        return null;
    }

    private String addLineNumbers(String source) {
        StringBuilder returnSource = new StringBuilder();
        String[] split = source.split("\n");
        for (int i = 0; i < split.length; i++) {
            returnSource.append(i + 1).append("\t").append(split[i]).append("\n");
        }
        return returnSource.toString();
    }

}
