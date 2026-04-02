package com.taobao.arthas.core.command.klass100;

// 文件操作相关导入
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
// Java Instrumentation API 相关导入
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
// 集合类导入
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// 日志相关导入
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
// ASM 字节码操作库导入
import com.alibaba.deps.org.objectweb.asm.ClassReader;
// Arthas 核心类导入
import com.taobao.arthas.core.advisor.TransformerManager;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.command.model.RetransformModel;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.SearchUtils;
// CLI 注解导入
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Retransform 命令 - 用于重新转换已加载的类
 *
 * 该命令提供了比 redefine 更强大的类转换功能，主要特点：
 * 1. 支持通过外部 .class 文件重新转换类
 * 2. 可以通过类模式批量触发类的重新转换
 * 3. 支持列出、删除已注册的转换条目
 * 4. 使用 ClassFileTransformer 实现类转换，转换会持久化
 * 5. 每次类重定义时都会触发转换器，提供持续的转换能力
 *
 * 与 redefine 的区别：
 * - redefine 只是一次性的类替换，不会持久化
 * - retransform 会注册转换器，每次类加载时都会应用转换
 * - retransform 适用于需要持续监控和修改类的场景
 *
 * 使用场景：
 * 1. 添加日志、性能监控等非侵入式增强
 * 2. 临时修复生产环境的 bug
 * 3. 动态添加调试代码
 *
 * @author hengyunabc 2021-01-05
 * @see java.lang.instrument.Instrumentation#retransformClasses(Class...)
 */
@Name("retransform")
@Summary("Retransform classes. @see Instrumentation#retransformClasses(Class...)")
@Description(Constants.EXAMPLE + "  retransform /tmp/Test.class\n"
        + "  retransform -l \n"
        + "  retransform -d 1                    # delete retransform entry\n"
        + "  retransform --deleteAll             # delete all retransform entries\n"
        + "  retransform --classPattern demo.*   # triger retransform classes\n"
        + "  retransform -c 327a647b /tmp/Test.class /tmp/Test\\$Inner.class \n"
        + "  retransform --classLoaderClass 'sun.misc.Launcher$AppClassLoader' /tmp/Test.class\n"
        + Constants.WIKI + Constants.WIKI_HOME
        + "retransform")
public class RetransformCommand extends AnnotatedCommand {
    // 日志记录器，用于记录命令执行过程中的各种信息
    private static final Logger logger = LoggerFactory.getLogger(RetransformCommand.class);
    // 最大文件大小限制：10MB，防止加载过大的 class 文件导致内存问题
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 存储所有已注册的 retransform 条目列表
    // 使用 volatile 确保多线程环境下的可见性
    private static volatile List<RetransformEntry> retransformEntries = new ArrayList<RetransformEntry>();
    // 类文件转换器，用于在类加载/重加载时转换类的字节码
    // 使用 volatile 确保多线程环境下的可见性
    private static volatile ClassFileTransformer transformer = null;

    // 类加载器的哈希码，用于指定特定的类加载器
    private String hashCode;
    // 类加载器的类名，用于通过类名查找特定的类加载器
    private String classLoaderClass;

    // 要转换的 .class 文件路径列表
    private List<String> paths;

    // 是否列出所有已注册的 retransform 条目
    private boolean list;

    // 要删除的 retransform 条目 ID，-1 表示不删除
    private int delete = -1;

    // 是否删除所有 retransform 条目
    private boolean deleteAll;

    // 类名模式，用于批量触发类的重新转换
    private String classPattern;

    // 转换类的数量限制，默认值为 50
    private int limit;

    /**
     * 设置是否列出所有已注册的 retransform 条目
     *
     * @param list true 表示列出所有条目
     */
    @Option(shortName = "l", longName = "list", flag = true)
    @Description("list all retransform entry.")
    public void setList(boolean list) {
        this.list = list;
    }

    /**
     * 设置要删除的 retransform 条目 ID
     *
     * @param delete 要删除的条目 ID
     */
    @Option(shortName = "d", longName = "delete")
    @Description("delete retransform entry by id.")
    public void setDelete(int delete) {
        this.delete = delete;
    }

