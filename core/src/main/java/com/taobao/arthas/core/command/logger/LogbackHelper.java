package com.taobao.arthas.core.command.logger;

import java.lang.reflect.Field;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.ILoggerFactory;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.pattern.PatternLayoutBase;

/**
 * Logback日志助手类
 *
 * 用于在Arthas中操作和查询Logback日志框架的Logger信息。
 * 提供更新日志级别和获取Logger详细信息的功能。
 *
 * @author hengyunabc 2019-09-06
 *
 */
public class LogbackHelper {

    /**
     * 标识当前环境是否使用了Logback日志框架
     */
    private static boolean Logback = false;

    /**
     * 反射字段：PatternLayoutBase的head字段，用于访问日志格式化器的头部信息
     */
    private static Field headField;

    /**
     * 反射字段：ThrowableProxyConverter的lengthOption字段，用于控制异常堆栈的输出长度
     */
    private static Field lengthOptionField;

    /**
     * Logger工厂实例，用于获取和管理Logger对象
     */
    private static ILoggerFactory loggerFactoryInstance;

    /**
     * 静态初始化块
     * 检测当前运行环境是否使用Logback日志框架，并进行必要的初始化
     */
    static {
        try {
            // 尝试加载Logback的Logger类
            Class<?> loggerClass = LogbackHelper.class.getClassLoader().loadClass("ch.qos.logback.classic.Logger");
            // 这里可能会加载到应用中依赖的logback，因此需要判断classloader
            // 确保加载的Logback类是由同一个类加载器加载的，避免类加载器隔离问题
            if (loggerClass.getClassLoader().equals(LogbackHelper.class.getClassLoader())) {
                // 获取SLF4J的LoggerFactory实例
                ILoggerFactory loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory();

                // 验证LoggerFactory是否是Logback的实现
                if (loggerFactory instanceof LoggerContext) {
                    loggerFactoryInstance = loggerFactory;

                    // 通过反射获取PatternLayoutBase的head字段，用于后续可能的格式化操作
                    headField = PatternLayoutBase.class.getDeclaredField("head");
                    headField.setAccessible(true);

                    // 通过反射获取ThrowableProxyConverter的lengthOption字段，用于控制异常堆栈输出长度
                    lengthOptionField = ThrowableProxyConverter.class.getDeclaredField("lengthOption");
                    lengthOptionField.setAccessible(true);

                    // 标记Logback可用
                    Logback = true;
                }
            }
        } catch (Throwable t) {
            // ignore - 忽略所有异常，如果Logback不可用则保持Logback为false
        }
    }

    /**
     * 更新指定Logger的日志级别
     *
     * @param name Logger的名称，如"ROOT"或具体的类名
     * @param level 要设置的日志级别，如"DEBUG"、"INFO"、"WARN"、"ERROR"
     * @return 更新成功返回true，更新失败返回false，如果Logback不可用返回null
     */
    public static Boolean updateLevel(String name, String level) {
        // 检查Logback是否可用
        if (Logback) {
            try {
                // 将字符串级别的日志级别转换为Logback的Level对象
                // 如果传入的级别无效，默认使用ERROR级别
                Level l = Level.toLevel(level, Level.ERROR);

                // 获取Logger上下文
                LoggerContext loggerContext = (LoggerContext) loggerFactoryInstance;

                // 查找指定名称的Logger
                Logger logger = loggerContext.exists(name);
                if (logger != null) {
                    // 设置Logger的日志级别
                    logger.setLevel(l);
                    return true;
                }
            } catch (Throwable t) {
                // ignore - 忽略所有异常，返回false表示更新失败
            }
            return false;
        }
        // Logback不可用，返回null
        return null;
    }

    /**
     * 获取Logger信息
     *
     * @param name Logger名称，如果为null或空字符串，则获取所有Logger
     * @param includeNoAppender 是否包含没有Appender的Logger
     * @return Logger信息映射表，key为Logger名称，value为Logger的详细信息
     */
    public static Map<String, Map<String, Object>> getLoggers(String name, boolean includeNoAppender) {
        // 使用LinkedHashMap保持插入顺序
        Map<String, Map<String, Object>> loggerInfoMap = new LinkedHashMap<String, Map<String, Object>>();

        // 检查Logback是否可用
        if (Logback) {
            LoggerContext loggerContext = (LoggerContext) loggerFactoryInstance;

            // 如果指定了Logger名称
            if (name != null && !name.trim().isEmpty()) {
                // 查找指定名称的Logger
                Logger logger = loggerContext.exists(name);
                if (logger != null) {
                    // 获取Logger信息并放入结果Map
                    loggerInfoMap.put(name, doGetLoggerInfo(logger));
                }
            } else {
                // 获取所有logger时，如果没有appender则忽略（根据参数决定）
                List<Logger> loggers = loggerContext.getLoggerList();
                for (Logger logger : loggers) {
                    // 获取当前Logger的详细信息
                    Map<String, Object> info = doGetLoggerInfo(logger);

                    // 根据参数决定是否包含没有Appender的Logger
                    if (!includeNoAppender) {
                        // 获取Logger的Appender列表
                        List<?> appenders = (List<?>) info.get(LoggerHelper.appenders);
                        // 只有当Logger有Appender时才添加到结果中
                        if (appenders != null && !appenders.isEmpty()) {
                            loggerInfoMap.put(logger.getName(), info);
                        }
                    } else {
                        // 包含所有Logger，无论是否有Appender
                        loggerInfoMap.put(logger.getName(), info);
                    }

                }
            }
        }

        return loggerInfoMap;
    }

