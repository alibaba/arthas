package com.taobao.arthas.core.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.concurrent.ConcurrentWeakKeyHashMap;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.system.ExecStatus;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.ProcessAware;

/**
 * 通知监听器管理类
 *
 * 该类负责管理和维护所有的AdviceListener（通知监听器），支持按类加载器、类名、方法名和方法描述进行监听器的注册和查询。
 * 主要用于Arthas的各种诊断命令（如watch、trace等）在方法增强时获取对应的监听器。
 *
 * 功能特点：
 * 1. 支持普通AdviceListener的注册和查询
 * 2. 支持Trace相关的AdviceListener的注册和查询
 * 3. 使用弱引用的ConcurrentWeakKeyHashMap存储ClassLoader，避免内存泄漏
 * 4. 定时清理已失效的监听器（进程已终止的）
 * 5. 为Bootstrap ClassLoader提供FakeBootstrapClassLoader包装
 *
 * TODO 待实现的功能：
 * - line级别的listener记录方式
 * - trace命令避免重复trace到SPY的invoke
 * - trace命令动态增加新函数
 * - 在SPY中动态生成存放Listener数组的类
 * - 支持行号绑定以精确输出行号
 * - 动态增加annotation防止重复增强
 * - 支持通过Listener ID进行trace/watch
 *
 * @author hengyunabc 2020-04-24
 */

/**
 * 
 * TODO line 的记录 listener方式？ 还是有string为key，不过 classname|method|desc|num 这样子？
 * 判断是否已插入了，可以在两行中间查询，有没有 SpyAPI 的invoke?
 * 
 * TODO trace的怎么搞？ trace 只记录一次就可以了 classname|method|desc|trace ? 怎么避免 trace 到
 * SPY的invoke ？直接忽略？
 * 
 * TODO trace命令可以动态的增加 新的函数进去不？只要关联上同一个 Listener应该是可以的。
 * 
 * TODO 在SPY里放很多的 Object数组，然后动态的设置进去？ 比如有新的 Listener来的时候。 这样子连查表都不用了。 甚至可以动态生成
 * 存放这些 Listener数组的类？ 这样子的话，只要有 Binding那里，查询到一个具体分配好的类， 这样子就可以了？
 * 甚至每个ClassLoader里都动态生成这样子的 存放类，那么这样子不可以避免查 ClassLoader了么？
 * 
 * 动态为每一个增强类，生成一个新的类，新的类里，有各种的 ID 数组，保存每一个类的每一种 trace 点的信息？？
 * 
 * 多个 watch命令 对同一个类，现在的逻辑是，每个watch都有一个自己的 TransForm，但不会重复增强，因为做了判断。
 * watch命令停止时，也没有去掉增强的代码。 只有reset时 才会去掉。
 * 
 * 其实用户想查看局部变量，并不是想查看哪一行！ 而是想看某个函数里子调用时的 局部变量的值！ 所以实际上是想要一个新的命令，比如 watchinmethod
 * ， 可以 在某个子调用里，
 * 
 * TODO 现在的trace 可以输出行号，可能不是很精确，但是可以对应上的。 这个在新的方式里怎么支持？ 增加一个 linenumber binding？
 * 从mehtodNode，向上查找到最近的行号？
 * 
 * TODO 防止重复增强，最重要的应该还是动态增加 annotation，这个才是真正可以做到某一行，某一个子 invoke 都能识别出来的！ 无论是
 * transform多少次！ 字节码怎么动态加 annotation ？ annotation里签名用 url ?的key/value方式表达！
 * 这样子可以有效还原信息
 * 
 * TODO 是否考虑一个 trace /watch命令之后，得到一个具体的 Listener ID， 允许在另外的窗口里，再次
 * trace/watch时指定这个ID，就会查找到，并处理。 这样子的话，真正达到了动态灵活的，一层一层增加的trace ！
 * 
 * 
 * @author hengyunabc 2020-04-24
 *
 */
public class AdviceListenerManager {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(AdviceListenerManager.class);
    // 用于包装Bootstrap ClassLoader的伪类加载器实例
    private static final FakeBootstrapClassLoader FAKEBOOTSTRAPCLASSLOADER = new FakeBootstrapClassLoader();

