package com.taobao.arthas.core.command.monitor200;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.instrument.Instrumentation;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.VmToolUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.Express;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.VmToolModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.cli.OptionCompleteHandler;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import arthas.VmTool;

/**
 * VmTool命令类
 * 提供了一系列JVM工具方法，包括获取实例、强制GC、堆分析、引用分析、线程中断等操作
 *
 * @author hengyunabc 2021-04-27
 * @author ZhangZiCheng 2021-04-29
 */
//@formatter:off
// 命令名称定义
@Name("vmtool")
// 命令摘要说明
@Summary("jvm tool")
// 命令详细描述，包含示例和文档链接
@Description(Constants.EXAMPLE
        + "  vmtool --action getInstances --className demo.MathGame\n"
        + "  vmtool --action getInstances --className demo.MathGame --express 'instances.length'\n"
        + "  vmtool --action getInstances --className demo.MathGame --express 'instances[0]'\n"
        + "  vmtool --action getInstances --className demo.MathGame -x 2\n"
        + "  vmtool --action getInstances --className java.lang.String --limit 10\n"
        + "  vmtool --action getInstances --classLoaderClass org.springframework.boot.loader.LaunchedURLClassLoader --className org.springframework.context.ApplicationContext\n"
        + "  vmtool --action forceGc\n"
        + "  vmtool --action heapAnalyze --classNum 20 --objectNum 20\n"
        + "  vmtool --action referenceAnalyze --className java.lang.String --objectNum 20 --backtraceNum 2\n"
        + "  vmtool --action interruptThread -t 1\n"
        + "  vmtool --action mallocTrim\n"
        + "  vmtool --action mallocStats\n"
        + Constants.WIKI + Constants.WIKI_HOME + "vmtool")
//@formatter:on
public class VmToolCommand extends AnnotatedCommand {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(VmToolCommand.class);

    // 要执行的操作类型（获取实例、强制GC、堆分析等）
    private VmToolAction action;
    // 目标类名
    private String className;
    // 表达式，用于对结果进行过滤或转换
    private String express;
    // 线程ID，用于中断指定线程
    private int threadId;
    // 类加载器的哈希码
    private String hashCode = null;
    // 类加载器的类名
    private String classLoaderClass;
    /**
     * 对象展开层级，默认值为1
     * 用于控制对象属性的显示深度
     */
    private int expand;

    /**
     * 获取实例时的数量限制，默认值为10
     * 设置为-1表示不限制
     */
    private int limit;

    /**
     * 堆分析时显示的类数量，默认值为20
     */
    private int classNum = 20;

    /**
     * 堆分析时显示的对象数量，默认值为20
     */
    private int objectNum = 20;

    /**
     * 引用分析时回溯的步数，默认值为2
     */
    private int backtraceNum = 2;

    // VmTool本地库的路径
    private String libPath;
    // 默认的本地库路径
    private static String defaultLibPath;
    // VmTool单例实例
    private static VmTool vmTool = null;

