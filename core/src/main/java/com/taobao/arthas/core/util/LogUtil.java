package com.taobao.arthas.core.util;

import java.io.File;
import java.util.Iterator;

import com.alibaba.arthas.deps.ch.qos.logback.classic.LoggerContext;
import com.alibaba.arthas.deps.ch.qos.logback.classic.joran.JoranConfigurator;
import com.alibaba.arthas.deps.ch.qos.logback.classic.spi.ILoggingEvent;
import com.alibaba.arthas.deps.ch.qos.logback.core.Appender;
import com.alibaba.arthas.deps.ch.qos.logback.core.rolling.RollingFileAppender;
import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.AnsiLog;
import com.taobao.arthas.core.env.ArthasEnvironment;

/**
 * 
 * @author hengyunabc
 *
 */
public class LogUtil {

    public static final String LOGGING_CONFIG_PROPERTY = "arthas.logging.config";
    public static final String LOGGING_CONFIG = "${arthas.logging.config:${arthas.home}/logback.xml}";

    /**
     * The name of the property that contains the name of the log file. Names can be
     * an exact location or relative to the current directory.
     */
    public static final String FILE_NAME_PROPERTY = "arthas.logging.file.name";
    public static final String ARTHAS_LOG_FILE = "ARTHAS_LOG_FILE";

    /**
     * The name of the property that contains the directory where log files are
     * written.
     */
    public static final String FILE_PATH_PROPERTY = "arthas.logging.file.path";
    public static final String ARTHAS_LOG_PATH = "ARTHAS_LOG_PATH";

    private static String logFile = "";

    /**
     * <pre>
     * 1. 尝试从 arthas.logging.config 这个配置里加载 logback.xml
     * 2. 尝试从 arthas.home 下面找 logback.xml
     * 
     * 可以用 arthas.logging.file.name 指定具体arthas.log的名字
     * 可以用 arthas.logging.file.path 指定具体arthas.log的目录
     * 
     * </pre>
     * 
     * @param env
     */
    public static LoggerContext initLooger(ArthasEnvironment env) {
        String loggingConfig = env.resolvePlaceholders(LOGGING_CONFIG);
        if (loggingConfig == null || loggingConfig.trim().isEmpty()) {
            return null;
        }
        AnsiLog.debug("arthas logging file: " + loggingConfig);
        File configFile = new File(loggingConfig);
        if (!configFile.isFile()) {
            AnsiLog.error("can not find arthas logging config: " + loggingConfig);
            return null;
        }

        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();

            String fileName = env.getProperty(FILE_NAME_PROPERTY);
            if (fileName != null) {
                loggerContext.putProperty(ARTHAS_LOG_FILE, fileName);
            }
            String filePath = env.getProperty(FILE_PATH_PROPERTY);
            if (filePath != null) {
                loggerContext.putProperty(ARTHAS_LOG_PATH, filePath);
            }

            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(configFile.toURI().toURL()); // load logback xml file

            // 查找 arthas.log appender
            Iterator<Appender<ILoggingEvent>> appenders = loggerContext.getLogger("root").iteratorForAppenders();

            while (appenders.hasNext()) {
                Appender<ILoggingEvent> appender = appenders.next();
                if (appender instanceof RollingFileAppender) {
                    RollingFileAppender fileAppender = (RollingFileAppender) appender;
                    if ("ARTHAS".equalsIgnoreCase(fileAppender.getName())) {
                        logFile = fileAppender.getFile();
                    }
                }
            }

            return loggerContext;
        } catch (Throwable e) {
            AnsiLog.error("try to load arthas logging config file error: " + configFile, e);
        }
        return null;
    }

    public static String loggingFile() {
        if (logFile == null || logFile.trim().isEmpty()) {
            return "arthas.log";
        }
        return logFile;
    }

    public static String loggingDir() {
        if (logFile != null && !logFile.isEmpty()) {
            String parent = new File(logFile).getParent();
            if (parent != null) {
                return parent;
            }
        }
        return new File("").getAbsolutePath();
    }

    public static String cacheDir() {
        File logsDir = new File(loggingDir()).getParentFile();
        if (logsDir.exists()) {
            File arthasCacheDir = new File(logsDir, "arthas-cache");
            arthasCacheDir.mkdirs();
            return arthasCacheDir.getAbsolutePath();
        } else {
            File arthasCacheDir = new File("arthas-cache");
            arthasCacheDir.mkdirs();
            return arthasCacheDir.getAbsolutePath();
        }
    }

    public static Logger getResultLogger() {
        return LoggerFactory.getLogger("result");
    }
}
