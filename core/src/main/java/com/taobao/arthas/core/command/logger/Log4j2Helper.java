package com.taobao.arthas.core.command.logger;

import java.lang.reflect.Field;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Log4j2日志框架助手类
 * <p>
 * 该类提供了一系列静态方法用于操作和管理Log4j2日志框架。
 * 主要功能包括：
 * 1. 检测Log4j2是否可用
 * 2. 动态更新日志级别
 * 3. 获取Logger配置信息
 * 4. 获取Appender信息
 * </p>
 *
 * @author hengyunabc 2019-09-20
 *
 */
public class Log4j2Helper {

    /**
     * 标识Log4j2是否可用
     * 在静态初始化块中检测Log4j2是否存在并设置此标志
     */
    private static boolean Log4j2 = false;

    /**
     * LoggerConfig类中的config字段
     * 通过反射获取，用于访问LoggerConfig的私有配置信息
     */
    private static Field configField = null;

    /**
     * 静态初始化块
     * 在类加载时执行，用于检测Log4j2是否可用，并初始化反射字段
     */
    static {
        try {
            // 尝试加载Log4j2的Logger类
            Class<?> loggerClass = Log4j2Helper.class.getClassLoader().loadClass("org.apache.logging.log4j.Logger");
            // 这里可能会加载到其它上游ClassLoader的log4j2，因此需要判断是否当前classloader
            if (loggerClass.getClassLoader().equals(Log4j2Helper.class.getClassLoader())) {
                Log4j2 = true;
            }

            try {
                // 通过反射获取LoggerConfig类的config字段，用于后续访问配置信息
                configField = LoggerConfig.class.getDeclaredField("config");
                configField.setAccessible(true);
            } catch (Throwable e) {
                // ignore 忽略获取字段失败的异常
            }
        } catch (Throwable t) {
            // ignore 忽略类加载失败的异常
        }
    }

    /**
     * 判断字符串是否有长度
     *
     * @param str 要检查的字符串
     * @return 如果字符串不为null且不为空则返回true，否则返回false
     */
    public static boolean hasLength(String str) {
        return (str != null && !str.isEmpty());
    }

    /**
     * 根据名称获取Logger配置
     * <p>
     * 如果名称为空或为"root"，则返回root logger的配置
     * </p>
     *
     * @param name Logger的名称
     * @return 对应的LoggerConfig对象，如果不存在则返回null
     */
    private static LoggerConfig getLoggerConfig(String name) {
        // 如果名称为空或者是root，则使用root logger名称
        if (!hasLength(name) || LoggerConfig.ROOT.equalsIgnoreCase(name)) {
            name = LogManager.ROOT_LOGGER_NAME;
        }
        // 从当前上下文的配置中获取指定名称的Logger配置
        return getLoggerContext().getConfiguration().getLoggers().get(name);
    }

    /**
     * 获取Log4j2的Logger上下文
     *
     * @return LoggerContext对象
     */
    private static LoggerContext getLoggerContext() {
        return (LoggerContext) LogManager.getContext(false);
    }

    /**
     * 更新指定Logger的日志级别
     * <p>
     * 该方法用于动态修改Log4j2中指定Logger的日志级别。
     * 如果Logger不存在，会创建一个新的Logger配置。
     * </p>
     *
     * @param loggerName Logger的名称，如果为空或"root"则操作root logger
     * @param logLevel   要设置的日志级别（如TRACE、DEBUG、INFO、WARN、ERROR等）
     * @return 如果成功返回true，如果Log4j2不可用或日志级别无效返回null
     */
    public static Boolean updateLevel(String loggerName, String logLevel) {
        // 检查Log4j2是否可用
        if (Log4j2) {
            // 将日志级别字符串转换为大写并获取对应的Level对象
            Level level = Level.getLevel(logLevel.toUpperCase());
            // 如果日志级别无效，返回null
            if (level == null) {
                return null;
            }
            // 获取指定名称的Logger配置
            LoggerConfig loggerConfig = getLoggerConfig(loggerName);
            // 如果Logger配置不存在，则创建新的配置
            if (loggerConfig == null) {
                loggerConfig = new LoggerConfig(loggerName, level, true);
                getLoggerContext().getConfiguration().addLogger(loggerName, loggerConfig);
            } else {
                // 更新现有Logger的日志级别
                loggerConfig.setLevel(level);
            }
            // 更新Logger上下文，使更改生效
            getLoggerContext().updateLoggers();
            return Boolean.TRUE;
        }
        // Log4j2不可用时返回null
        return null;
    }

