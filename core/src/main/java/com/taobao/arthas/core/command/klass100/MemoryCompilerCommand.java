package com.taobao.arthas.core.command.klass100;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.compiler.DynamicCompiler;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MemoryCompilerModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 *
 * @author hengyunabc 2019-02-05
 *
 */
@Name("mc")
@Summary("Memory compiler, compiles java files into bytecode and class files in memory.")
@Description(Constants.EXAMPLE + "  mc /tmp/Test.java\n" + "  mc -c 327a647b /tmp/Test.java\n"
                + "  mc -d /tmp/output /tmp/ClassA.java /tmp/ClassB.java\n" + Constants.WIKI + Constants.WIKI_HOME
                + "mc")
public class MemoryCompilerCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(MemoryCompilerCommand.class);

    private String directory;
    private String hashCode;
    private String encoding;

    private List<String> sourcefiles;

    @Argument(argName = "sourcefiles", index = 0)
    @Description("source files")
    public void setClassPattern(List<String> sourcefiles) {
        this.sourcefiles = sourcefiles;
    }

    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    @Option(longName = "encoding")
    @Description("Source file encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Option(shortName = "d", longName = "directory")
    @Description("Sets the destination directory for class files")
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public void process(final CommandProcess process) {
        RowAffect affect = new RowAffect();

        try {
            Instrumentation inst = process.session().getInstrumentation();
            ClassLoader classloader = null;
            if (hashCode == null) {
                classloader = ClassLoader.getSystemClassLoader();
            } else {
                classloader = ClassLoaderUtils.getClassLoader(inst, hashCode);
                if (classloader == null) {
                    process.end(-1, "Can not find classloader with hashCode: " + hashCode + ".");
                    return;
                }
            }

            DynamicCompiler dynamicCompiler = new DynamicCompiler(classloader);

            Charset charset = Charset.defaultCharset();
            if (encoding != null) {
                charset = Charset.forName(encoding);
            }

            for (String sourceFile : sourcefiles) {
                String sourceCode = FileUtils.readFileToString(new File(sourceFile), charset);
                String name = new File(sourceFile).getName();
                if (name.endsWith(".java")) {
                    name = name.substring(0, name.length() - ".java".length());
                }
                dynamicCompiler.addSource(name, sourceCode);
            }

            Map<String, byte[]> byteCodes = dynamicCompiler.buildByteCodes();

            File outputDir = null;
            if (this.directory != null) {
                outputDir = new File(this.directory);
            } else {
                outputDir = new File("").getAbsoluteFile();
            }

            List<String> files = new ArrayList<String>();
            for (Entry<String, byte[]> entry : byteCodes.entrySet()) {
                File byteCodeFile = new File(outputDir, entry.getKey().replace('.', '/') + ".class");
                FileUtils.writeByteArrayToFile(byteCodeFile, entry.getValue());
                files.add(byteCodeFile.getAbsolutePath());
                affect.rCnt(1);
            }
            process.appendResult(new MemoryCompilerModel(files));
            process.appendResult(new RowAffectModel(affect));
            process.end();
        } catch (Throwable e) {
            logger.warn("Memory compiler error", e);
            process.end(-1, "Memory compiler error, exception message: " + e.getMessage()
                            + ", please check $HOME/logs/arthas/arthas.log for more details.");
        }
    }

    @Override
    public void complete(Completion completion) {
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }
}
