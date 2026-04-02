package com.taobao.arthas.core.command.klass100;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Collection;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.compiler.DynamicCompiler;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MemoryCompilerModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.FileUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * 内存编译器命令
 *
 * 此命令用于在内存中编译Java源代码文件，生成字节码和class文件。
 * 这是Arthas工具中非常重要的一个功能，允许用户在不重启应用的情况下动态编译和加载Java代码。
 *
 * 主要功能：
 * 1. 编译Java源文件为字节码
 * 2. 支持指定类加载器进行编译
 * 3. 支持指定输出目录
 * 4. 支持指定源文件编码
 * 5. 可以编译多个源文件
 *
 * 使用场景：
 * - 热修复：在不停机的情况下修复代码bug
 * - 动态调试：快速测试代码修改
 * - 临时补丁：为生产环境应用临时修复
 *
 * @author hengyunabc 2019-02-05
 */
@Name("mc")
@Summary("Memory compiler, compiles java files into bytecode and class files in memory.")
@Description(Constants.EXAMPLE + "  mc /tmp/Test.java\n" + "  mc -c 327a647b /tmp/Test.java\n"
                + "  mc -d /tmp/output /tmp/ClassA.java /tmp/ClassB.java\n" + Constants.WIKI + Constants.WIKI_HOME
                + "mc")
public class MemoryCompilerCommand extends AnnotatedCommand {

    /**
     * 日志记录器，用于记录命令执行过程中的信息和错误
     */
    private static final Logger logger = LoggerFactory.getLogger(MemoryCompilerCommand.class);

    /**
     * 编译后class文件的输出目录
     * 如果为null，则输出到当前工作目录
     */
    private String directory;

    /**
     * 指定类加载器的哈希码
     * 用于标识使用哪个类加载器来加载编译后的类
     */
    private String hashCode;

    /**
     * 指定类加载器的类名
     * 通过类名来查找对应的类加载器
     */
    private String classLoaderClass;

    /**
     * 源文件的编码格式
     * 如果为null，则使用系统默认编码
     */
    private String encoding;

    /**
     * 待编译的Java源文件列表
     * 可以包含一个或多个Java源文件路径
     */
    private List<String> sourcefiles;

    /**
     * 设置待编译的源文件列表
     * 这是命令的主要参数，接受一个或多个Java源文件路径
     *
     * @param sourcefiles Java源文件路径列表
     */
    @Argument(argName = "sourcefiles", index = 0)
    @Description("source files")
    public void setClassPattern(List<String> sourcefiles) {
        this.sourcefiles = sourcefiles;
    }