    /**
     * 获取Logger信息
     * <p>
     * 根据指定的Logger名称获取对应的配置信息，或者获取所有Logger的信息。
     * 可以选择是否包含没有Appender的Logger。
     * </p>
     *
     * @param name             Logger的名称，如果为null或空字符串则获取所有Logger
     * @param includeNoAppender 是否包含没有Appender的Logger
     * @return 包含Logger信息的Map，key为Logger名称，value为Logger的详细信息
     */
    public static Map<String, Map<String, Object>> getLoggers(String name, boolean includeNoAppender) {
        // 创建用于存储Logger信息的Map
        Map<String, Map<String, Object>> loggerInfoMap = new HashMap<String, Map<String, Object>>();
        // 如果Log4j2不可用，直接返回空Map
        if (!Log4j2) {
            return loggerInfoMap;
        }

        // 获取Log4j2的配置对象
        Configuration configuration = getLoggerContext().getConfiguration();

        // 如果指定了Logger名称
        if (name != null && !name.trim().isEmpty()) {
            // 获取指定名称的Logger配置
            LoggerConfig loggerConfig = configuration.getLoggerConfig(name);
            // 如果Logger配置不存在，返回空Map
            if (loggerConfig == null) {
                return loggerInfoMap;
            }
            // 排掉非root时，获取到root的logger config
            // 如果请求的不是root logger，但返回的是root配置，说明请求的logger不存在
            if (!name.equalsIgnoreCase(LoggerConfig.ROOT) && isEmpty(loggerConfig.getName())) {
                return loggerInfoMap;
            }
            // 将Logger信息添加到结果Map中
            loggerInfoMap.put(name, doGetLoggerInfo(loggerConfig));
        } else {
            // 获取所有logger时，如果没有appender则忽略（根据includeNoAppender参数决定）
            Map<String, LoggerConfig> loggers = configuration.getLoggers();
            if (loggers != null) {
                // 遍历所有Logger
                for (Entry<String, LoggerConfig> entry : loggers.entrySet()) {
                    LoggerConfig loggerConfig = entry.getValue();
                    // 如果不包含没有Appender的Logger，则只添加有Appender的Logger
                    if (!includeNoAppender) {
                        if (!loggerConfig.getAppenders().isEmpty()) {
                            loggerInfoMap.put(entry.getKey(), doGetLoggerInfo(entry.getValue()));
                        }
                    } else {
                        // 包含所有Logger，无论是否有Appender
                        loggerInfoMap.put(entry.getKey(), doGetLoggerInfo(entry.getValue()));
                    }
                }
            }
        }

        return loggerInfoMap;
    }

    /**
     * 通过反射获取LoggerConfig的config字段值
     *
     * @param loggerConfig LoggerConfig对象
     * @return config字段的值，如果获取失败返回null
     */
    private static Object getConfigField(LoggerConfig loggerConfig) {
        try {
            // 如果configField已初始化，则通过反射获取其值
            if (configField != null) {
                return configField.get(loggerConfig);
            }
        } catch (Throwable e) {
            // ignore 忽略反射访问异常
        }
        return null;
    }