    // 静态初始化块，在类加载时执行
    // 用于检测并设置默认的VmTool本地库路径
    static {
        // 检测当前平台的库名称（如libArthasVmTool.so、libArthasVmTool.dylib等）
        String libName = VmToolUtils.detectLibName();
        if (libName != null) {
            // 获取当前类的保护域代码源，即类的加载位置
            CodeSource codeSource = VmToolCommand.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                try {
                    // 获取boot jar文件的路径
                    File bootJarPath = new File(codeSource.getLocation().toURI().getSchemeSpecificPart());
                    // 构建本地库文件的完整路径（在jar文件同目录下的lib子目录）
                    File soFile = new File(bootJarPath.getParentFile(), "lib" + File.separator + libName);
                    // 如果本地库文件存在，则设置为默认路径
                    if (soFile.exists()) {
                        defaultLibPath = soFile.getAbsolutePath();
                    }
                } catch (Throwable e) {
                    // 如果获取路径失败，记录错误日志
                    logger.error("can not find VmTool so", e);
                }
            }
        }

    }

    // 设置要执行的操作类型（必填参数）
    @Option(shortName = "a", longName = "action", required = true)
    @Description("Action to execute")
    public void setAction(VmToolAction action) {
        this.action = action;
    }

    // 设置目标类名
    @Option(longName = "className")
    @Description("The class name")
    public void setClassName(String className) {
        this.className = className;
    }

    // 设置对象展开层级（默认值为1）
    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    @DefaultValue("1")
    public void setExpand(int expand) {
        this.expand = expand;
    }

    // 设置类加载器的哈希码（用于指定特定的类加载器）
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    // 设置类加载器的类名（用于查找特定的类加载器）
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    // 设置获取实例时的数量限制（默认值为10，-1表示不限制）
    @Option(shortName = "l", longName = "limit")
    @Description("Set the limit value of the getInstances action, default value is 10, set to -1 is unlimited")
    @DefaultValue("10")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    // 设置堆分析时显示的类数量
    @Option(longName = "classNum", required = false)
    @Description("The number of classes to be shown.")
    public void setClassNum(int classNum) {
        this.classNum = classNum;
    }

    // 设置堆分析时显示的对象数量
    @Option(longName = "objectNum", required = false)
    @Description("The number of objects to be shown.")
    public void setObjectNum(int objectNum) {
        this.objectNum = objectNum;
    }

    // 设置引用分析时回溯的步数
    @Option(longName = "backtraceNum", required = false)
    @Description("The steps of backtrace by reference.")
    public void setBacktraceNum(int backtraceNum) {
        this.backtraceNum = backtraceNum;
    }

    // 设置VmTool本地库的路径
    @Option(longName = "libPath")
    @Description("The specify lib path.")
    public void setLibPath(String path) {
        libPath = path;
    }

    // 设置OGNL表达式（默认值为"instances"）
    @Option(longName = "express", required = false)
    @Description("The ognl expression, default value is `instances`.")
    public void setExpress(String express) {
        this.express = express;
    }

    // 设置要中断的线程ID
    @Option(shortName = "t", longName = "threadId", required = false)
    @Description("The id of the thread to be interrupted")
    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    /**
     * VmTool操作枚举
     * 定义了所有支持的操作类型
     */
    public enum VmToolAction {
        // 获取类的所有实例
        getInstances,
        // 强制执行垃圾回收
        forceGc,
        // 堆内存分析
        heapAnalyze,
        // 对象引用分析
        referenceAnalyze,
        // 中断指定线程
        interruptThread,
        // 释放malloc未使用的内存
        mallocTrim,
        // 显示malloc内存统计信息
        mallocStats
    }

    /**
     * 处理命令执行的核心方法
     * 根据不同的action类型执行相应的操作
     *
     * @param process 命令进程对象，用于输入输出和状态管理
     */
    @Override
    public void process(final CommandProcess process) {
        try {
            // 获取Java Instrumentation实例，用于 JVM级别的操作
            Instrumentation inst = process.session().getInstrumentation();

            // 处理获取实例和引用分析的操作，这两个操作需要指定类名
            if (VmToolAction.getInstances.equals(action) || VmToolAction.referenceAnalyze.equals(action)) {
                // 检查是否指定了类名，这是必填参数
                if (className == null) {
                    process.end(-1, "The className option cannot be empty!");
                    return;
                }

                // 确定要使用的类加载器
                ClassLoader classLoader = null;

                // 优先使用指定的类加载器哈希码
                if (hashCode != null) {
                    classLoader = ClassLoaderUtils.getClassLoader(inst, hashCode);
                    if (classLoader == null) {
                        process.end(-1, "Can not find classloader with hashCode: " + hashCode + ".");
                        return;
                    }
                }
                // 其次使用类加载器的类名来查找
                else if ( classLoaderClass != null) {
                    // 根据类加载器的类名查找匹配的类加载器
                    List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst,
                            classLoaderClass);
                    // 如果只找到一个匹配的类加载器，直接使用
                    if (matchedClassLoaders.size() == 1) {
                        classLoader = matchedClassLoaders.get(0);
                        hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                    }
                    // 如果找到多个匹配的类加载器，提示用户选择
                    else if (matchedClassLoaders.size() > 1) {
                        Collection<ClassLoaderVO> classLoaderVOList = ClassUtils
                                .createClassLoaderVOList(matchedClassLoaders);

                        VmToolModel vmToolModel = new VmToolModel().setClassLoaderClass(classLoaderClass)
                                .setMatchedClassLoaders(classLoaderVOList);
                        process.appendResult(vmToolModel);
                        process.end(-1,
                                "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                        return;
                    }
                    // 如果没有找到匹配的类加载器，返回错误
                    else {
                        process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                        return;
                    }
                }
                // 如果没有指定类加载器，使用系统类加载器
                else {
                    classLoader = ClassLoader.getSystemClassLoader();
                }

                // 根据类名和类加载器搜索匹配的类
                List<Class<?>> matchedClasses = new ArrayList<Class<?>>(
                        SearchUtils.searchClassOnly(inst, className, false, hashCode));
                int matchedClassSize = matchedClasses.size();

                // 如果没有找到匹配的类，返回错误
                if (matchedClassSize == 0) {
                    process.end(-1, "Can not find class by class name: " + className + ".");
                    return;
                }
                // 如果找到多个匹配的类，提示用户指定类加载器
                else if (matchedClassSize > 1) {
                    process.end(-1, "Found more than one class: " + matchedClasses + ", please specify classloader with '-c <classloader hash>'");
                    return;
                }
                // 找到唯一的匹配类，执行相应操作
                else {
                    // 执行获取实例操作
                    if (VmToolAction.getInstances.equals(action)) {
                        // 使用VmTool获取指定类的所有实例（受limit限制）
                        Object[] instances = vmToolInstance().getInstances(matchedClasses.get(0), limit);
                        Object value = instances;

                        // 如果指定了表达式，则对结果进行表达式计算
                        if (express != null) {
                            // 创建非池化的表达式执行器
                            Express unpooledExpress = ExpressFactory.unpooledExpress(classLoader);
                            try {
                                // 绑定实例数组并执行表达式
                                value = unpooledExpress.bind(new InstancesWrapper(instances)).get(express);
                            } catch (ExpressException e) {
                                logger.warn("ognl: failed execute express: " + express, e);
                                process.end(-1, "Failed to execute ognl, exception message: " + e.getMessage()
                                        + ", please check $HOME/logs/arthas/arthas.log for more details. ");
                            }
                        }

                        // 构建结果模型并输出
                        VmToolModel vmToolModel = new VmToolModel().setValue(new ObjectVO(value, expand));
                        process.appendResult(vmToolModel);
                        process.end();
                    }
                    // 执行引用分析操作
                    else {
                        // 分析指定类的对象引用关系
                        String result = vmToolInstance().referenceAnalyze(matchedClasses.get(0), objectNum, backtraceNum);
                        process.write(result);
                        process.end();
                    }
                }
            }
            // 执行强制GC操作
            else if (VmToolAction.forceGc.equals(action)) {
                // 强制JVM执行垃圾回收
                vmToolInstance().forceGc();
                process.write("\n");
                process.end();
                return;
            }
            // 执行堆分析操作
            else if (VmToolAction.heapAnalyze.equals(action)) {
                // 分析堆内存使用情况，包括类的实例数量和内存占用
                String result = vmToolInstance().heapAnalyze(classNum, objectNum);
                process.write(result);
                process.end();
                return;
            }
            // 执行中断线程操作
            else if (VmToolAction.interruptThread.equals(action)) {
                // 中断指定ID的线程
                vmToolInstance().interruptSpecialThread(threadId);
                process.write("\n");
                process.end();

                return;
            }
            // 执行malloc内存释放操作
            else if (VmToolAction.mallocTrim.equals(action)) {
                // 尝试释放malloc分配但未使用的内存回操作系统
                int result = vmToolInstance().mallocTrim();
                process.write("\n");
                // 根据返回值判断操作结果：1-成功，0-失败，-1-不支持
                process.end(result == 1 ? 0 : -1, "mallocTrim result: " +
                    (result == 1 ? "true" : (result == 0 ? "false" : "not supported")));
                return;
            }
            // 执行malloc统计操作
            else if (VmToolAction.mallocStats.equals(action)) {
                // 显示malloc内存分配的统计信息
                boolean result = vmToolInstance().mallocStats();
                process.write("\n");
                process.end(result ? 0 : -1, "mallocStats result: " +
                    (result ? "true" : "not supported"));
                return;
            }

            // 结束命令处理
            process.end();
        } catch (Throwable e) {
            // 捕获所有异常并记录日志
            logger.error("vmtool error", e);
            process.end(1, "vmtool error: " + e.getMessage());
        }
    }

    /**
     * 实例包装类
     * 用于将实例数组包装成对象，以便在OGNL表达式中访问
     */
    static class InstancesWrapper {
        // 被包装的实例对象
        Object instances;

        /**
         * 构造函数
         * @param instances 要包装的实例对象
         */
        public InstancesWrapper(Object instances) {
            this.instances = instances;
        }

        /**
         * 获取被包装的实例对象
         * @return 实例对象
         */
        public Object getInstances() {
            return instances;
        }

        /**
         * 设置被包装的实例对象
         * @param instances 要设置的实例对象
         */
        public void setInstances(Object instances) {
            this.instances = instances;
        }
    }

    /**
     * 获取VmTool实例
     * 使用单例模式，如果实例不存在则创建新实例
     * 为了避免多次attach时出现"Native Library already loaded in another classloader"错误，
     * 会将本地库文件复制到临时文件中再加载
     *
     * @return VmTool实例
     */
    private VmTool vmToolInstance() {
        // 如果VmTool实例已存在，直接返回
        if (vmTool != null) {
            return vmTool;
        } else {
            // 如果没有指定库路径，使用默认路径
            if (libPath == null) {
                libPath = defaultLibPath;
            }

            // 尝试把lib文件复制到临时文件里，避免多次attach时出现 Native Library already loaded in another classloader
            FileOutputStream tmpLibOutputStream = null;
            FileInputStream libInputStream = null;
            try {
                // 创建临时文件
                File tmpLibFile = File.createTempFile(VmTool.JNI_LIBRARY_NAME, null);
                tmpLibOutputStream = new FileOutputStream(tmpLibFile);
                libInputStream = new FileInputStream(libPath);

                // 将原始库文件复制到临时文件
                IOUtils.copy(libInputStream, tmpLibOutputStream);
                libPath = tmpLibFile.getAbsolutePath();
                logger.debug("copy {} to {}", libPath, tmpLibFile);
            } catch (Throwable e) {
                // 如果复制失败，记录错误日志，但仍尝试使用原始路径加载
                logger.error("try to copy lib error! libPath: {}", libPath, e);
            } finally {
                // 关闭所有流
                IOUtils.close(libInputStream);
                IOUtils.close(tmpLibOutputStream);
            }

            // 创建VmTool实例
            vmTool = VmTool.getInstance(libPath);
        }
        return vmTool;
    }

    /**
     * 获取所有可用的操作类型
     * 用于命令行自动补全功能
     *
     * @return 包含所有操作名称的集合
     */
    private Set<String> actions() {
        Set<String> values = new HashSet<String>();
        // 遍历VmToolAction枚举，收集所有操作名称
        for (VmToolAction action : VmToolAction.values()) {
            values.add(action.toString());
        }
        return values;
    }

    /**
     * 命令行自动补全功能
     * 根据用户输入的部分命令，提供可能的补全选项
     *
     * @param completion 补全上下文对象，包含当前输入信息和补全结果
     */
    @Override
    public void complete(Completion completion) {
        // 创建选项处理器列表
        List<OptionCompleteHandler> handlers = new ArrayList<OptionCompleteHandler>();

        // 添加action选项的补全处理器
        handlers.add(new OptionCompleteHandler() {

            @Override
            public boolean matchName(String token) {
                // 匹配 -a 或 --action 参数
                return "-a".equals(token) || "--action".equals(token);
            }

            @Override
            public boolean complete(Completion completion) {
                // 使用所有可用的action进行补全
                return CompletionUtils.complete(completion, actions());
            }

        });

        // 添加className选项的补全处理器
        handlers.add(new OptionCompleteHandler() {
            @Override
            public boolean matchName(String token) {
                // 匹配 --className 参数
                return "--className".equals(token);
            }

            @Override
            public boolean complete(Completion completion) {
                // 使用类名补全功能
                return CompletionUtils.completeClassName(completion);
            }
        });

        // 尝试使用定义的处理器进行补全
        if (CompletionUtils.completeOptions(completion, handlers)) {
            return;
        }

        // 如果自定义处理器无法处理，调用父类的补全方法
        super.complete(completion);
    }

}