    // 静态初始化块：启动定时任务清理失效的AdviceListener
    static {
        // 创建一个定时任务，每3秒执行一次，用于清理失效的 AdviceListener
        ArthasBootstrap.getInstance().getScheduledExecutorService().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    // 遍历所有ClassLoader对应的AdviceListenerManager
                    for (Entry<ClassLoader, ClassLoaderAdviceListenerManager> entry : adviceListenerMap.entrySet()) {
                        ClassLoaderAdviceListenerManager adviceListenerManager = entry.getValue();
                        // 同步访问，避免并发问题
                        synchronized (adviceListenerManager) {
                            // 遍历该ClassLoader下的所有监听器列表
                            for (Entry<String, List<AdviceListener>> eee : adviceListenerManager.map.entrySet()) {
                                List<AdviceListener> listeners = eee.getValue();
                                // 创建新的列表用于保存有效的监听器
                                List<AdviceListener> newResult = new ArrayList<AdviceListener>();
                                for (AdviceListener listener : listeners) {
                                    // 检查监听器是否实现了ProcessAware接口
                                    if (listener instanceof ProcessAware) {
                                        ProcessAware processAware = (ProcessAware) listener;
                                        Process process = processAware.getProcess();
                                        // 如果进程为空，跳过该监听器
                                        if (process == null) {
                                            continue;
                                        }
                                        // 获取进程状态
                                        ExecStatus status = process.status();
                                        // 只有未终止的进程对应的监听器才保留
                                        if (!status.equals(ExecStatus.TERMINATED)) {
                                            newResult.add(listener);
                                        }
                                    }
                                }

                                // 如果有效监听器数量与原数量不同，更新map
                                if (newResult.size() != listeners.size()) {
                                    adviceListenerManager.map.put(eee.getKey(), newResult);
                                }

                            }
                        }
                    }
                } catch (Throwable e) {
                    // 捕获所有异常，避免定时任务终止
                    try {
                        logger.error("clean AdviceListener error", e);
                    } catch (Throwable t) {
                        // ignore - 忽略日志记录异常
                    }
                }
            }
        }, 3, 3, TimeUnit.SECONDS); // 初始延迟3秒，之后每3秒执行一次
    }

    /**
     * 核心数据结构：使用弱引用的ConcurrentHashMap存储ClassLoader到AdviceListenerManager的映射
     * 使用弱引用是为了避免ClassLoader无法被回收导致内存泄漏
     * Key: ClassLoader（弱引用）
     * Value: ClassLoaderAdviceListenerManager（该ClassLoader对应的监听器管理器）
     */
    private static final ConcurrentWeakKeyHashMap<ClassLoader, ClassLoaderAdviceListenerManager> adviceListenerMap = new ConcurrentWeakKeyHashMap<ClassLoader, ClassLoaderAdviceListenerManager>();

    /**
     * 类加载器级别的AdviceListener管理器（内部类）
     *
     * 该类负责管理特定ClassLoader下的所有AdviceListener，使用ConcurrentHashMap存储监听器映射关系。
     * 每个ClassLoader对应一个ClassLoaderAdviceListenerManager实例。
     *
     * 数据结构：
     * - Key: 由类名、方法名、方法描述符拼接而成的字符串
     * - Value: 对应的AdviceListener列表
     */
    static class ClassLoaderAdviceListenerManager {
        /**
         * 监听器映射表
         * Key: 类名+方法名+方法描述符（普通方法） 或 类名+所有者+方法名+方法描述符（trace方法）
         * Value: 对应的AdviceListener列表
         */
        private ConcurrentHashMap<String, List<AdviceListener>> map = new ConcurrentHashMap<String, List<AdviceListener>>();

        /**
         * 生成普通方法的监听器key
         *
         * @param className 类名
         * @param methodName 方法名
         * @param methodDesc 方法描述符（ASM类型的描述符）
         * @return 拼接后的key字符串
         */
        private String key(String className, String methodName, String methodDesc) {
            return className + methodName + methodDesc;
        }

        /**
         * 生成trace方法的监听器key
         * trace需要额外的owner信息（调用的目标类）来区分不同的调用路径
         *
         * @param className 类名
         * @param owner 调用的目标类（trace中的被调用类）
         * @param methodName 方法名
         * @param methodDesc 方法描述符
         * @return 拼接后的key字符串
         */
        private String keyForTrace(String className, String owner, String methodName, String methodDesc) {
            return className + owner + methodName + methodDesc;
        }

        /**
         * 注册普通的AdviceListener
         * 用于watch、monitor等命令
         *
         * @param className 类名（可能使用'/'分隔符）
         * @param methodName 方法名
         * @param methodDesc 方法描述符
         * @param listener 要注册的监听器
         */
        public void registerAdviceListener(String className, String methodName, String methodDesc,
                AdviceListener listener) {
            // 使用synchronized保证线程安全
            synchronized (this) {
                // 将类名中的'/'替换为'.'
                className = className.replace('/', '.');
                // 生成唯一的key
                String key = key(className, methodName, methodDesc);

                // 获取已存在的监听器列表，如果不存在则创建新的
                List<AdviceListener> listeners = map.get(key);
                if (listeners == null) {
                    listeners = new ArrayList<AdviceListener>();
                    map.put(key, listeners);
                }
                // 避免重复添加同一个监听器
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        }

        /**
         * 查询指定方法的AdviceListener列表
         *
         * @param className 类名（可能使用'/'分隔符）
         * @param methodName 方法名
         * @param methodDesc 方法描述符
         * @return 对应的AdviceListener列表，如果不存在则返回null
         */
        public List<AdviceListener> queryAdviceListeners(String className, String methodName, String methodDesc) {
            // 将类名中的'/'替换为'.'
            className = className.replace('/', '.');
            // 生成key并查询
            String key = key(className, methodName, methodDesc);

            List<AdviceListener> listeners = map.get(key);

            return listeners;
        }

        /**
         * 注册trace相关的AdviceListener
         * trace命令需要额外的owner参数来标识被调用的类
         *
         * @param className 类名（可能使用'/'分隔符）
         * @param owner 被调用的目标类名
         * @param methodName 方法名
         * @param methodDesc 方法描述符
         * @param listener 要注册的监听器
         */
        public void registerTraceAdviceListener(String className, String owner, String methodName, String methodDesc,
                AdviceListener listener) {
            // 使用synchronized保证线程安全
            synchronized (this) {
                // 将类名中的'/'替换为'.'
                className = className.replace('/', '.');
                // 生成trace专用的key（包含owner信息）
                String key = keyForTrace(className, owner, methodName, methodDesc);

                // 获取已存在的监听器列表，如果不存在则创建新的
                List<AdviceListener> listeners = map.get(key);
                if (listeners == null) {
                    listeners = new ArrayList<AdviceListener>();
                    map.put(key, listeners);
                }
                // 避免重复添加同一个监听器
                if (!listeners.contains(listener)) {
                    listeners.add(listener);
                }
            }
        }

        /**
         * 查询trace相关的AdviceListener列表
         *
         * @param className 类名（可能使用'/'分隔符）
         * @param owner 被调用的目标类名
         * @param methodName 方法名
         * @param methodDesc 方法描述符
         * @return 对应的AdviceListener列表，如果不存在则返回null
         */
        public List<AdviceListener> queryTraceAdviceListeners(String className, String owner, String methodName,
                String methodDesc) {
            // 将类名中的'/'替换为'.'
            className = className.replace('/', '.');
            // 生成trace专用的key并查询
            String key = keyForTrace(className, owner, methodName, methodDesc);

            List<AdviceListener> listeners = map.get(key);

            return listeners;
        }
    }

    /**
     * 注册普通的AdviceListener
     * 将监听器注册到指定ClassLoader下的指定方法
     *
     * @param classLoader 类加载器，如果为null则使用Bootstrap ClassLoader
     * @param className 类名（可能使用'/'分隔符）
     * @param methodName 方法名
     * @param methodDesc 方法描述符
     * @param listener 要注册的监听器
     */
    public static void registerAdviceListener(ClassLoader classLoader, String className, String methodName,
            String methodDesc, AdviceListener listener) {
        // 包装ClassLoader，处理null的情况（Bootstrap ClassLoader）
        classLoader = wrap(classLoader);
        // 将类名中的'/'替换为'.'
        className = className.replace('/', '.');

        // 记录注册日志
        logger.info("registerAdviceListener: classLoader={}, className={}, methodName={}, methodDesc={}, listener={}",
                classLoader, className, methodName, methodDesc, listener.id());

        // 获取或创建ClassLoader对应的管理器
        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager == null) {
            // 如果管理器不存在，创建新的并放入map
            manager = new ClassLoaderAdviceListenerManager();
            adviceListenerMap.put(classLoader, manager);
        }
        // 委托给管理器进行注册
        manager.registerAdviceListener(className, methodName, methodDesc, listener);
    }

    /**
     * 更新监听器列表
     * TODO 该方法目前为空实现，预留用于动态更新监听器
     */
    public static void updateAdviceListeners() {

    }

    /**
     * 查询指定方法的AdviceListener列表
     *
     * @param classLoader 类加载器，如果为null则使用Bootstrap ClassLoader
     * @param className 类名（可能使用'/'分隔符）
     * @param methodName 方法名
     * @param methodDesc 方法描述符
     * @return 对应的AdviceListener列表，如果不存在则返回null
     */
    public static List<AdviceListener> queryAdviceListeners(ClassLoader classLoader, String className,
            String methodName, String methodDesc) {
        // 包装ClassLoader，处理null的情况
        classLoader = wrap(classLoader);
        // 将类名中的'/'替换为'.'
        className = className.replace('/', '.');
        // 获取ClassLoader对应的管理器
        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager != null) {
            // 委托给管理器进行查询
            return manager.queryAdviceListeners(className, methodName, methodDesc);
        }

        return null;
    }

    /**
     * 注册trace相关的AdviceListener
     * trace命令需要额外的owner参数来标识被调用的类
     *
     * @param classLoader 类加载器，如果为null则使用Bootstrap ClassLoader
     * @param className 类名（可能使用'/'分隔符）
     * @param owner 被调用的目标类名
     * @param methodName 方法名
     * @param methodDesc 方法描述符
     * @param listener 要注册的监听器
     */
    public static void registerTraceAdviceListener(ClassLoader classLoader, String className, String owner,
            String methodName, String methodDesc, AdviceListener listener) {
        // 包装ClassLoader，处理null的情况
        classLoader = wrap(classLoader);
        // 将类名中的'/'替换为'.'
        className = className.replace('/', '.');

        // 获取或创建ClassLoader对应的管理器
        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager == null) {
            // 如果管理器不存在，创建新的并放入map
            manager = new ClassLoaderAdviceListenerManager();
            adviceListenerMap.put(classLoader, manager);
        }
        // 委托给管理器进行注册
        manager.registerTraceAdviceListener(className, owner, methodName, methodDesc, listener);
    }

    /**
     * 查询trace相关的AdviceListener列表
     *
     * @param classLoader 类加载器，如果为null则使用Bootstrap ClassLoader
     * @param className 类名（可能使用'/'分隔符）
     * @param owner 被调用的目标类名
     * @param methodName 方法名
     * @param methodDesc 方法描述符
     * @return 对应的AdviceListener列表，如果不存在则返回null
     */
    public static List<AdviceListener> queryTraceAdviceListeners(ClassLoader classLoader, String className,
            String owner, String methodName, String methodDesc) {
        // 包装ClassLoader，处理null的情况
        classLoader = wrap(classLoader);
        // 将类名中的'/'替换为'.'
        className = className.replace('/', '.');
        // 获取ClassLoader对应的管理器
        ClassLoaderAdviceListenerManager manager = adviceListenerMap.get(classLoader);

        if (manager != null) {
            // 委托给管理器进行查询
            return manager.queryTraceAdviceListeners(className, owner, methodName, methodDesc);
        }

        return null;
    }

    /**
     * 包装ClassLoader
     * 将null转换为FakeBootstrapClassLoader，用于统一处理Bootstrap ClassLoader的情况
     * Bootstrap ClassLoader在Java中无法获取引用，所以用一个伪ClassLoader来表示
     *
     * @param classLoader 原始的ClassLoader，可能为null
     * @return 如果classLoader不为null则返回原值，否则返回FakeBootstrapClassLoader实例
     */
    private static ClassLoader wrap(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }
        // 返回Bootstrap ClassLoader的代理
        return FAKEBOOTSTRAPCLASSLOADER;
    }

    /**
     * 伪Bootstrap ClassLoader（内部类）
     *
     * 用于代表Java的Bootstrap ClassLoader（启动类加载器）。
     * Bootstrap ClassLoader是用C++实现的，无法在Java代码中获取其实例，
     * 所以创建这个空类来作为标识，用于在adviceListenerMap中作为key使用。
     */
    private static class FakeBootstrapClassLoader extends ClassLoader {

    }
}
