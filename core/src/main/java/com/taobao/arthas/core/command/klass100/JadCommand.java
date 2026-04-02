package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.Pair;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.JadModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;
import com.taobao.arthas.core.util.*;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * jad 反编译命令实现类
 * 用于反编译已加载的类的字节码，还原为可读的 Java 源代码
 *
 * @author diecui1202 on 15/11/24.
 * @author hengyunabc 2018-11-16
 */
@Name("jad")
@Summary("Decompile class")
@Description(Constants.EXAMPLE +
        "  jad java.lang.String\n" +
        "  jad java.lang.String toString\n" +
        "  jad java.lang.String -d /tmp/jad/dump\n" +
        "  jad --source-only java.lang.String\n" +
        "  jad -c 39eb305e org/apache/log4j/Logger\n" +
        "  jad -c 39eb305e -E org\\\\.apache\\\\.*\\\\.StringUtils\n" +
        Constants.WIKI + Constants.WIKI_HOME + "jad")
public class JadCommand extends AnnotatedCommand {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(JadCommand.class);
    // 正则表达式模式，用于匹配反编译结果中的空 Javadoc 注释
    private static Pattern pattern = Pattern.compile("(?m)^/\\*\\s*\\*/\\s*$" + System.getProperty("line.separator"));

    // 类名匹配模式
    private String classPattern;
    // 方法名模式，如果指定则只反编译指定方法
    private String methodName;
    // 类加载器的哈希码
    private String code = null;
    // 类加载器的类名
    private String classLoaderClass;
    // 是否使用正则表达式匹配
    private boolean isRegEx = false;
    // 是否隐藏 Unicode 字符
    private boolean hideUnicode = false;
    // 是否显示行号
    private boolean lineNumber;
    // 指定类文件转储目录
    private String directory;

    /**
     * 是否只输出源代码
     * 为 true 时，不输出类信息、位置等额外信息
     */
    private boolean sourceOnly = false;

