package com.taobao.arthas.core.command.logger;

import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Log4j日志框架助手类
 * <p>
 * 该类提供了一系列静态方法用于操作和管理Log4j 1.x日志框架。
 * 主要功能包括：
 * 1. 检测Log4j是否可用
 * 2. 动态更新日志级别
 * 3. 获取Logger配置信息
 * 4. 获取Appender信息
 * </p>
 *
 * @author hengyunabc 2019-09-06
 *
 */
public class Log4jHelper {

    /**
     * 标识Log4j是否可用
     * 在静态初始化块中检测Log4j是否存在并设置此标志
     */
    private static boolean Log4j = false;

    /**
     * 静态初始化块
     * 在类加载时执行，用于检测Log4j是否可用
     */
    static {
        try {
            // 尝试加载Log4j的Logger类
            Class<?> loggerClass = Log4jHelper.class.getClassLoader().loadClass("org.apache.log4j.Logger");
            // 这里可能会加载到其它上游ClassLoader的log4j，因此需要判断是否当前classloader
            if (loggerClass.getClassLoader().equals(Log4jHelper.class.getClassLoader())) {
                Log4j = true;
            }
        } catch (Throwable t) {
            // ignore 忽略类加载失败的异常
        }
    }

    /**
     * 更新指定Logger的日志级别
     * <p>
     * 该方法用于动态修改Log4j中指定Logger的日志级别。
     * 支持普通Logger和root logger的日志级别修改。
     * </p>
     *
     * @param name  Logger的名称
     * @param level 要设置的日志级别（如TRACE、DEBUG、INFO、WARN、ERROR等）
     * @return 如果成功返回true，如果Logger不存在返回false，如果Log4j不可用返回null
     */
    public static Boolean updateLevel(String name, String level) {
        // 检查Log4j是否可用
        if (Log4j) {
            // 将日志级别字符串转换为Level对象，如果转换失败则默认使用ERROR级别
            Level l = Level.toLevel(level, Level.ERROR);
            // 尝试获取指定名称的Logger
            Logger logger = LogManager.getLoggerRepository().exists(name);
            if (logger != null) {
                // 如果Logger存在，设置其日志级别
                logger.setLevel(l);
                return true;
            } else {
                // 如果Logger不存在，检查是否是root logger
                Logger root = LogManager.getLoggerRepository().getRootLogger();
                if (root.getName().equals(name)) {
                    // 如果是root logger，设置其日志级别
                    root.setLevel(l);
                    return true;
                }
            }
            // Logger不存在且不是root logger，返回false
            return false;
        }
        // Log4j不可用时返回null
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
        // 如果Log4j不可用，直接返回空Map
        if (!Log4j) {
            return loggerInfoMap;
        }

        // 如果指定了Logger名称
        if (name != null && !name.trim().isEmpty()) {
            // 尝试获取指定名称的Logger
            Logger logger = LogManager.getLoggerRepository().exists(name);
            if (logger != null) {
                // 如果Logger存在，将其信息添加到结果Map中
                loggerInfoMap.put(name, doGetLoggerInfo(logger));
            }
        } else {
            // 获取所有logger时，如果没有appender则忽略（根据includeNoAppender参数决定）
            @SuppressWarnings("unchecked")
            // 获取当前所有的Logger（不包括root logger）
            Enumeration<Logger> loggers = LogManager.getLoggerRepository().getCurrentLoggers();

            if (loggers != null) {
                // 遍历所有Logger
                while (loggers.hasMoreElements()) {
                    Logger logger = loggers.nextElement();
                    // 获取Logger的详细信息
                    Map<String, Object> info = doGetLoggerInfo(logger);
                    // 如果不包含没有Appender的Logger，则只添加有Appender的Logger
                    if (!includeNoAppender) {
                        List<?> appenders = (List<?>) info.get(LoggerHelper.appenders);
                        if (appenders != null && !appenders.isEmpty()) {
                            loggerInfoMap.put(logger.getName(), info);
                        }
                    } else {
                        // 包含所有Logger，无论是否有Appender
                        loggerInfoMap.put(logger.getName(), info);
                    }
                }
            }

            // 获取root logger的信息
            Logger root = LogManager.getLoggerRepository().getRootLogger();
            if (root != null) {
                // 获取root logger的详细信息
                Map<String, Object> info = doGetLoggerInfo(root);
                // 如果不包含没有Appender的Logger，则只检查是否有Appender
                if (!includeNoAppender) {
                    List<?> appenders = (List<?>) info.get(LoggerHelper.appenders);
                    if (appenders != null && !appenders.isEmpty()) {
                        loggerInfoMap.put(root.getName(), info);
                    }
                } else {
                    // 包含root logger，无论是否有Appender
                    loggerInfoMap.put(root.getName(), info);
                }
            }
        }

        return loggerInfoMap;
    }