    /**
     * 获取单个Logger的详细信息
     *
     * @param logger 要查询的Logger对象
     * @return 包含Logger详细信息的Map
     */
    private static Map<String, Object> doGetLoggerInfo(Logger logger) {
        // 使用LinkedHashMap保持插入顺序
        Map<String, Object> info = new LinkedHashMap<String, Object>();

        // 添加Logger名称
        info.put(LoggerHelper.name, logger.getName());

        // 添加Logger的Class对象
        info.put(LoggerHelper.clazz, logger.getClass());

        // 获取Logger类的代码源位置
        CodeSource codeSource = logger.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            // 如果代码源存在，添加其位置信息（jar包路径或class文件路径）
            info.put(LoggerHelper.codeSource, codeSource.getLocation());
        }

        // 添加Logger的additivity属性（是否继承父Logger的Appender）
        info.put(LoggerHelper.additivity, logger.isAdditive());

        // 获取Logger的配置级别和实际有效级别
        Level level = logger.getLevel(), effectiveLevel = logger.getEffectiveLevel();
        if (level != null) {
            // 如果Logger有明确配置的级别，添加到信息中
            info.put(LoggerHelper.level, level.toString());
        }
        if (effectiveLevel != null) {
            // 添加实际有效的日志级别（可能继承自父Logger）
            info.put(LoggerHelper.effectiveLevel, effectiveLevel.toString());
        }

        // 获取Logger的所有Appender信息
        List<Map<String, Object>> result = doGetLoggerAppenders(logger.iteratorForAppenders());
        info.put(LoggerHelper.appenders, result);

        return info;
    }

    /**
     * 获取Logger的所有Appender信息
     *
     * @param appenders Appender迭代器
     * @return Appender信息列表
     */
    @SuppressWarnings("rawtypes")
    private static List<Map<String, Object>> doGetLoggerAppenders(Iterator<Appender<ILoggingEvent>> appenders) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        // 遍历所有Appender
        while (appenders.hasNext()) {
            // 使用LinkedHashMap保持插入顺序
            Map<String, Object> info = new LinkedHashMap<String, Object>();

            // 获取当前Appender
            Appender<ILoggingEvent> appender = appenders.next();

            // 添加Appender名称
            info.put(LoggerHelper.name, appender.getName());

            // 添加Appender的Class对象
            info.put(LoggerHelper.clazz, appender.getClass());

            // 根据不同类型的Appender，添加特定的信息
            if (appender instanceof FileAppender) {
                // 文件Appender：添加文件路径
                info.put(LoggerHelper.file, ((FileAppender) appender).getFile());
            } else if (appender instanceof AsyncAppender) {
                // 异步Appender：获取其引用的其他Appender信息
                AsyncAppender aa = (AsyncAppender) appender;

                // 获取异步Appender内部的所有Appender
                Iterator<Appender<ILoggingEvent>> iter = aa.iteratorForAppenders();
                List<Map<String, Object>> asyncs = doGetLoggerAppenders(iter);

                // 异步appender所 ref的 appender，参考： https://logback.qos.ch/manual/appenders.html
                List<String> appenderRef = new ArrayList<String>();
                for (Map<String, Object> a : asyncs) {
                    // 收集被引用的Appender名称
                    appenderRef.add((String) a.get(LoggerHelper.name));
                    // 将被引用的Appender信息也添加到结果中
                    result.add(a);
                }

                // 添加Appender引用列表
                info.put(LoggerHelper.appenderRef, appenderRef);

                // 添加是否阻塞的标识（!neverBlock表示可能阻塞）
                info.put(LoggerHelper.blocking, !aa.isNeverBlock());
            } else if (appender instanceof ConsoleAppender) {
                // 控制台Appender：添加输出目标（System.out或System.err）
                info.put(LoggerHelper.target, ((ConsoleAppender) appender).getTarget());
            }

            // 将当前Appender信息添加到结果列表
            result.add(info);
        }

        return result;
    }
}