    /**
     * 设置是否删除所有 retransform 条目
     *
     * @param deleteAll true 表示删除所有条目
     */
    @Option(longName = "deleteAll", flag = true)
    @Description("delete all retransform entries.")
    public void setDeleteAll(boolean deleteAll) {
        this.deleteAll = deleteAll;
    }

    /**
     * 设置类名模式，用于批量触发匹配的类进行重新转换
     *
     * @param classPattern 类名匹配模式
     */
    @Option(longName = "classPattern")
    @Description("trigger retransform matched classes by class pattern.")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置类加载器的哈希码
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
     *
     * @param classLoaderClass 类加载器的完整类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置要转换的 .class 文件路径列表
     *
     * @param paths .class 文件的绝对路径列表
     */
    @Argument(argName = "classfilePaths", index = 0, required = false)
    @Description(".class file paths")
    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    /**
     * 设置转换类的数量限制
     *
     * @param limit 最大转换数量，默认值为 50
     */
    @Option(longName = "limit")
    @Description("The limit of dump classes size, default value is 50")
    @DefaultValue("50")
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * 初始化类文件转换器
     * 使用双重检查锁定（Double-Checked Locking）模式确保只初始化一次
     * 转换器会被注册到 TransformerManager 中，用于后续的类转换操作
     */
    private static void initTransformer() {
        // 第一次检查：如果转换器已经存在，直接返回，避免进入同步块
        if (transformer != null) {
            return;
        } else {
            // 同步块，确保线程安全
            synchronized (RetransformCommand.class) {
                // 第二次检查：在同步块内再次检查，防止多个线程同时通过第一次检查
                if (transformer == null) {
                    // 创建新的类文件转换器实例
                    transformer = new RetransformClassFileTransformer();
                    // 获取 Arthas 的 TransformerManager 实例
                    TransformerManager transformerManager = ArthasBootstrap.getInstance().getTransformerManager();
                    // 将转换器注册为 retransform 类型的转换器
                    transformerManager.addRetransformer(transformer);
                }
            }
        }
    }

