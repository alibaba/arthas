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
 * 
 * @author hengyunabc 2019-09-06
 *
 */
public class Log4jHelper {

    private static boolean Log4j = false;

    static {
        try {
            Class<?> loggerClass = Log4jHelper.class.getClassLoader().loadClass("org.apache.log4j.Logger");
            // 这里可能会加载到其它上游ClassLoader的log4j，因此需要判断是否当前classloader
            if (loggerClass.getClassLoader().equals(Log4jHelper.class.getClassLoader())) {
                Log4j = true;
            }
        } catch (Throwable t) {
        }
    }

    public static Boolean updateLevel(String name, String level) {
        if (Log4j) {
            Level l = Level.toLevel(level, Level.ERROR);
            Logger logger = LogManager.getLoggerRepository().exists(name);
            if (logger != null) {
                logger.setLevel(l);
                return true;
            } else {
                Logger root = LogManager.getLoggerRepository().getRootLogger();
                if (root.getName().equals(name)) {
                    root.setLevel(l);
                    return true;
                }
            }
            return false;
        }
        return null;
    }

    public static Map<String, Map<String, Object>> getLoggers(String name, boolean includeNoAppender) {
        Map<String, Map<String, Object>> loggerInfoMap = new HashMap<String, Map<String, Object>>();
        if (!Log4j) {
            return loggerInfoMap;
        }

        if (name != null && !name.trim().isEmpty()) {
            Logger logger = LogManager.getLoggerRepository().exists(name);
            if (logger != null) {
                loggerInfoMap.put(name, doGetLoggerInfo(logger));
            }
        } else {
            // 获取所有logger时，如果没有appender则忽略
            @SuppressWarnings("unchecked")
            Enumeration<Logger> loggers = LogManager.getLoggerRepository().getCurrentLoggers();

            if (loggers != null) {
                while (loggers.hasMoreElements()) {
                    Logger logger = loggers.nextElement();
                    Map<String, Object> info = doGetLoggerInfo(logger);
                    if (!includeNoAppender) {
                        List<?> appenders = (List<?>) info.get(LoggerHelper.appenders);
                        if (appenders != null && !appenders.isEmpty()) {
                            loggerInfoMap.put(logger.getName(), info);
                        }
                    } else {
                        loggerInfoMap.put(logger.getName(), info);
                    }
                }
            }

            Logger root = LogManager.getLoggerRepository().getRootLogger();
            if (root != null) {
                Map<String, Object> info = doGetLoggerInfo(root);
                if (!includeNoAppender) {
                    List<?> appenders = (List<?>) info.get(LoggerHelper.appenders);
                    if (appenders != null && !appenders.isEmpty()) {
                        loggerInfoMap.put(root.getName(), info);
                    }
                } else {
                    loggerInfoMap.put(root.getName(), info);
                }
            }
        }

        return loggerInfoMap;
    }

    private static Map<String, Object> doGetLoggerInfo(Logger logger) {
        Map<String, Object> info = new HashMap<String, Object>();
        info.put(LoggerHelper.name, logger.getName());
        info.put(LoggerHelper.clazz, logger.getClass());
        CodeSource codeSource = logger.getClass().getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            info.put(LoggerHelper.codeSource, codeSource.getLocation());
        }
        info.put(LoggerHelper.additivity, logger.getAdditivity());

        Level level = logger.getLevel(), effectiveLevel = logger.getEffectiveLevel();
        if (level != null) {
            info.put(LoggerHelper.level, level.toString());
        }
        if (effectiveLevel != null) {
            info.put(LoggerHelper.effectiveLevel, effectiveLevel.toString());
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = doGetLoggerAppenders(logger.getAllAppenders());
        info.put(LoggerHelper.appenders, result);
        return info;
    }

    private static List<Map<String, Object>> doGetLoggerAppenders(Enumeration<Appender> appenders) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        if (appenders == null) {
            return result;
        }

        while (appenders.hasMoreElements()) {
            Map<String, Object> info = new HashMap<String, Object>();
            Appender appender = appenders.nextElement();

            info.put(LoggerHelper.name, appender.getName());
            info.put(LoggerHelper.clazz, appender.getClass());

            result.add(info);
            if (appender instanceof FileAppender) {
                info.put(LoggerHelper.file, ((FileAppender) appender).getFile());
            } else if (appender instanceof ConsoleAppender) {
                info.put(LoggerHelper.target, ((ConsoleAppender) appender).getTarget());
            } else if (appender instanceof AsyncAppender) {
                @SuppressWarnings("unchecked")
                Enumeration<Appender> appendersOfAsync = ((AsyncAppender) appender).getAllAppenders();
                if (appendersOfAsync != null) {
                    List<Map<String, Object>> asyncs = doGetLoggerAppenders(appendersOfAsync);
                    // 标明异步appender
                    List<String> appenderRef = new ArrayList<String>();
                    for (Map<String, Object> a : asyncs) {
                        appenderRef.add((String) a.get(LoggerHelper.name));
                        result.add(a);
                    }
                    info.put(LoggerHelper.blocking, ((AsyncAppender) appender).getBlocking());
                    info.put(LoggerHelper.appenderRef, appenderRef);
                }
            }
        }

        return result;
    }

}
