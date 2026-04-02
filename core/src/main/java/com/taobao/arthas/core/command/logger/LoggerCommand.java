package com.taobao.arthas.core.command.logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.IOUtils;
import com.taobao.arthas.common.ReflectUtils;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.LoggerModel;
import com.taobao.arthas.core.command.model.ClassLoaderVO;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ClassUtils;
import com.taobao.arthas.core.util.ClassLoaderUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

/**
 * Logger命令
 *
 * 用于查看和修改应用程序的日志配置。
 * 支持Log4j、Logback和Log4j2三种日志框架。
 * 可以查看Logger信息、Appender信息，以及动态修改日志级别。
 *
 * @author hengyunabc 2019-09-04
 */
//@formatter:off
@Name("logger")
@Summary("Print logger info, and update the logger level")
@Description("\nExamples:\n"
                + "  logger\n"
                + "  logger -c 327a647b\n"
                + "  logger -c 327a647b --name ROOT --level debug\n"
                + "  logger --include-no-appender\n"
                + Constants.WIKI + Constants.WIKI_HOME + "logger")
//@formatter:on
public class LoggerCommand extends AnnotatedCommand {
    /**
     * 日志记录器，用于记录命令执行过程中的日志
     */
    private static final Logger logger = LoggerFactory.getLogger(LoggerCommand.class);

    /**
     * LoggerHelper类的字节码
     */
    private static byte[] LoggerHelperBytes;

    /**
     * Log4jHelper类的字节码
     */
    private static byte[] Log4jHelperBytes;

    /**
     * LogbackHelper类的字节码
     */
    private static byte[] LogbackHelperBytes;

    /**
     * Log4j2Helper类的字节码
     */
    private static byte[] Log4j2HelperBytes;

    /**
     * 类到字节码的映射表，用于在不同ClassLoader中动态加载Helper类
     */
    private static Map<Class<?>, byte[]> classToBytesMap = new HashMap<Class<?>, byte[]>();

    /**
     * Arthas类加载器的哈希码，用于生成唯一的Helper类名
     */
    private static String arthasClassLoaderHash = ClassLoaderUtils
            .classLoaderHash(LoggerCommand.class.getClassLoader());

    /**
     * 静态初始化块
     * 加载所有Helper类的字节码，并建立类到字节码的映射
     */
    static {
        // 加载各个Helper类的字节码
        LoggerHelperBytes = loadClassBytes(LoggerHelper.class);
        Log4jHelperBytes = loadClassBytes(Log4jHelper.class);
        LogbackHelperBytes = loadClassBytes(LogbackHelper.class);
        Log4j2HelperBytes = loadClassBytes(Log4j2Helper.class);

        // 建立类到字节码的映射，用于后续在不同ClassLoader中加载
        classToBytesMap.put(LoggerHelper.class, LoggerHelperBytes);
        classToBytesMap.put(Log4jHelper.class, Log4jHelperBytes);
        classToBytesMap.put(LogbackHelper.class, LogbackHelperBytes);
        classToBytesMap.put(Log4j2Helper.class, Log4j2HelperBytes);
    }

    /**
     * Logger名称，用于指定要操作的具体Logger
     */
    private String name;

    /**
     * ClassLoader的哈希码，用于指定要操作的ClassLoader
     */
    private String hashCode;

    /**
     * ClassLoader的类名，用于通过类名查找ClassLoader
     */
    private String classLoaderClass;

    /**
     * 日志级别，用于设置Logger的日志级别
     */
    private String level;

    /**
     * 是否包含没有Appender的Logger，默认为false
     * include the loggers which don't have appenders, default false.
     */
    private boolean includeNoAppender;

