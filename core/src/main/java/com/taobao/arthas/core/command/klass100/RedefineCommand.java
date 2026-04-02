package com.taobao.arthas.core.command.klass100;

// 文件操作相关导入
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
// Java Instrumentation API 相关导入
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
// 集合类导入
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

// ASM 字节码操作库导入
import com.alibaba.deps.org.objectweb.asm.ClassReader;
// 日志相关导入
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
// Arthas 核心类导入
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.RedefineModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
// CLI 注解导入
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Redefite 命令 - 用于热更新已加载的类
 * 该命令允许用户通过提供新的 .class 文件来重新定义 JVM 中已加载的类
 * 这是一种热部署机制，可以在不重启应用的情况下更新类代码
 *
 * 功能说明：
 * 1. 支持通过外部 .class 文件重新定义类
 * 2. 支持指定特定的类加载器进行重定义
 * 3. 支持同时重定义多个类（包括内部类）
 * 4. 文件大小限制为 10MB
 *
 * 注意事项：
 * - 重定义的类必须保持相同的方法签名和字段
 * - 不能修改或添加/删除字段和方法
 * - 仅修改方法体内容
 *
 * @author hengyunabc 2018-07-13
 * @see java.lang.instrument.Instrumentation#redefineClasses(ClassDefinition...)
 */
@Name("redefine")
@Summary("Redefine classes. @see Instrumentation#redefineClasses(ClassDefinition...)")
@Description(Constants.EXAMPLE +
                "  redefine /tmp/Test.class\n" +
                "  redefine -c 327a647b /tmp/Test.class /tmp/Test\\$Inner.class \n" +
                "  redefine --classLoaderClass 'sun.misc.Launcher$AppClassLoader' /tmp/Test.class \n" +
                Constants.WIKI + Constants.WIKI_HOME + "redefine")
public class RedefineCommand extends AnnotatedCommand {
    // 日志记录器，用于记录命令执行过程中的各种信息
    private static final Logger logger = LoggerFactory.getLogger(RedefineCommand.class);
    // 最大文件大小限制：10MB，防止加载过大的 class 文件导致内存问题
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 类加载器的哈希码，用于指定特定的类加载器
    private String hashCode;
    // 类加载器的类名，用于通过类名查找特定的类加载器
    private String classLoaderClass;

    // 要重定义的 .class 文件路径列表
    private List<String> paths;