    /**
     * 设置类加载器的哈希码
     * 通过指定类加载器的哈希码来选择特定的类加载器进行编译
     *
     * @param hashCode 类加载器的哈希码（十六进制字符串）
     */
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special ClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 设置类加载器的类名
     * 通过类名来查找对应的类加载器，如果找到多个匹配的类加载器，会提示用户进一步指定
     *
     * @param classLoaderClass 类加载器的完整类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置源文件的编码格式
     * 用于正确读取包含非ASCII字符的Java源文件
     *
     * @param encoding 编码格式，如 UTF-8、GBK 等
     */
    @Option(longName = "encoding")
    @Description("Source file encoding")
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * 设置编译后class文件的输出目录
     * 指定编译生成的class文件保存的目录路径
     *
     * @param directory 输出目录的绝对路径或相对路径
     */
    @Option(shortName = "d", longName = "directory")
    @Description("Sets the destination directory for class files")
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * 处理内存编译命令
     *
     * 这是命令的核心执行方法，负责：
     * 1. 解析和验证类加载器参数
     * 2. 读取Java源文件
     * 3. 在内存中编译源代码
     * 4. 将编译后的字节码写入class文件
     *
     * @param process 命令处理上下文对象，用于访问会话信息和输出结果
     */
    @Override
    public void process(final CommandProcess process) {
        // 创建影响计数器，用于记录编译成功的文件数量
        RowAffect affect = new RowAffect();

        try {
            // 获取Java Instrumentation实例，用于访问类加载器信息
            Instrumentation inst = process.session().getInstrumentation();

            // 如果用户没有指定类加载器哈希码，但指定了类加载器类名
            // 则通过类名查找对应的类加载器
            if (hashCode == null && classLoaderClass != null) {
                // 根据类名查找所有匹配的类加载器
                List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
                // 如果只找到一个匹配的类加载器，使用它的哈希码
                if (matchedClassLoaders.size() == 1) {
                    hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                } else if (matchedClassLoaders.size() > 1) {
                    // 如果找到多个匹配的类加载器，提示用户进一步指定
                    Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                    MemoryCompilerModel memoryCompilerModel = new MemoryCompilerModel()
                            .setClassLoaderClass(classLoaderClass)
                            .setMatchedClassLoaders(classLoaderVOList);
                    process.appendResult(memoryCompilerModel);
                    process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                    return;
                } else {
                    // 如果没有找到匹配的类加载器，返回错误
                    process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                    return;
                }
            }

            // 确定要使用的类加载器
            ClassLoader classloader = null;
            if (hashCode == null) {
                // 如果没有指定类加载器，使用系统类加载器
                classloader = ClassLoader.getSystemClassLoader();
            } else {
                // 根据哈希码查找指定的类加载器
                classloader = ClassLoaderUtils.getClassLoader(inst, hashCode);
                if (classloader == null) {
                    process.end(-1, "Can not find classloader with hashCode: " + hashCode + ".");
                    return;
                }
            }

            // 创建动态编译器，使用指定的类加载器
            DynamicCompiler dynamicCompiler = new DynamicCompiler(classloader);

            // 确定源文件编码格式
            Charset charset = Charset.defaultCharset();
            if (encoding != null) {
                charset = Charset.forName(encoding);
            }

            // 遍历所有源文件，读取内容并添加到编译器
            for (String sourceFile : sourcefiles) {
                // 使用指定编码读取源文件内容
                String sourceCode = FileUtils.readFileToString(new File(sourceFile), charset);
                // 从文件名中提取类名（去除.java后缀）
                String name = new File(sourceFile).getName();
                if (name.endsWith(".java")) {
                    name = name.substring(0, name.length() - ".java".length());
                }
                // 将源代码添加到编译器
                dynamicCompiler.addSource(name, sourceCode);
            }

            // 执行编译，获取编译后的字节码
            // 返回的Map键为类全限定名，值为字节码数组
            Map<String, byte[]> byteCodes = dynamicCompiler.buildByteCodes();

            // 确定输出目录
            File outputDir = null;
            if (this.directory != null) {
                // 使用用户指定的输出目录
                outputDir = new File(this.directory);
            } else {
                // 默认使用当前工作目录
                outputDir = new File("").getAbsoluteFile();
            }

            // 将编译后的字节码写入class文件
            List<String> files = new ArrayList<String>();
            for (Entry<String, byte[]> entry : byteCodes.entrySet()) {
                // 根据类的全限定名创建文件路径（将.替换为/）
                File byteCodeFile = new File(outputDir, entry.getKey().replace('.', '/') + ".class");
                // 将字节码写入文件
                FileUtils.writeByteArrayToFile(byteCodeFile, entry.getValue());
                // 记录生成的文件路径
                files.add(byteCodeFile.getAbsolutePath());
                // 增加成功计数
                affect.rCnt(1);
            }
            // 将编译结果添加到命令输出
            process.appendResult(new MemoryCompilerModel(files));
            process.appendResult(new RowAffectModel(affect));
            // 正常结束命令
            process.end();
        } catch (Throwable e) {
            // 捕获所有异常，记录日志并返回错误信息
            logger.warn("Memory compiler error", e);
            process.end(-1, "Memory compiler error, exception message: " + e.getMessage()
                            + ", please check $HOME/logs/arthas/arthas.log for more details.");
        }
    }

    /**
     * 命令自动补全功能
     *
     * 当用户输入命令参数时，提供文件路径的自动补全功能。
     * 这可以让用户更方便地输入Java源文件的路径。
     *
     * @param completion 补全上下文对象，包含当前输入信息和可用的补全选项
     */
    @Override
    public void complete(Completion completion) {
        // 尝试进行文件路径补全
        // 如果补全失败（例如不是文件路径场景），则调用父类的默认补全逻辑
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }
}
