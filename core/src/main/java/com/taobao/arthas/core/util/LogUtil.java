package com.taobao.arthas.core.util;

import com.taobao.middleware.logger.Level;
import com.taobao.middleware.logger.Logger;
import com.taobao.middleware.logger.LoggerFactory;
import com.taobao.middleware.logger.support.LogLog;
import com.taobao.middleware.logger.support.LoggerHelper;

/**
 * Arthas日志
 * Created by vlinux on 15/3/8.
 */
public class LogUtil {

    /**
     * Arthas 内部日志Logger
     */
    private static final Logger arthasLogger;

    private static final org.slf4j.Logger resultLogger;

    /**
     * 接管Netty的Logger
     */
    private static final Logger nettyLogger;

    /**
     * 接管termd的Logger
     */
    private static final Logger termdLogger;

    public static final String LOGGER_FILE = LoggerHelper.getLogFile("arthas", "arthas.log");

    static {
        LogLog.setQuietMode(true);

        LoggerHelper.setPattern("arthas-cache", "%d{yyyy-MM-dd HH:mm:ss.SSS}%n%m%n");

        arthasLogger = LoggerFactory.getLogger("arthas");
        arthasLogger.activateAppenderWithTimeAndSizeRolling("arthas", "arthas.log", "UTF-8", "100MB");
        arthasLogger.setLevel(Level.INFO);
        arthasLogger.setAdditivity(false);

        com.taobao.middleware.logger.Logger log = LoggerFactory.getLogger("result");
        log.activateAppenderWithSizeRolling("arthas-cache", "result.log", "UTF-8", "100MB", 3);
        log.setAdditivity(false);
        log.activateAsync(64, -1);
        resultLogger = (org.slf4j.Logger) log.getDelegate();

        nettyLogger = LoggerFactory.getLogger("io.netty");
        nettyLogger.activateAppender(arthasLogger);
        nettyLogger.setLevel(Level.INFO);
        nettyLogger.setAdditivity(false);

        termdLogger = LoggerFactory.getLogger("io.termd");
        termdLogger.activateAppender(arthasLogger);
        termdLogger.setLevel(Level.INFO);
        termdLogger.setAdditivity(false);
    }

    public static Logger getArthasLogger() {
        return arthasLogger;
    }

    public static org.slf4j.Logger getResultLogger() {
        return resultLogger;
    }

    public static void closeResultLogger() {
        closeSlf4jLogger(resultLogger);
    }

    public static void closeSlf4jLogger(org.slf4j.Logger logger) {
        if (logger != null) {
            if (logger instanceof ch.qos.logback.classic.Logger) {
                ((ch.qos.logback.classic.Logger) logger).detachAndStopAllAppenders();
            } else {
                // arthas strongly depends on logback.
                // So do nothing here
                // https://github.com/alibaba/arthas/issues/319
            }
        }
    }
}