    /**
     * 获取Logger的详细信息
     * <p>
     * 该方法将LoggerConfig的各种信息提取并封装到一个Map中，
     * 包括名称、类信息、代码源、配置对象、可加性、日志级别和Appender信息等。
     * </p>
     *
     * @param loggerConfig LoggerConfig对象
     * @return 包含Logger详细信息的Map
     */
    private static Map<String, Object> doGetLoggerInfo(LoggerConfig loggerConfig) {
        // 创建用于存储Logger信息的Map
        Map<String, Object> info = new HashMap<String, Object>();

        // 获取Logger的名称
        String name = loggerConfig.getName();
        // 如果名称为空，则设置为ROOT
        if (name == null || name.trim().isEmpty()) {
            name = LoggerConfig.ROOT;
        }

        // 添加Logger的基本信息
        info.put(LoggerHelper.name, name);                          // Logger名称
        info.put(LoggerHelper.clazz, loggerConfig.getClass());     // Logger类信息
        // 获取类的代码源位置
        CodeSource codeSource = loggerConfig.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            info.put(LoggerHelper.codeSource, codeSource.getLocation());  // 代码源位置
        }
        // 通过反射获取config对象
        Object config = getConfigField(loggerConfig);
        if (config != null) {
            info.put(LoggerHelper.config, config);                  // 配置对象
        }

        // 添加Logger的可加性（是否继承父Logger的配置）
        info.put(LoggerHelper.additivity, loggerConfig.isAdditive());

        // 添加Logger的日志级别
        Level level = loggerConfig.getLevel();
        if (level != null) {
            info.put(LoggerHelper.level, level.toString());         // 日志级别
        }

        // 获取并添加Logger的Appender信息
        List<Map<String, Object>> result = doGetLoggerAppenders(loggerConfig);
        info.put(LoggerHelper.appenders, result);                  // Appender列表
        return info;
    }

    /**
     * 获取Logger的所有Appender信息
     * <p>
     * 该方法遍历Logger的所有Appender，并将每个Appender的信息封装到Map中。
     * 支持FileAppender、ConsoleAppender和AsyncAppender三种类型的特殊处理。
     * </p>
     *
     * @param loggerConfig LoggerConfig对象
     * @return 包含所有Appender信息的列表
     */
    private static List<Map<String, Object>> doGetLoggerAppenders(LoggerConfig loggerConfig) {
        // 创建用于存储Appender信息的列表
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        // 获取Logger的所有Appender
        Map<String, Appender> appenders = loggerConfig.getAppenders();

        // 遍历所有Appender
        for (Entry<String, Appender> entry : appenders.entrySet()) {
            // 创建用于存储单个Appender信息的Map
            Map<String, Object> info = new HashMap<String, Object>();
            Appender appender = entry.getValue();
            // 添加Appender的基本信息
            info.put(LoggerHelper.name, appender.getName());        // Appender名称
            info.put(LoggerHelper.clazz, appender.getClass());      // Appender类信息

            result.add(info);
            // 根据Appender类型添加特定的信息
            if (appender instanceof FileAppender) {
                // 文件Appender：添加文件路径
                info.put(LoggerHelper.file, ((FileAppender) appender).getFileName());
            } else if (appender instanceof ConsoleAppender) {
                // 控制台Appender：添加输出目标（SYSTEM_OUT或SYSTEM_ERR）
                info.put(LoggerHelper.target, ((ConsoleAppender) appender).getTarget());
            } else if (appender instanceof AsyncAppender) {
                // 异步Appender：添加阻塞标志和引用的Appender列表

                AsyncAppender asyncAppender = ((AsyncAppender) appender);
                // 获取异步Appender引用的所有Appender名称
                String[] appenderRefStrings = asyncAppender.getAppenderRefStrings();

                // 添加异步Appender的特定信息
                info.put(LoggerHelper.blocking, asyncAppender.isBlocking());                    // 是否阻塞
                info.put(LoggerHelper.appenderRef, Arrays.asList(appenderRefStrings));          // 引用的Appender列表
            }
        }
        return result;
    }

    /**
     * 判断对象是否为空
     *
     * @param str 要检查的对象
     * @return 如果对象为null或空字符串则返回true，否则返回false
     */
    private static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }
}
