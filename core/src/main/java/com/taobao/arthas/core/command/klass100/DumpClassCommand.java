package com.taobao.arthas.core.command.klass100;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassVO;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.DumpClassModel;
import com.taobao.arthas.core.command.model.DumpClassVO;
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
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;


/**
 * Dump类命令
 *
 * 该命令用于将JVM中已加载的类的字节码导出到文件系统中，便于离线分析类的内容。
 * 支持通配符匹配和正则表达式匹配类名，可以指定类加载器和输出目录。
 *
 * <p>主要功能：</p>
 * <ul>
 * <li>按类名模式匹配类，支持通配符(*)和正则表达式</li>
 * <li>按类加载器过滤类</li>
 * <li>指定类的字节码输出目录</li>
 * <li>限制dump类的数量，避免一次性导出过多类</li>
 * <li>自动生成以类名为名的.class文件</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>
 * dump java.lang.String                          # 导出String类的字节码
 * dump -d /tmp/output java.lang.String          # 导出到指定目录
 * dump *StringUtils                             # 使用通配符匹配
 * dump -E org\\.apache\\.commons\\..*            # 使用正则表达式匹配
 * </pre>
 *
 * <p>限制说明：</p>
 * 默认限制最多导出50个类，可以通过--limit参数调整。
 * 当匹配的类数量超过限制时，会列出所有匹配的类但不导出。
 *
 * Dump class byte array
 */
@Name("dump")
@Summary("Dump class byte array from JVM")
@Description(Constants.EXAMPLE +
        "  dump java.lang.String\n" +
        "  dump -d /tmp/output java.lang.String\n" +
        "  dump org/apache/commons/lang/StringUtils\n" +
        "  dump *StringUtils\n" +
        "  dump -E org\\\\.apache\\\\.commons\\\\.lang\\\\.StringUtils\n" +
        Constants.WIKI + Constants.WIKI_HOME + "dump")
public class DumpClassCommand extends AnnotatedCommand {
    /**
     * 日志记录器，用于记录命令执行过程中的信息和错误
     */
    private static final Logger logger = LoggerFactory.getLogger(DumpClassCommand.class);

    /**
     * 类名模式匹配字符串
     * 支持通配符(*)或正则表达式匹配，取决于isRegEx标志
     */
    private String classPattern;

    /**
     * 类加载器的哈希码（十六进制字符串）
     * 用于精确定位特定类加载器加载的类
     */
    private String code = null;

    /**
     * 类加载器的类名
     * 通过类加载器的类名来筛选类，与code参数二选一
     */
    private String classLoaderClass;

    /**
     * 是否使用正则表达式匹配类名
     * false表示使用通配符匹配（默认）
     */
    private boolean isRegEx = false;

    /**
     * 导出字节码的目标目录
     * 如果为null，则使用默认目录
     */
    private String directory;

    /**
     * 允许导出的类的数量限制
     * 超过此数量时将不会执行导出操作，避免性能问题
     * 默认值为50
     */
    private int limit;

