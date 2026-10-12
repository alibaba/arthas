package com.taobao.arthas.core.command.logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * date: 2020/04/27
 *
 * @author qxo
 */
public class Slf4jSimpleHelper {

  private static boolean LOG_ON = false;
  private static Field configMapField = null;
  private static Field levelField = null;
  private static Method stringToLevelField = null;
  private static ILoggerFactory loggerFactoryInstance;

  static {
    try {
      Class<?> loggerClass =
              Slf4jSimpleHelper.class.getClassLoader().loadClass("org.slf4j.impl.SimpleLoggerFactory");
      // 这里可能会加载到其它上游ClassLoader的log4j2，因此需要判断是否当前classloader
      if (loggerClass.getClassLoader().equals(Slf4jSimpleHelper.class.getClassLoader())) {

        try {
          configMapField = loggerClass.getDeclaredField("loggerMap");
          configMapField.setAccessible(true);

          final ClassLoader loader = loggerClass.getClassLoader();
          loggerFactoryInstance = org.slf4j.LoggerFactory.getILoggerFactory();

          levelField =
              loader.loadClass("org.slf4j.impl.SimpleLogger").getDeclaredField("currentLogLevel");
          levelField.setAccessible(true);

          stringToLevelField = loader.loadClass("org.slf4j.impl.SimpleLoggerConfiguration")
                  .getDeclaredMethod("stringToLevel", String.class);
          stringToLevelField.setAccessible(true);
          LOG_ON = true;
        } catch (Throwable e) {
          // ignore
        }
      }

    } catch (Throwable t) {
    }
  }

  public Slf4jSimpleHelper() {
  }

  public static Boolean updateLevel(String name, String level) {
    if (LOG_ON) {
      try {
        // SimpleLoggerConfiguration.stringToLevel(levelString)
        Object v = stringToLevelField.invoke(null, level);
        final Map<String, Logger> map = (Map) configMapField.get(loggerFactoryInstance);
        Logger logger = (Logger) map.get(name);
//        if (logger == null) {
//            logger = loggerFactoryInstance.getLogger(name);
//        }
//        System.out.println("configMapField:"+map); 
        if (logger != null) {
          levelField.set(logger, v);
          return true;
        }
      } catch (Throwable t) {
          // ignore
          t.printStackTrace(System.out);
      }
      return false;
    }
    return null;
  }

  private static Map<String, Object> doGetLoggerInfo(Logger logger) {
    Map<String, Object> info = new LinkedHashMap<String, Object>();
    info.put(LoggerHelper.name, logger.getName());
    info.put(LoggerHelper.clazz, logger.getClass());
    CodeSource codeSource = logger.getClass().getProtectionDomain().getCodeSource();
    if (codeSource != null) {
      info.put(LoggerHelper.codeSource, codeSource.getLocation());
    }
    String level = stringToLevel(logger);
    if (level != null) {
      info.put(LoggerHelper.level, level.toString());
    }
    return info;
  }

  static String stringToLevel(Logger logger) {
    String levelStr = null;
    if (logger.isErrorEnabled()) {
      levelStr = "error";
    } else if (logger.isWarnEnabled()) {
      levelStr = "warn";
    } else if (logger.isInfoEnabled()) {
      levelStr = "info";
    } else if (logger.isDebugEnabled()) {
      levelStr = "debug";
    } else if (logger.isTraceEnabled()) {
      levelStr = "debug";
    }
    return levelStr;
  }

  public static Map<String, Map<String, Object>> getLoggers(
      String name, boolean includeNoAppender) {
    Map<String, Map<String, Object>> loggerInfoMap =
        new LinkedHashMap<String, Map<String, Object>>();
    if (LOG_ON) {
      try {
        final Map<String, Logger> map = (Map) configMapField.get(loggerFactoryInstance);
        if (name != null && !name.isEmpty()) {
          final Logger logger = (Logger) map.get(name);
          if (logger != null) {
            loggerInfoMap.put(name, doGetLoggerInfo(logger));
          }
        } else {
          for (Logger logger : map.values()) {
            Map<String, Object> info = doGetLoggerInfo(logger);
            loggerInfoMap.put(logger.getName(), info);
          }
        }
      } catch (Throwable t) {
          t.printStackTrace(System.out);
      }
    }
    return loggerInfoMap;
  }
}