    /**
     * 获取Logger的详细信息
     * <p>
     * 该方法将Logger的各种信息提取并封装到一个Map中，
     * 包括名称、类信息、代码源、可加性、日志级别、有效日志级别和Appender信息等。
     * </p>
     *
     * @param logger Logger对象
     * @return 包含Logger详细信息的Map
     */
    private static Map<String, Object> doGetLoggerInfo(Logger logger) {
        // 创建用于存储Logger信息的Map
        Map<String, Object> info = new HashMap<String, Object>();
        // 添加Logger的基本信息
        info.put(LoggerHelper.name, logger.getName());              // Logger名称
        info.put(LoggerHelper.clazz, logger.getClass());            // Logger类信息
        // 获取类的代码源位置
        CodeSource codeSource = logger.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            info.put(LoggerHelper.codeSource, codeSource.getLocation());  // 代码源位置
        }
        // 添加Logger的可加性（是否继承父Logger的配置）
        info.put(LoggerHelper.additivity, logger.getAdditivity());

        // 获取Logger的日志级别和有效日志级别
        Level level = logger.getLevel(), effectiveLevel = logger.getEffectiveLevel();
        if (level != null) {
            info.put(LoggerHelper.level, level.toString());        // 配置的日志级别
        }
        if (effectiveLevel != null) {
            info.put(LoggerHelper.effectiveLevel, effectiveLevel.toString());  // 实际生效的日志级别
        }

        // 获取并添加Logger的Appender信息
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = doGetLoggerAppenders(logger.getAllAppenders());
        info.put(LoggerHelper.appenders, result);                  // Appender列表
        return info;
    }

    /**
     * 获取Logger的所有Appender信息
     * <p>
     * 该方法遍历Logger的所有Appender，并将每个Appender的信息封装到Map中。
     * 支持FileAppender、ConsoleAppender和AsyncAppender三种类型的特殊处理。
     * 对于AsyncAppender，会递归获取其引用的所有Appender信息。
     * </p>
     *
     * @param appenders Appender的枚举集合
     * @return 包含所有Appender信息的列表
     */
    private static List<Map<String, Object>> doGetLoggerAppenders(Enumeration<Appender> appenders) {
        // 创建用于存储Appender信息的列表
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        // 如果Appender枚举为空，直接返回空列表
        if (appenders == null) {
            return result;
        }

        // 遍历所有Appender
        while (appenders.hasMoreElements()) {
            // 创建用于存储单个Appender信息的Map
            Map<String, Object> info = new HashMap<String, Object>();
            Appender appender = appenders.nextElement();

            // 添加Appender的基本信息
            info.put(LoggerHelper.name, appender.getName());        // Appender名称
            info.put(LoggerHelper.clazz, appender.getClass());      // Appender类信息

            result.add(info);
            // 根据Appender类型添加特定的信息
            if (appender instanceof FileAppender) {
                // 文件Appender：添加文件路径
                info.put(LoggerHelper.file, ((FileAppender) appender).getFile());
            } else if (appender instanceof ConsoleAppender) {
                // 控制台Appender：添加输出目标（System.out或System.err）
                info.put(LoggerHelper.target, ((ConsoleAppender) appender).getTarget());
            } else if (appender instanceof AsyncAppender) {
                // 异步Appender：需要获取其引用的所有Appender
                @SuppressWarnings("unchecked")
                // 获取异步Appender引用的所有Appender
                Enumeration<Appender> appendersOfAsync = ((AsyncAppender) appender).getAllAppenders();
                if (appendersOfAsync != null) {
                    // 递归获取异步Appender引用的所有Appender信息
                    List<Map<String, Object>> asyncs = doGetLoggerAppenders(appendersOfAsync);
                    // 标明异步appender
                    List<String> appenderRef = new ArrayList<String>();
                    // 遍历异步Appender引用的所有Appender
                    for (Map<String, Object> a : asyncs) {
                        // 将Appender名称添加到引用列表
                        appenderRef.add((String) a.get(LoggerHelper.name));
                        // 将Appender信息也添加到结果列表中
                        result.add(a);
                    }
                    // 添加异步Appender的特定信息
                    info.put(LoggerHelper.blocking, ((AsyncAppender) appender).getBlocking());    // 是否阻塞
                    info.put(LoggerHelper.appenderRef, appenderRef);                             // 引用的Appender名称列表
                }
            }
        }

        return result;
    }

}