    /**
     * 设置类名模式
     *
     * @param classPattern 类名模式，支持'.'或'/'作为分隔符
     *                      可以包含通配符(*)或正则表达式
     */
    @Argument(index = 0, argName = "class-pattern")
    @Description("Class name pattern, use either '.' or '/' as separator")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置类加载器的哈希码
     *
     * @param code 类加载器的哈希码（十六进制字符串）
     *             用于精确定位由特定类加载器加载的类
     */
    @Option(shortName = "c", longName = "code")
    @Description("The hash code of the special class's classLoader")
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 设置类加载器的类名
     *
     * @param classLoaderClass 类加载器的完整类名
     *                        系统会查找匹配此类名的所有类加载器
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置是否使用正则表达式
     *
     * @param regEx true表示使用正则表达式匹配，false表示使用通配符匹配（默认）
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置导出目录
     *
     * @param directory 类字节码文件导出的目标目录
     *                  如果目录不存在或不是目录，命令将失败
     */
    @Option(shortName = "d", longName = "directory")
    @Description("Sets the destination directory for class files")
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * 设置导出类数量限制
     *
     * @param limit 允许导出的类的最大数量
     *              当匹配的类数量超过此值时，将不会执行导出
     */
    @Option(shortName = "l", longName = "limit")
    @Description("The limit of dump classes size, default value is 50")
    @DefaultValue("50")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * 处理dump命令的执行
     *
     * 这是命令的主入口方法，负责：
     * 1. 验证输出目录的有效性
     * 2. 根据classLoaderClass参数查找类加载器
     * 3. 搜索匹配的类
     * 4. 根据匹配结果执行不同的处理逻辑
     *
     * @param process 命令进程对象，用于获取上下文和返回结果
     */
    @Override
    public void process(CommandProcess process) {
        try {
            // 验证输出目录：如果指定了目录，检查它是否存在且为目录
            if (directory != null && !FileUtils.isDirectoryOrNotExist(directory)) {
                process.end(-1, directory + " :is not a directory, please check it");
                return;
            }

            // 获取JVM Instrumentation实例，用于操作已加载的类
            Instrumentation inst = process.session().getInstrumentation();

            // 如果未指定code但指定了classLoaderClass，需要通过类名查找类加载器
            if (code == null && classLoaderClass != null) {
                // 根据类加载器的类名查找所有匹配的类加载器
                List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);

                if (matchedClassLoaders.size() == 1) {
                    // 只找到一个类加载器，直接使用其哈希码
                    code = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                } else if (matchedClassLoaders.size() > 1) {
                    // 找到多个类加载器，需要用户明确指定
                    Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                    DumpClassModel dumpClassModel = new DumpClassModel()
                            .setClassLoaderClass(classLoaderClass)
                            .setMatchedClassLoaders(classLoaderVOList);
                    process.appendResult(dumpClassModel);
                    process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                    return;
                } else {
                    // 没有找到类加载器
                    process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                    return;
                }
            }

            // 搜索匹配的类
            Set<Class<?>> matchedClasses = SearchUtils.searchClass(inst, classPattern, isRegEx, code);

            // 用于记录影响行数的对象
            final RowAffect effect = new RowAffect();
            // 命令执行状态
            final ExitStatus status;

            // 根据匹配结果的数量执行不同的处理逻辑
            if (matchedClasses == null || matchedClasses.isEmpty()) {
                // 没有匹配的类
                status = processNoMatch(process);
            } else if (matchedClasses.size() > limit) {
                // 匹配的类数量超过限制
                status = processMatches(process, matchedClasses);
            } else {
                // 匹配的类数量在限制范围内，执行dump操作
                status = processMatch(process, effect, inst, matchedClasses);
            }

            // 记录影响的行数
            process.appendResult(new RowAffectModel(effect));
            // 结束命令处理
            CommandUtils.end(process, status);
        } catch (Throwable e){
            // 捕获所有异常，记录错误日志
            logger.error("processing error", e);
            process.end(-1, "processing error");
        }
    }

    /**
     * 命令自动补全处理
     *
     * 当用户输入不完整的命令时，提供类名自动补全功能
     *
     * @param completion 补全上下文对象，包含当前输入信息
     */
    @Override
    public void complete(Completion completion) {
        // 尝试进行类名补全
        if (!CompletionUtils.completeClassName(completion)) {
            // 如果类名补全失败，使用默认的补全逻辑
            super.complete(completion);
        }
    }

    /**
     * 处理类匹配成功的情况
     *
     * 当匹配的类数量在限制范围内时，调用此方法执行实际的dump操作
     *
     * @param process 命令进程对象
     * @param effect 行数影响统计对象
     * @param inst JVM Instrumentation实例
     * @param matchedClasses 匹配的类的集合
     * @return 命令执行状态，成功返回success，失败返回failure
     */
    private ExitStatus processMatch(CommandProcess process, RowAffect effect, Instrumentation inst, Set<Class<?>> matchedClasses) {
        try {
            // 执行实际的dump操作，获取类到文件的映射
            Map<Class<?>, File> classFiles = dump(inst, matchedClasses);

            // 创建dump结果列表
            List<DumpClassVO> dumpedClasses = new ArrayList<DumpClassVO>(classFiles.size());

            // 遍历dump结果，构建返回对象
            for (Map.Entry<Class<?>, File> entry : classFiles.entrySet()) {
                Class<?> clazz = entry.getKey();
                File file = entry.getValue();

                // 创建dump类视图对象
                DumpClassVO dumpClassVO = new DumpClassVO();
                // 设置文件路径（规范化路径）
                dumpClassVO.setLocation(file.getCanonicalPath());
                // 填充类的详细信息
                ClassUtils.fillSimpleClassVO(clazz, dumpClassVO);

                dumpedClasses.add(dumpClassVO);
            }

            // 将dump结果添加到命令输出
            process.appendResult(new DumpClassModel().setDumpedClasses(dumpedClasses));

            // 记录影响的类数量
            effect.rCnt(classFiles.keySet().size());
            return ExitStatus.success();
        } catch (Throwable t) {
            // 记录dump失败的错误日志
            logger.error("dump: fail to dump classes: " + matchedClasses, t);
            return ExitStatus.failure(-1, "dump: fail to dump classes: " + matchedClasses);
        }
    }

    /**
     * 处理匹配类数量超过限制的情况
     *
     * 当匹配的类数量超过limit时，不执行dump操作，而是列出所有匹配的类
     * 提示用户使用-c选项指定类加载器或使用--limit选项调整限制
     *
     * @param process 命令进程对象
     * @param matchedClasses 匹配的类的集合
     * @return 命令执行状态，总是返回failure
     */
    private ExitStatus processMatches(CommandProcess process, Set<Class<?>> matchedClasses) {
        // 构建错误消息
        String msg = String.format(
                "Found more than %d class for: %s, Please Try to specify the classloader with the -c option, or try to use --limit option.",
                limit, classPattern);

        // 添加消息到输出
        process.appendResult(new MessageModel(msg));

        // 创建匹配类的视图对象列表
        List<ClassVO> classVOs = ClassUtils.createClassVOList(matchedClasses);
        // 将匹配类信息添加到输出
        process.appendResult(new DumpClassModel().setMatchedClasses(classVOs));

        return ExitStatus.failure(-1, msg);
    }

    /**
     * 处理没有匹配类的情况
     *
     * 当没有任何类匹配指定的模式时调用此方法
     *
     * @param process 命令进程对象
     * @return 命令执行状态，总是返回failure
     */
    private ExitStatus processNoMatch(CommandProcess process) {
        return ExitStatus.failure(-1, "No class found for: " + classPattern);
    }

    /**
     * 执行类字节码导出操作
     *
     * 使用ClassDumpTransformer将类的字节码导出到文件系统
     *
     * @param inst JVM Instrumentation实例
     * @param classes 要导出的类的集合
     * @return 类到导出文件的映射关系
     * @throws UnmodifiableClassException 如果类无法被转换时抛出
     */
    private Map<Class<?>, File> dump(Instrumentation inst, Set<Class<?>> classes) throws UnmodifiableClassException {
        // 类转换器，用于捕获类的字节码
        ClassDumpTransformer transformer = null;

        // 根据是否指定了输出目录创建不同的转换器
        if (directory != null) {
            // 使用指定的输出目录
            transformer = new ClassDumpTransformer(classes, new File(directory));
        } else {
            // 使用默认输出目录
            transformer = new ClassDumpTransformer(classes);
        }

        // 使用Instrumentation重新转换类，触发ClassDumpTransformer捕获字节码
        InstrumentationUtils.retransformClasses(inst, transformer, classes);

        // 返回dump结果：类到文件的映射
        return transformer.getDumpResult();
    }
}