    /**
     * 处理 retransform 命令的核心方法
     * 根据不同的参数执行不同的操作：
     * 1. 列出所有已注册的转换条目
     * 2. 删除指定的转换条目
     * 3. 删除所有转换条目
     * 4. 按类模式批量触发转换
     * 5. 通过 .class 文件注册新的转换条目
     *
     * @param process 命令处理进程对象，用于获取 Instrumentation 实例和返回结果
     */
    @Override
    public void process(CommandProcess process) {
        // 首先初始化类文件转换器，确保转换器已经注册
        initTransformer();

        // 创建命令结果模型，用于存储和返回转换的结果信息
        RetransformModel retransformModel = new RetransformModel();
        // 获取 Java Instrumentation 实例，用于执行类的转换操作
        Instrumentation inst = process.session().getInstrumentation();

        // 处理列出所有转换条目的请求
        if (this.list) {
            // 获取所有已注册的 retransform 条目
            List<RetransformEntry> retransformEntryList = allRetransformEntries();
            // 将条目列表设置到结果模型中
            retransformModel.setRetransformEntries(retransformEntryList);
            // 添加结果并正常结束
            process.appendResult(retransformModel);
            process.end();
            return;
        // 处理删除所有转换条目的请求
        } else if (this.deleteAll) {
            // 清空所有 retransform 条目
            deleteAllRetransformEntry();
            // 添加结果并正常结束
            process.appendResult(retransformModel);
            process.end();
            return;
        // 处理删除指定 ID 的转换条目
        } else if (this.delete > 0) {
            // 删除指定 ID 的条目
            deleteRetransformEntry(this.delete);
            // 结束命令处理
            process.end();
            return;
        // 处理按类模式批量触发转换的请求
        } else if (this.classPattern != null) {
            // 根据类模式搜索匹配的类
            Set<Class<?>> searchClass = SearchUtils.searchClass(inst, classPattern, false, this.hashCode);
            // 检查是否找到匹配的类
            if (searchClass.isEmpty()) {
                process.end(-1, "These classes are not found in the JVM and may not be loaded: " + classPattern);
                return;
            }

            // 检查匹配的类数量是否超过限制
            if (searchClass.size() > limit) {
                process.end(-1, "match classes size: " + searchClass.size() + ", more than limit: " + limit
                        + ", It is recommended to use a more precise class pattern.");
            }
            try {
                // 触发所有匹配类的重新转换
                inst.retransformClasses(searchClass.toArray(new Class[0]));
                // 记录所有被转换的类名
                for (Class<?> clazz : searchClass) {
                    retransformModel.addRetransformClass(clazz.getName());
                }
                // 添加结果并正常结束
                process.appendResult(retransformModel);
                process.end();
                return;
            } catch (Throwable e) {
                // 转换失败，记录错误并返回错误信息
                String message = "retransform error! " + e.toString();
                logger.error(message, e);
                process.end(-1, message);
                return;
            }
        }

        // 处理通过 .class 文件注册新的转换条目
        // 第一步：验证所有输入的文件路径
        for (String path : paths) {
            File file = new File(path);
            // 检查文件是否存在
            if (!file.exists()) {
                process.end(-1, "file does not exist, path:" + path);
                return;
            }
            // 检查是否为普通文件
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
        // 使用 Map 存储类名到字节码的映射
        Map<String, byte[]> bytesMap = new HashMap<String, byte[]>();
        for (String path : paths) {
            RandomAccessFile f = null;
            try {
                // 以只读模式打开文件
                f = new RandomAccessFile(path, "r");
                // 创建字节数组并读取文件内容
                final byte[] bytes = new byte[(int) f.length()];
                f.readFully(bytes);

                // 从字节码中读取类的全限定名
                final String clazzName = readClassName(bytes);

                // 将类名和字节码存入 Map
                bytesMap.put(clazzName, bytes);

            } catch (Exception e) {
                // 读取文件失败，记录警告并终止命令
                logger.warn("load class file failed: " + path, e);
                process.end(-1, "load class file failed: " + path + ", error: " + e);
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
        if (bytesMap.size() != paths.size()) {
            process.end(-1, "paths may contains same class name!");
            return;
        }

        // 第四步：在 JVM 中查找匹配的已加载类，并准备创建转换条目
        // 创建转换条目列表，用于存储新创建的条目
        List<RetransformEntry> retransformEntryList = new ArrayList<RetransformEntry>();

        // 创建要转换的类列表
        List<Class<?>> classList = new ArrayList<Class<?>>();

        // 遍历 JVM 中所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 如果当前类的名称在字节码 Map 中存在
            if (bytesMap.containsKey(clazz.getName())) {

                // 处理类加载器的匹配逻辑
                if (hashCode == null && classLoaderClass != null) {
                    // 根据类加载器的类名查找匹配的类加载器实例
                    List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst,
                            classLoaderClass);
                    if (matchedClassLoaders.size() == 1) {
                        // 找到唯一的匹配类加载器，使用其哈希码
                        hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
                    } else if (matchedClassLoaders.size() > 1) {
                        // 找到多个匹配的类加载器，需要用户明确指定
                        Collection<ClassLoaderVO> classLoaderVOList = ClassUtils
                                .createClassLoaderVOList(matchedClassLoaders);
                        retransformModel.setClassLoaderClass(classLoaderClass)
                                .setMatchedClassLoaders(classLoaderVOList);
                        process.appendResult(retransformModel);
                        process.end(-1,
                                "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                        return;
                    } else {
                        // 没有找到匹配的类加载器
                        process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                        return;
                    }
                }

                // 检查类加载器是否匹配
                ClassLoader classLoader = clazz.getClassLoader();
                if (classLoader != null && hashCode != null
                        && !Integer.toHexString(classLoader.hashCode()).equals(hashCode)) {
                    continue;
                }

                // 创建新的 retransform 条目并添加到列表
                RetransformEntry retransformEntry = new RetransformEntry(clazz.getName(), bytesMap.get(clazz.getName()),
                        hashCode, classLoaderClass);
                retransformEntryList.add(retransformEntry);
                // 将类添加到要转换的类列表
                classList.add(clazz);
                // 记录要转换的类名到结果模型
                retransformModel.addRetransformClass(clazz.getName());

                logger.info("Try retransform class name: {}, ClassLoader: {}", clazz.getName(), clazz.getClassLoader());
            }
        }

        // 第五步：注册转换条目并执行类的转换
        try {
            // 如果没有找到任何匹配的类，返回错误
            if (retransformEntryList.isEmpty()) {
                process.end(-1, "These classes are not found in the JVM and may not be loaded: " + bytesMap.keySet());
                return;
            }
            // 将新的转换条目添加到全局列表中
            addRetransformEntry(retransformEntryList);

            // 触发类的重新转换
            inst.retransformClasses(classList.toArray(new Class[0]));

            // 收集所有新增条目的 ID
            List<Integer> ids  = new ArrayList<Integer>();
            for (RetransformEntry retransformEntry : retransformEntryList) {
                ids.add(retransformEntry.getId());
            }
            // 将 ID 列表设置到结果模型中
            retransformModel.setIds(ids);

            // 添加结果并正常结束
            process.appendResult(retransformModel);
            process.end();
        } catch (Throwable e) {
            // 转换失败，记录错误并返回错误信息
            String message = "retransform error! " + e.toString();
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
        return new ClassReader(bytes).getClassName().replace('/', '.');
    }

    /**
     * 实现命令行的自动补全功能
     * 支持以下补全场景：
     * 1. --classPattern 选项后补全类名
     * 2. 如果没有选项参数，补全文件路径
     * 3. 其他情况使用默认补全逻辑
     *
     * @param completion 补全上下文对象，包含当前输入信息
     */
    @Override
    public void complete(Completion completion) {
        // 获取命令行的所有 token
        List<CliToken> tokens = completion.lineTokens();

        // 如果正在补全 --classPattern 选项的值，则进行类名补全
        if (CompletionUtils.shouldCompleteOption(completion, "--classPattern")) {
            CompletionUtils.completeClassName(completion);
            return;
        }

        // 检查是否已经输入了选项参数（以 - 开头）
        for (CliToken token : tokens) {
            String tokenStr = token.value();
            if (tokenStr != null && tokenStr.startsWith("-")) {
                // 如果已经有选项参数，使用父类的默认补全逻辑
                super.complete(completion);
                return;
            }
        }

        // 最后，如果没有 - 开头的选项参数，才尝试补全文件路径
        if (!CompletionUtils.completeFilePath(completion)) {
            super.complete(completion);
        }
    }

    /**
     * Retransform 条目类
     * 每个条目存储了一个类的转换信息，包括类名、字节码、类加载器信息等
     * 这些条目会被注册到全局列表中，在类转换时使用
     */
    public static class RetransformEntry {
        // 原子计数器，用于为每个条目生成唯一 ID
        private static final AtomicInteger counter = new AtomicInteger(0);
        // 条目的唯一标识 ID
        private int id;
        // 要转换的类的全限定名
        private String className;
        // 类的新字节码内容
        private byte[] bytes;
        // 类加载器的哈希码，用于匹配特定的类加载器
        private String hashCode;
        // 类加载器的类名，用于通过类名匹配类加载器
        private String classLoaderClass;

        /**
         * 被 transform 触发次数
         * 每次该条目的转换被应用时，计数器会增加
         * 这个值可以用来监控转换的活跃程度
         */
        private int transformCount = 0;

        /**
         * 构造函数 - 创建一个新的 Retransform 条目
         * 自动分配唯一 ID，并设置所有相关属性
         *
         * @param className 类的全限定名
         * @param bytes 类的新字节码内容
         * @param hashCode 类加载器的哈希码（可以为 null）
         * @param classLoaderClass 类加载器的类名（可以为 null）
         */
        public RetransformEntry(String className, byte[] bytes, String hashCode, String classLoaderClass) {
            // 使用原子计数器生成唯一 ID
            id = counter.incrementAndGet();
            this.className = className;
            this.bytes = bytes;
            this.hashCode = hashCode;
            this.classLoaderClass = classLoaderClass;
        }

        /**
         * 增加转换触发计数
         * 每次该条目的转换被应用到类时调用
         */
        public void incTransformCount() {
            transformCount++;
        }

        /**
         * 获取条目的唯一 ID
         *
         * @return 条目 ID
         */
        public int getId() {
            return id;
        }

        /**
         * 设置条目的 ID
         * 一般不需要手动设置，由构造函数自动分配
         *
         * @param id 条目 ID
         */
        public void setId(int id) {
            this.id = id;
        }

        /**
         * 获取转换触发次数
         *
         * @return 转换被触发的次数
         */
        public int getTransformCount() {
            return transformCount;
        }

        /**
         * 设置转换触发次数
         *
         * @param transformCount 转换触发次数
         */
        public void setTransformCount(int transformCount) {
            this.transformCount = transformCount;
        }

        /**
         * 获取类的全限定名
         *
         * @return 类名
         */
        public String getClassName() {
            return className;
        }

        /**
         * 设置类的全限定名
         *
         * @param className 类名
         */
        public void setClassName(String className) {
            this.className = className;
        }

        /**
         * 获取类的字节码内容
         *
         * @return 字节数组形式的类字节码
         */
        public byte[] getBytes() {
            return bytes;
        }

        /**
         * 设置类的字节码内容
         *
         * @param bytes 字节数组形式的类字节码
         */
        public void setBytes(byte[] bytes) {
            this.bytes = bytes;
        }

        /**
         * 获取类加载器的哈希码
         *
         * @return 类加载器的十六进制哈希码字符串
         */
        public String getHashCode() {
            return hashCode;
        }

        /**
         * 设置类加载器的哈希码
         *
         * @param hashCode 类加载器的十六进制哈希码字符串
         */
        public void setHashCode(String hashCode) {
            this.hashCode = hashCode;
        }

        /**
         * 获取类加载器的类名
         *
         * @return 类加载器的完整类名
         */
        public String getClassLoaderClass() {
            return classLoaderClass;
        }

        /**
         * 设置类加载器的类名
         *
         * @param classLoaderClass 类加载器的完整类名
         */
        public void setClassLoaderClass(String classLoaderClass) {
            this.classLoaderClass = classLoaderClass;
        }
    }

    /**
     * 添加新的 retransform 条目到全局列表
     * 该方法是线程安全的，使用 synchronized 保证并发访问的正确性
     * 添加后会按 ID 对所有条目进行排序
     *
     * @param retransformEntryList 要添加的新条目列表
     */
    public static synchronized void addRetransformEntry(List<RetransformEntry> retransformEntryList) {
        // 创建临时列表，包含所有现有的条目
        List<RetransformEntry> tmp = new ArrayList<RetransformEntry>();
        tmp.addAll(retransformEntries);
        // 添加新的条目
        tmp.addAll(retransformEntryList);
        // 按 ID 对所有条目进行排序，确保条目按 ID 顺序排列
        Collections.sort(tmp, new Comparator<RetransformEntry>() {
            @Override
            public int compare(RetransformEntry entry1, RetransformEntry entry2) {
                return Integer.compare(entry1.getId(), entry2.getId());
            }
        });
        // 更新全局条目列表
        retransformEntries = tmp;
    }

    /**
     * 删除指定 ID 的 retransform 条目
     * 该方法是线程安全的，使用 synchronized 保证并发访问的正确性
     *
     * @param id 要删除的条目 ID
     * @return 被删除的条目对象，如果找不到对应 ID 则返回 null
     */
    public static synchronized RetransformEntry deleteRetransformEntry(int id) {
        RetransformEntry result = null;
        // 创建临时列表，用于存储除指定 ID 外的所有条目
        List<RetransformEntry> tmp = new ArrayList<RetransformEntry>();
        for (RetransformEntry entry : retransformEntries) {
            if (entry.getId() != id) {
                // 保留不匹配的条目
                tmp.add(entry);
            } else {
                // 记录要删除的条目
                result = entry;
            }
        }
        // 更新全局条目列表
        retransformEntries = tmp;
        // 返回被删除的条目
        return result;
    }

    /**
     * 获取所有已注册的 retransform 条目
     * 注意：返回的是列表的引用，不是副本
     *
     * @return 所有 retransform 条目的列表
     */
    public static List<RetransformEntry> allRetransformEntries() {
        return retransformEntries;
    }

    /**
     * 删除所有 retransform 条目
     * 该方法是线程安全的，使用 synchronized 保证并发访问的正确性
     */
    public static synchronized void deleteAllRetransformEntry() {
        // 创建一个新的空列表，清空所有条目
        retransformEntries = new ArrayList<RetransformEntry>();
    }

    /**
     * Retransform 类文件转换器
     * 实现了 ClassFileTransformer 接口，用于在类加载/重加载时转换类的字节码
     *
     * 工作原理：
     * 1. 当 JVM 需要加载或重新定义类时，会调用此转换器的 transform 方法
     * 2. 转换器会遍历所有已注册的 retransform 条目
     * 3. 找到与当前类匹配的条目（类名、类加载器都匹配）
     * 4. 返回条目中存储的新字节码，实现类的转换
     *
     * 注意：使用倒序遍历，使后添加的条目优先生效（覆盖前面的配置）
     */
    static class RetransformClassFileTransformer implements ClassFileTransformer {
        /**
         * 转换类文件的方法
         * 当类被加载或重新定义时，JVM 会调用此方法
         *
         * @param loader 定义要转换的类的类加载器；如果是引导加载器，则为 null
         * @param className 类的全限定名，使用 '/' 作为包分隔符
         * @param classBeingRedefined 被重新定义的类；如果是类加载（不是重定义），则为 null
         * @param protectionDomain 正在定义或重定义的类的保护域
         * @param classfileBuffer 类文件格式的输入字节缓冲区（不得修改）
         * @return 转换后的类文件字节码，如果不需要转换则返回 null
         * @throws IllegalClassFormatException 如果类文件格式不合法
         */
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            // 如果类名为 null，直接返回 null（不转换）
            if (className == null) {
                return null;
            }

            // 将类名从 '/' 分隔符转换为 '.' 分隔符，以便与条目中的类名进行比较
            className = className.replace('/', '.');

            // 获取所有已注册的 retransform 条目
            List<RetransformEntry> allRetransformEntries = allRetransformEntries();
            // 使用倒序迭代器遍历条目列表
            // 倒序的原因：使后添加的条目优先生效（类似配置文件的覆盖机制）
            ListIterator<RetransformEntry> listIterator = allRetransformEntries
                    .listIterator(allRetransformEntries.size());
            while (listIterator.hasPrevious()) {
                // 获取前一个条目
                RetransformEntry retransformEntry = listIterator.previous();
                int id = retransformEntry.getId();
                // 标记是否需要更新类的字节码
                boolean updateFlag = false;
                // 首先检查类名是否匹配
                // 类名一致，则看是否要比较 loader，如果不需要比较 loader，则认为成功
                if (className.equals(retransformEntry.getClassName())) {
                    // 如果条目指定了类加载器相关的限制，需要检查类加载器是否匹配
                    if (retransformEntry.getClassLoaderClass() != null || retransformEntry.getHashCode() != null) {
                        // 检查类加载器是否匹配
                        updateFlag = isLoaderMatch(retransformEntry, loader);
                    } else {
                        // 没有类加载器限制，直接认为匹配
                        updateFlag = true;
                    }
                }

                // 如果找到了匹配的条目
                if (updateFlag) {
                    // 记录转换信息
                    logger.info("RetransformCommand match class: {}, id: {}, classLoaderClass: {}, hashCode: {}",
                            className, id, retransformEntry.getClassLoaderClass(), retransformEntry.getHashCode());
                    // 增加该条目的转换计数
                    retransformEntry.incTransformCount();
                    // 返回新字节码，实现类的转换
                    return retransformEntry.getBytes();
                }

            }

            // 没有找到匹配的条目，返回 null（不转换）
            return null;
        }

        /**
         * 检查类加载器是否与 retransform 条目匹配
         *
         * @param retransformEntry retransform 条目
         * @param loader 要检查的类加载器
         * @return true 表示匹配，false 表示不匹配
         */
        private boolean isLoaderMatch(RetransformEntry retransformEntry, ClassLoader loader) {
            // 如果类加载器为 null（引导类加载器），不匹配
            if (loader == null) {
                return false;
            }
            // 如果条目指定了类加载器的类名，检查是否匹配
            if (retransformEntry.getClassLoaderClass() != null) {
                if (loader.getClass().getName().equals(retransformEntry.getClassLoaderClass())) {
                    return true;
                }
            }
            // 如果条目指定了类加载器的哈希码，检查是否匹配
            if (retransformEntry.getHashCode() != null) {
                String hashCode = Integer.toHexString(loader.hashCode());
                if (hashCode.equals(retransformEntry.getHashCode())) {
                    return true;
                }
            }
            // 都不匹配
            return false;
        }

    }
}