    /**
     * 设置类加载器的哈希码
     * 用于指定在哪个类加载器中重定义类
     *
     * @param hashCode 类加载器的十六进制哈希码字符串
     */
    @Option(shortName = "c", longName = "classloader")
    @Description("classLoader hashcode")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 设置类加载器的类名
     * 当存在多个类加载器时，可以通过类名来定位特定的类加载器
     *
     * @param classLoaderClass 类加载器的完整类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置要重定义的 .class 文件路径列表
     * 支持同时指定多个 .class 文件
     *
     * @param paths .class 文件的绝对路径列表
     */
    @Argument(argName = "classfilePaths", index = 0)
    @Description(".class file paths")
    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    /**
     * 处理 redefine 命令的核心方法
     * 该方法执行以下步骤：
     * 1. 验证输入的 .class 文件路径是否有效
     * 2. 读取所有 .class 文件的字节码
     * 3. 在 JVM 中查找匹配的已加载类
     * 4. 执行类的重定义操作
     *
     * @param process 命令处理进程对象，用于获取 Instrumentation 实例和返回结果
     */
    @Override
    public void process(CommandProcess process) {
        // 创建命令结果模型，用于存储和返回重定义的结果信息
        RedefineModel redefineModel = new RedefineModel();
        // 获取 Java Instrumentation 实例，用于执行类的重定义操作
        Instrumentation inst = process.session().getInstrumentation();

        // 第一步：验证所有输入的文件路径
        for (String path : paths) {
            File file = new File(path);
            // 检查文件是否存在
            if (!file.exists()) {
                process.end(-1, "file does not exist, path:" + path);
                return;
            }
            // 检查是否为普通文件（排除目录等）
            if (!file.isFile()) {
                process.end(-1, "not a normal file, path:" + path);
                return;
            }
            // 检查文件大小是否超过限制
            if (file.length() >= MAX_FILE_SIZE) {
                process.end(-1, "file size: " + file.length() + " >= " + MAX_FILE_SIZE + ", path: " + path);
                return;
            }
        }

        // 第二步：读取所有 .class 文件的字节码内容
        // 使用 Map 存储类名到字节码的映射，键为类全限定名，值为类的字节码
        Map<String, byte[]> bytesMap = new HashMap<String, byte[]>();
        for (String path : paths) {
            RandomAccessFile f = null;
            try {
                // 以只读模式打开文件
                f = new RandomAccessFile(path, "r");
                // 创建字节数组，大小为文件长度
                final byte[] bytes = new byte[(int) f.length()];
                // 将文件内容完全读入字节数组
                f.readFully(bytes);

                // 从字节码中读取类的全限定名
                final String clazzName = readClassName(bytes);

                // 将类名和字节码存入 Map
                bytesMap.put(clazzName, bytes);

            } catch (Exception e) {
                // 读取文件失败，记录警告并终止命令
                logger.warn("load class file failed: "+path, e);
                process.end(-1, "load class file failed: " +path+", error: " + e);
                return;
            } finally {
                // 确保文件句柄被正确关闭
                if (f != null) {
                    try {
                        f.close();
                    } catch (IOException e) {
                        // 忽略关闭时的异常
                    }
                }
            }
        }

        // 第三步：检查是否有重复的类名
        // 如果 Map 的大小与路径数量不一致，说明有重复的类名
        if (bytesMap.size() != paths.size()) {
            process.end(-1, "paths may contains same class name!");
            return;
        }

        // 第四步：在 JVM 中查找匹配的已加载类，并准备重定义
        // 创建 ClassDefinition 列表，用于存储要重定义的类定义
        List<ClassDefinition> definitions = new ArrayList<ClassDefinition>();
        // 遍历 JVM 中所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 如果当前类的名称在字节码 Map 中存在
            if (bytesMap.containsKey(clazz.getName())) {

                // 处理类加载器的匹配逻辑
                // 如果没有指定 hashCode 但指定了 classLoaderClass，需要通过类名查找类加载器
                if (hashCode == null && classLoaderClass != null) {
                    // 根据类加载器的类名查找匹配的类加载器实例
                    List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst, classLoaderClass);
                    if (matchedClassLoaders.size() == 1) {
                        // 找到唯一的匹配类加载器，使用其哈希码
                        hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                    } else if (matchedClassLoaders.size() > 1) {
                        // 找到多个匹配的类加载器，需要用户明确指定
                        Collection<ClassLoaderVO> classLoaderVOList = ClassUtils.createClassLoaderVOList(matchedClassLoaders);
                        RedefineModel classredefineModel = new RedefineModel()
                                .setClassLoaderClass(classLoaderClass)
                                .setMatchedClassLoaders(classLoaderVOList);
                        process.appendResult(classredefineModel);
                        process.end(-1, "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                        return;
                    } else {
                        // 没有找到匹配的类加载器
                        process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                        return;
                    }
                }

                // 检查类加载器是否匹配
                ClassLoader classLoader = clazz.getClassLoader();
                // 如果指定了 hashCode，且当前类的类加载器哈希码不匹配，则跳过
                if (classLoader != null && hashCode != null && !Integer.toHexString(classLoader.hashCode()).equals(hashCode)) {
                    continue;
                }
                // 创建 ClassDefinition 对象并添加到列表
                definitions.add(new ClassDefinition(clazz, bytesMap.get(clazz.getName())));
                // 记录要重定义的类名到结果模型
                redefineModel.addRedefineClass(clazz.getName());
                logger.info("Try redefine class name: {}, ClassLoader: {}", clazz.getName(), clazz.getClassLoader());
            }
        }

        // 第五步：执行类的重定义操作
        try {
            // 如果没有找到任何匹配的类，返回错误
            if (definitions.isEmpty()) {
                process.end(-1, "These classes are not found in the JVM and may not be loaded: " + bytesMap.keySet());
                return;
            }
            // 调用 Instrumentation API 执行类的重定义
            inst.redefineClasses(definitions.toArray(new ClassDefinition[0]));
            // 将结果模型添加到进程输出中
            process.appendResult(redefineModel);
            // 正常结束命令处理
            process.end();
        } catch (Throwable e) {
            // 重定义失败，记录错误并返回错误信息
            String message = "redefine error! " + e.toString();
            logger.error(message, e);
            process.end(-1, message);
        }

    }

    /**
     * 从类的字节码中读取类的全限定名
     * 使用 ASM 库的 ClassReader 来解析字节码并提取类名
     *
     * @param bytes 类的字节码数组
     * @return 类的全限定名，使用 '.' 作为包分隔符
     */
    private static String readClassName(final byte[] bytes) {
        // 使用 ASM 的 ClassReader 读取类名
        // getClassName() 返回的类名使用 '/' 作为包分隔符，需要替换为 '.'
        return new ClassReader(bytes).getClassName().replace("/", ".");
    }

    /**
     * 实现命令行的自动补全功能
     * 当用户输入命令时，可以自动补全文件路径
     *
     * @param completion 补全上下文对象，包含当前输入信息
     */
    @Override
    public void complete(Completion completion) {
        // 尝试进行文件路径补全
        if (!CompletionUtils.completeFilePath(completion)) {
            // 如果文件路径补全失败，使用父类的默认补全逻辑
            super.complete(completion);
        }
    }
}