    /**
     * 设置Logger名称
     *
     * @param name Logger名称，如"ROOT"或具体的类名
     */
    @Option(shortName = "n", longName = "name")
    @Description("logger name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 设置ClassLoader的哈希码
     *
     * @param hashCode ClassLoader的哈希码，如果未设置则默认使用SystemClassLoader
     */
    @Option(shortName = "c", longName = "classloader")
    @Description("classLoader hashcode, if no value is set, default value is SystemClassLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 设置ClassLoader的类名
     *
     * @param classLoaderClass 指定类的ClassLoader的类名
     */
    @Option(longName = "classLoaderClass")
    @Description("The class name of the special class's classLoader.")
    public void setClassLoaderClass(String classLoaderClass) {
        this.classLoaderClass = classLoaderClass;
    }

    /**
     * 设置日志级别
     *
     * @param level 日志级别，如"DEBUG"、"INFO"、"WARN"、"ERROR"
     */
    @Option(shortName = "l", longName = "level")
    @Description("set logger level")
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * 设置是否包含没有Appender的Logger
     *
     * @param includeNoAppender 是否包含没有Appender的Logger，默认为false
     */
    @Option(longName = "include-no-appender", flag = true)
    @Description("include the loggers which don't have appenders, default value false")
    public void setHaveAppender(boolean includeNoAppender) {
        this.includeNoAppender = includeNoAppender;
    }

    /**
     * 处理命令
     *
     * 根据参数决定是更新日志级别还是查询Logger信息
     *
     * @param process 命令处理上下文
     */
    @Override
    public void process(CommandProcess process) {
        // 所有代码都用 hashCode 来定位classloader，如果有指定 classLoaderClass，则尝试用 classLoaderClass 找到对应 classloader 的 hashCode
        if (hashCode == null && classLoaderClass != null) {
            // 获取Instrumentation实例
            Instrumentation inst = process.session().getInstrumentation();

            // 通过类名查找匹配的ClassLoader
            List<ClassLoader> matchedClassLoaders = ClassLoaderUtils.getClassLoaderByClassName(inst,
                    classLoaderClass);

            if (matchedClassLoaders.size() == 1) {
                // 只找到一个ClassLoader，使用其哈希码
                hashCode = Integer.toHexString(matchedClassLoaders.get(0).hashCode());
            } else if (matchedClassLoaders.size() > 1) {
                // 找到多个ClassLoader，提示用户选择
                Collection<ClassLoaderVO> classLoaderVOList = ClassUtils
                        .createClassLoaderVOList(matchedClassLoaders);
                LoggerModel loggerModel = new LoggerModel().setClassLoaderClass(classLoaderClass)
                        .setMatchedClassLoaders(classLoaderVOList);
                process.appendResult(loggerModel);
                process.end(-1,
                        "Found more than one classloader by class name, please specify classloader with '-c <classloader hash>'");
                return;
            } else {
                // 没有找到ClassLoader
                process.end(-1, "Can not find classloader by class name: " + classLoaderClass + ".");
                return;
            }
        }

        // 每个分支中调用process.end()结束执行
        if (this.name != null && this.level != null) {
            // 如果指定了Logger名称和日志级别，则执行更新日志级别的操作
            level(process);
        } else {
            // 否则执行查询Logger信息的操作
            loggers(process);
        }
    }

    /**
     * 更新Logger日志级别
     *
     * 根据检测到的日志框架类型，调用相应的Helper类来更新日志级别
     *
     * @param process 命令处理上下文
     */
    public void level(CommandProcess process) {
        // 获取Instrumentation实例
        Instrumentation inst = process.session().getInstrumentation();
        boolean result = false;

        // 如果不指定 classloader，则默认用 SystemClassLoader
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        // 如果指定了ClassLoader哈希码，则查找对应的ClassLoader
        if (hashCode != null) {
            classLoader = ClassLoaderUtils.getClassLoader(inst, hashCode);
            if (classLoader == null) {
                process.end(-1, "Can not find classloader by hashCode: " + hashCode + ".");
                return;
            }
        }

        // 查找ClassLoader中使用的日志框架类型
        LoggerTypes loggerTypes = findLoggerTypes(process.session().getInstrumentation(), classLoader);

        // 如果使用了Log4j，尝试更新日志级别
        if (loggerTypes.contains(LoggerType.LOG4J)) {
            try {
                Boolean updateResult = this.updateLevel(inst, classLoader, Log4jHelper.class);
                if (Boolean.TRUE.equals(updateResult)) {
                    result = true;
                }
            } catch (Throwable e) {
                logger.error("logger command update log4j level error", e);
            }
        }

        // 如果使用了Logback，尝试更新日志级别
        if (loggerTypes.contains(LoggerType.LOGBACK)) {
            try {
                Boolean updateResult = this.updateLevel(inst, classLoader, LogbackHelper.class);
                if (Boolean.TRUE.equals(updateResult)) {
                    result = true;
                }
            } catch (Throwable e) {
                logger.error("logger command update logback level error", e);
            }
        }

        // 如果使用了Log4j2，尝试更新日志级别
        if (loggerTypes.contains(LoggerType.LOG4J2)) {
            try {
                Boolean updateResult = this.updateLevel(inst, classLoader, Log4j2Helper.class);
                if (Boolean.TRUE.equals(updateResult)) {
                    result = true;
                }
            } catch (Throwable e) {
                logger.error("logger command update log4j2 level error", e);
            }
        }

        // 根据更新结果返回相应的消息
        if (result) {
            process.end(0, "Update logger level success.");
        } else {
            process.end(-1,
                    "Update logger level fail. Try to specify the classloader with the -c option. Use `sc -d CLASSNAME` to find out the classloader hashcode.");
        }
    }

    /**
     * 查询Logger信息
     *
     * 遍历所有ClassLoader，查找其中的Logger信息并返回
     *
     * @param process 命令处理上下文
     */
    public void loggers(CommandProcess process) {
        // 创建ClassLoader到Logger类型的映射表
        Map<ClassLoader, LoggerTypes> classLoaderLoggerMap = new LinkedHashMap<ClassLoader, LoggerTypes>();

        // 如果不指定 classloader，则打印所有 classloader 里的 logger 信息
        for (Class<?> clazz : process.session().getInstrumentation().getAllLoadedClasses()) {
            String className = clazz.getName();
            ClassLoader classLoader = clazz.getClassLoader();

            // if special classloader - 如果指定了ClassLoader哈希码，则只处理匹配的ClassLoader
            if (this.hashCode != null && !this.hashCode.equals(StringUtils.classLoaderHash(clazz))) {
                continue;
            }

            // 只处理有ClassLoader的类（忽略启动类加载器加载的类）
            if (classLoader != null) {
                // 获取或创建该ClassLoader的Logger类型集合
                LoggerTypes loggerTypes = classLoaderLoggerMap.get(classLoader);
                if (loggerTypes == null) {
                    loggerTypes = new LoggerTypes();
                    classLoaderLoggerMap.put(classLoader, loggerTypes);
                }
                // 根据类名更新Logger类型信息
                updateLoggerType(loggerTypes, classLoader, className);
            }
        }

        // 遍历所有ClassLoader，获取其中的Logger信息
        for (Entry<ClassLoader, LoggerTypes> entry : classLoaderLoggerMap.entrySet()) {
            ClassLoader classLoader = entry.getKey();
            LoggerTypes loggerTypes = entry.getValue();

            // 如果使用了Log4j，获取Log4j的Logger信息
            if (loggerTypes.contains(LoggerType.LOG4J)) {
                Map<String, Map<String, Object>> loggerInfoMap = loggerInfo(classLoader, Log4jHelper.class);
                process.appendResult(new LoggerModel(loggerInfoMap));
            }

            // 如果使用了Logback，获取Logback的Logger信息
            if (loggerTypes.contains(LoggerType.LOGBACK)) {
                Map<String, Map<String, Object>> loggerInfoMap = loggerInfo(classLoader, LogbackHelper.class);
                process.appendResult(new LoggerModel(loggerInfoMap));
            }

            // 如果使用了Log4j2，获取Log4j2的Logger信息
            if (loggerTypes.contains(LoggerType.LOG4J2)) {
                Map<String, Map<String, Object>> loggerInfoMap = loggerInfo(classLoader, Log4j2Helper.class);
                process.appendResult(new LoggerModel(loggerInfoMap));
            }
        }

        // 结束命令处理
        process.end();
    }

    /**
     * 查找指定ClassLoader中使用的日志框架类型
     *
     * @param inst Instrumentation实例
     * @param classLoader 要查找的ClassLoader
     * @return Logger类型集合
     */
    private LoggerTypes findLoggerTypes(Instrumentation inst, ClassLoader classLoader) {
        // 创建Logger类型集合
        LoggerTypes loggerTypes = new LoggerTypes();

        // 遍历所有已加载的类
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            // 只处理由指定ClassLoader加载的类
            if(classLoader == clazz.getClassLoader()) {
                // 根据类名更新Logger类型
                updateLoggerType(loggerTypes, classLoader, clazz.getName());
            }
        }
        return loggerTypes;
    }

    /**
     * 根据类名更新Logger类型信息
     *
     * 通过检查特定日志框架的核心类是否存在，判断使用了哪种日志框架
     *
     * @param loggerTypes Logger类型集合
     * @param classLoader ClassLoader实例
     * @param className 类名
     */
    private void updateLoggerType(LoggerTypes loggerTypes, ClassLoader classLoader, String className) {
        if ("org.apache.log4j.Logger".equals(className)) {
            // 判断 org.apache.log4j.AsyncAppender 是否存在，如果存在则是 log4j，不是slf4j-over-log4j
            try {
                if (classLoader.getResource("org/apache/log4j/AsyncAppender.class") != null) {
                    // 确认是真正的Log4j，添加到类型集合
                    loggerTypes.addType(LoggerType.LOG4J);
                }
            } catch (Throwable e) {
                // ignore - 忽略异常
            }
        } else if ("ch.qos.logback.classic.Logger".equals(className)) {
            try {
                // 检查Logback的核心类是否存在
                if (classLoader.getResource("ch/qos/logback/core/Appender.class") != null) {
                    loggerTypes.addType(LoggerType.LOGBACK);
                }
            } catch (Throwable e) {
                // ignore - 忽略异常
            }
        } else if ("org.apache.logging.log4j.Logger".equals(className)) {
            try {
                // 检查Log4j2的核心类是否存在
                if (classLoader.getResource("org/apache/logging/log4j/core/LoggerContext.class") != null) {
                    loggerTypes.addType(LoggerType.LOG4J2);
                }
            } catch (Throwable e) {
                // ignore - 忽略异常
            }
        }
    }

    /**
     * 在指定ClassLoader中加载或定义Helper类
     *
     * 为了在应用的ClassLoader中访问日志框架的内部类，需要将Helper类加载到应用的ClassLoader中。
     * 使用ASM重命名类名，避免类冲突。
     *
     * @param classLoader 目标ClassLoader
     * @param helperClass Helper类的Class对象
     * @return 加载后的Class对象，失败返回null
     */
    private static Class<?> helperClassNameWithClassLoader(ClassLoader classLoader, Class<?> helperClass) {
        // 获取目标ClassLoader的哈希码
        String classLoaderHash = ClassLoaderUtils.classLoaderHash(classLoader);

        // 获取Helper类的原始名称
        String className = helperClass.getName();

        // 生成唯一的Helper类名，包含Arthas ClassLoader和目标ClassLoader的哈希码
        // if want to debug, change to return className
        String helperClassName = className + arthasClassLoaderHash + classLoaderHash;

        try {
            // 尝试从目标ClassLoader加载已定义的Helper类
            return classLoader.loadClass(helperClassName);
        } catch (ClassNotFoundException e) {
            try {
                // 类不存在，使用ASM重命名类字节码，创建一个新类
                byte[] helperClassBytes = AsmRenameUtil.renameClass(classToBytesMap.get(helperClass),
                        helperClass.getName(), helperClassName);

                // 在目标ClassLoader中定义新类
                return ReflectUtils.defineClass(helperClassName, helperClassBytes, classLoader);
            } catch (Throwable e1) {
                // 记录定义Helper类时的错误
                logger.error("arthas loggger command try to define helper class error: " + helperClassName,
                        e1);
            }
        }
        return null;
    }

    /**
     * 获取Logger信息
     *
     * 通过反射调用Helper类的getLoggers方法，获取Logger的详细信息
     *
     * @param classLoader ClassLoader实例
     * @param helperClass Helper类的Class对象
     * @return Logger信息映射表
     */
    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> loggerInfo(ClassLoader classLoader, Class<?> helperClass) {
        // 初始化为空Map
        Map<String, Map<String, Object>> loggers = Collections.emptyMap();

        try {
            // 在目标ClassLoader中加载Helper类
            Class<?> clazz = helperClassNameWithClassLoader(classLoader, helperClass);

            // 获取getLoggers方法
            Method getLoggersMethod = clazz.getMethod("getLoggers", new Class<?>[]{String.class, boolean.class});

            // 反射调用getLoggers方法
            loggers = (Map<String, Map<String, Object>>) getLoggersMethod.invoke(null,
                    new Object[]{name, includeNoAppender});
        } catch (Throwable e) {
            // ignore - 忽略异常
        }

        //expose attributes to json: classloader, classloaderHash
        // 为每个Logger添加ClassLoader信息，便于输出到JSON
        for (Map<String, Object> loggerInfo : loggers.values()) {
            // 获取Logger的Class对象
            Class clazz = (Class) loggerInfo.get(LoggerHelper.clazz);

            // 添加ClassLoader名称和哈希码
            loggerInfo.put(LoggerHelper.classLoader, getClassLoaderName(clazz.getClassLoader()));
            loggerInfo.put(LoggerHelper.classLoaderHash, StringUtils.classLoaderHash(clazz));

            // 获取Logger的Appender列表
            List<Map<String, Object>> appenders = (List<Map<String, Object>>) loggerInfo.get(LoggerHelper.appenders);

            // 为每个Appender添加ClassLoader信息
            for (Map<String, Object> appenderInfo : appenders) {
                Class appenderClass = (Class) appenderInfo.get(LoggerHelper.clazz);
                if (appenderClass != null) {
                    appenderInfo.put(LoggerHelper.classLoader, getClassLoaderName(appenderClass.getClassLoader()));
                    appenderInfo.put(LoggerHelper.classLoaderHash, StringUtils.classLoaderHash(appenderClass));
                }
            }
        }

        return loggers;
    }

    /**
     * 获取ClassLoader的名称
     *
     * @param classLoader ClassLoader实例
     * @return ClassLoader的字符串表示，如果为null则返回null
     */
    private String getClassLoaderName(ClassLoader classLoader) {
        return classLoader == null ? null : classLoader.toString();
    }

    /**
     * 更新Logger级别
     *
     * 通过反射调用Helper类的updateLevel方法，更新指定Logger的日志级别
     *
     * @param inst Instrumentation实例（未使用）
     * @param classLoader ClassLoader实例
     * @param helperClass Helper类的Class对象
     * @return 更新成功返回true，失败返回false
     * @throws Exception 反射调用可能抛出异常
     */
    private Boolean updateLevel(Instrumentation inst, ClassLoader classLoader, Class<?> helperClass) throws Exception {
        // 在目标ClassLoader中加载Helper类
        Class<?> clazz = helperClassNameWithClassLoader(classLoader, helperClass);

        // 获取updateLevel方法
        Method updateLevelMethod = clazz.getMethod("updateLevel", new Class<?>[]{String.class, String.class});

        // 反射调用updateLevel方法，传入Logger名称和新的日志级别
        return (Boolean) updateLevelMethod.invoke(null, new Object[]{this.name, this.level});
    }

    /**
     * 日志框架类型枚举
     *
     * 定义了支持的三种日志框架类型
     */
    static enum LoggerType {
        /** Log4j 1.x日志框架 */
        LOG4J,

        /** Logback日志框架 */
        LOGBACK,

        /** Log4j 2.x日志框架 */
        LOG4J2
    }

    /**
     * Logger类型集合
     *
     * 用于存储一个ClassLoader中使用的所有日志框架类型
     */
    static class LoggerTypes {
        /**
         * 存储日志框架类型的集合
         */
        Set<LoggerType> types = new HashSet<LoggerType>();

        /**
         * 获取所有日志框架类型
         *
         * @return 日志框架类型集合
         */
        public Collection<LoggerType> types() {
            return types;
        }

        /**
         * 添加一个日志框架类型
         *
         * @param type 要添加的日志框架类型
         */
        public void addType(LoggerType type) {
            types.add(type);
        }

        /**
         * 检查是否包含指定的日志框架类型
         *
         * @param type 要检查的日志框架类型
         * @return 如果包含返回true，否则返回false
         */
        public boolean contains(LoggerType type) {
            return types.contains(type);
        }
    }

    /**
     * 加载类的字节码
     *
     * 从类路径中读取指定类的字节码，用于后续在不同ClassLoader中定义该类
     *
     * @param clazz 要加载字节码的类
     * @return 类的字节数组，加载失败返回null
     */
    private static byte[] loadClassBytes(Class<?> clazz) {
        try {
            // 从类路径中读取class文件
            InputStream stream = LoggerCommand.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class");

            // 将输入流转换为字节数组
            return IOUtils.getBytes(stream);
        } catch (IOException e) {
            // ignore - 忽略IO异常，返回null
            return null;
        }
    }
}