    /**
     * 设置类名匹配模式
     *
     * @param classPattern 类名模式，支持使用 '.' 或 '/' 作为分隔符
     */
    @Argument(argName = "class-pattern", index = 0)
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        // 标准化类名格式，统一使用 '.' 作为分隔符
        this.classPattern = StringUtils.normalizeClassName(classPattern);
    }

    /**
     * 设置方法名模式
     * 如果指定，则只反编译该特定方法而不是整个类
     *
     * @param methodName 方法名
     */
    @Argument(argName = "method-name", index = 1, required = false)
    @Description("Method name pattern, decompile a specific method instead of the whole class")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }


    /**
     * 设置类加载器的哈希码
     * 用于指定在特定的类加载器中查找类
     *
     * @param code 类加载器的哈希码（十六进制字符串）
     */
    @Option(shortName = "c", longName = "code")
    @Description("The hash code of the special class's classLoader")
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 设置类加载器的类名
     * 通过类加载器的类名来指定使用哪个类加载器
     *
     * @param classLoaderClass 类加载器的类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置是否使用正则表达式匹配
     * 默认使用通配符匹配
     *
     * @param regEx true 表示使用正则表达式，false 表示使用通配符
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置是否隐藏 Unicode 字符
     *
     * @param hideUnicode true 表示隐藏 Unicode 字符，false 表示显示
     */
    @Option(longName = "hideUnicode", flag = true)
    @Description("Hide unicode, default value false")
    public void setHideUnicode(boolean hideUnicode) {
        this.hideUnicode = hideUnicode;
    }

    /**
     * 设置是否只输出源代码
     *
     * @param sourceOnly true 表示只输出源代码，不输出类信息等
     */
    @Option(longName = "source-only", flag = true)
    @Description("Output source code only")
    public void setSourceOnly(boolean sourceOnly) {
        this.sourceOnly = sourceOnly;
    }

    /**
     * 设置是否显示行号
     *
     * @param lineNumber true 表示显示行号，false 表示不显示
     */
    @Option(longName = "lineNumber")
    @DefaultValue("true")
    @Description("Output source code contains line number, default value true")
    public void setLineNumber(boolean lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * 设置类文件转储目录
     * CFR 反编译器需要先将类文件转储到磁盘才能进行反编译
     *
     * @param directory 转储目录路径
     */
    @Option(shortName = "d", longName = "directory")
    @Description("Sets the destination directory for dumped class files required by cfr decompiler")
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * 处理 jad 命令
     * 这是命令的核心执行方法
     *
     * @param process 命令处理上下文对象
     */
    @Override
    public void process(CommandProcess process) {
        // 如果指定了目录，检查是否为有效的目录
        if (directory != null && !FileUtils.isDirectoryOrNotExist(directory)) {
            process.end(-1, directory + " :is not a directory, please check it");
            return;
        }
        // 获取 JVM Instrumentation 实例，用于类操作
        Instrumentation inst = process.session().getInstrumentation();

        // 如果没有指定类加载器哈希码，但指定了类加载器类名
        if (code == null && classLoaderClass != null) {
            // 通过类加载器类名查找匹配的类加载器
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
            // 如果只找到一个匹配的类加载器
            if (matchedClassLoaders.size() == 1) {
                // 将类加载器的哈希码转换为十六进制字符串
                code = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                // 如果找到多个匹配的类加载器，提示用户使用 -c 参数指定
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                JadModel jadModel = new JadModel()
                        .setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(jadModel);
                process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                // 如果没有找到匹配的类加载器
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        // 根据类名模式搜索匹配的类
        Set<Class<?>> matchedClasses = SearchUtils.searchClassOnly(inst, classPattern, isRegEx, code);

        try {
            // 创建行影响统计对象
            final RowAffect affect = new RowAffect();
            final ExitStatus status;
            // 如果没有找到匹配的类
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                status = processNoMatch(process);
            } else if (matchedClasses.size() > 1) {
                // 如果找到多个匹配的类
                status = processMatches(process, matchedClasses);
            } else { // matchedClasses size is 1
                // 只找到一个匹配的类，查找内部类
                // 使用类名 + "$*" 模式查找所有内部类
                Set<Class<?>> withInnerClasses = SearchUtils.searchClassOnly(inst,  matchedClasses.iterator().next().getName() + "$*", false, code);
                // 如果没有找到内部类，使用原匹配结果
                if(withInnerClasses.isEmpty()) {
                    withInnerClasses = matchedClasses;
                }
                // 处理精确匹配，包含主类和内部类
                status = processExactMatch(process, affect, inst, matchedClasses, withInnerClasses);
            }
            // 如果不是只输出源代码模式，添加行影响统计结果
            if (!this.sourceOnly) {
                process.appendResult(new RowAffectModel(affect));
            }
            // 结束命令处理
            CommandUtils.end(process, status);
        } catch (Throwable e){
            // 捕获所有异常，记录错误日志
            logger.error("processing error", e);
            process.end(-1, "processing error");
        }
    }

    /**
     * 处理精确匹配的情况
     * 当只找到一个匹配的类时，进行反编译操作
     *
     * @param process 命令处理上下文
     * @param affect 行影响统计对象
     * @param inst JVM Instrumentation 实例
     * @param matchedClasses 匹配的类集合（只包含一个主类）
     * @param withInnerClasses 包含内部类的集合
     * @return 命令执行状态
     */
    private ExitStatus processExactMatch(CommandProcess process, RowAffect affect, Instrumentation inst, Set<Class<?>> matchedClasses, Set<Class<?>> withInnerClasses) {
        // 获取匹配的主类
        Class<?> c = matchedClasses.iterator().next();
        // 创建包含主类和所有内部类的集合
        Set<Class<?>> allClasses = new HashSet<>(withInnerClasses);
        allClasses.add(c);
        try {
            // 创建类文件转储转换器
            final ClassDumpTransformer transformer;
            // 如果没有指定目录，使用默认临时目录
            if (directory == null) {
                transformer = new ClassDumpTransformer(allClasses);
            } else {
                // 使用用户指定的目录
                transformer = new ClassDumpTransformer(allClasses, new File(directory));
            }
            // 使用转换器重新转换类，将类的字节码转储到文件
            InstrumentationUtils.retransformClasses(inst, transformer, allClasses);

            // 获取转储的类文件映射
            Map<Class<?>, File> classFiles = transformer.getDumpResult();
            // 如果转储失败
            if (classFiles == null || classFiles.isEmpty()) {
                return ExitStatus.failure(-1, "jad: fail to dump class file for decompiler, make sure you have write permission of the directory \"" + transformer.dumpDir() +
                "\" or try with \"-d/--directory\" to specify the directory of dump files");
            }
            // 获取主类的转储文件
            File classFile = classFiles.get(c);
            // 使用 CFR 反编译器进行反编译，返回源代码和行号映射
            Pair<String,NavigableMap<Integer,Integer>> decompileResult = Decompiler.decompileWithMappings(classFile.getAbsolutePath(), methodName, hideUnicode, lineNumber);
            // 获取反编译的源代码
            String source = decompileResult.getFirst();
            // 移除空的 Javadoc 注释
            if (source != null) {
                source = pattern.matcher(source).replaceAll("");
            } else {
                source = "unknown";
            }
            // 创建反编译结果模型
            JadModel jadModel = new JadModel();
            // 设置反编译的源代码
            jadModel.setSource(source);
            // 设置行号映射关系
            jadModel.setMappings(decompileResult.getSecond());
            // 如果不是只输出源代码模式，添加类信息和位置信息
            if (!this.sourceOnly) {
                jadModel.setClassInfo(ClassUtils.createSimpleClassInfo(c));
                jadModel.setLocation(ClassUtils.getCodeSource(c.getProtectionDomain().getCodeSource()));
            }
            // 将反编译结果添加到命令输出
            process.appendResult(jadModel);
            // 统计处理的类数量
            affect.rCnt(classFiles.keySet().size());
            return ExitStatus.success();
        } catch (Throwable t) {
            // 捕获反编译过程中的所有异常
            logger.error("jad: fail to decompile class: " + c.getName(), t);
            return ExitStatus.failure(-1, "jad: fail to decompile class: " + c.getName() + ", please check $HOME/logs/arthas/arthas.log for more details.");
        }
    }

    /**
     * 处理多个类匹配的情况
     * 当找到多个匹配的类时，提示用户使用 -c 参数指定类加载器
     *
     * @param process 命令处理上下文
     * @param matchedClasses 匹配的类集合
     * @return 命令执行状态（失败状态）
     */
    private ExitStatus processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {

        // 构建使用提示字符串
        String usage = "jad -c <hashcode> " + classPattern;
        String msg = " Found more than one class for: " + classPattern + ", Please use " + usage;
        process.appendResult(new MessageModel(msg));

        // 将匹配的类信息转换为视图对象并添加到结果
        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        JadModel jadModel = new JadModel();
        jadModel.setMatchedClasses(classVOs);
        process.appendResult(jadModel);

        return ExitStatus.failure(-1, msg);
    }

    /**
     * 处理没有匹配类的情况
     *
     * @param process 命令处理上下文
     * @return 命令执行状态（失败状态）
     */
    private ExitStatus processNoMatch(CommandProcess process) {
        return ExitStatus.failure(-1, "No class found for: " + classPattern);
    }

    /**
     * 命令自动补全方法
     * 用于在用户输入命令时提供自动补全建议
     *
     * @param completion 补全上下文对象
     */
    @Override
    public void complete(Completion completion) {
        // 检测当前正在补全哪个参数
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);

        // 如果正在补全第一个参数（类名）
        if (argumentIndex == 1) {
            // 尝试补全类名
            if (!CompletionUtils.completeClassName(completion)) {
                // 如果无法补全类名，调用父类的默认补全逻辑
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 2) {
            // 如果正在补全第二个参数（方法名）
            // 尝试补全方法名
            if (!CompletionUtils.completeMethodName(completion)) {
                // 如果无法补全方法名，调用父类的默认补全逻辑
                super.complete(completion);
            }
            return;
        }

        // 其他情况调用父类的默认补全逻辑
        super.complete(completion);
    }
}
