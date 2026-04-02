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
 * 日志工具类
 * 负责初始化和管理Arthas的日志系统，基于Logback实现
 *
 * @author hengyunabc
 *
 */
public class LogUtil {

    /**
     * 日志配置文件属性名
     * 用于指定logback配置文件的位置
     */
    public static final String LOGGING_CONFIG_PROPERTY = "arthas.logging.config";

    /**
     * 日志配置文件路径的默认值
     * 优先使用arthas.logging.config配置，如果未配置则使用${arthas.home}/logback.xml
     */
    public static final String LOGGING_CONFIG = "${arthas.logging.config:${arthas.home}/logback.xml}";

    /**
     * 日志文件名称属性名
     * 可以是绝对路径或相对路径
     */
    public static final String FILE_NAME_PROPERTY = "arthas.logging.file.name";

    /**
     * 日志文件名称在Logback配置中的变量名
     */
    public static final String ARTHAS_LOG_FILE = "ARTHAS_LOG_FILE";

    /**
     * 日志文件目录属性名
     * 用于指定日志文件的写入目录
     */
    public static final String FILE_PATH_PROPERTY = "arthas.logging.file.path";

    /**
     * 日志文件路径在Logback配置中的变量名
     */
    public static final String ARTHAS_LOG_PATH = "ARTHAS_LOG_PATH";

    /**
     * 日志文件的完整路径
     * 初始化后保存实际使用的日志文件路径
     */
    private static String logFile = "";

    /**
     * 初始化日志系统
     *
     * <pre>
     * 1. 尝试从 arthas.logging.config 这个配置里加载 logback.xml
     * 2. 尝试从 arthas.home 下面找 logback.xml
     *
     * 可以用 arthas.logging.file.name 指定具体arthas.log的名字
     * 可以用 arthas.logging.file.path 指定具体arthas.log的目录
     *
     * </pre>
     *
     * @param env Arthas环境配置对象
     * @return 初始化后的LoggerContext对象，如果初始化失败则返回null
     */
    public static LoggerContext initLogger(ArthasEnvironment env) {
        // 解析日志配置文件路径占位符
        String loggingConfig = env.resolvePlaceholders(LOGGING_CONFIG);
        if (loggingConfig == null || loggingConfig.trim().isEmpty()) {
            return null;
        }
        // 输出调试信息，显示日志配置文件路径
        AnsiLog.debug("arthas logging file: " + loggingConfig);
        File configFile = new File(loggingConfig);
        // 检查配置文件是否存在
        if (!configFile.isFile()) {
            AnsiLog.error("can not find arthas logging config: " + loggingConfig);
            return null;
        }

        try {
            // 获取LoggerContext实例
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            // 重置LoggerContext，清除之前的配置
            loggerContext.reset();

            // 获取日志文件名称配置
            String fileName = env.getProperty(FILE_NAME_PROPERTY);
            if (fileName != null) {
                // 将日志文件名称设置为Logback上下文属性
                loggerContext.putProperty(ARTHAS_LOG_FILE, fileName);
            }
            // 获取日志文件路径配置
            String filePath = env.getProperty(FILE_PATH_PROPERTY);
            if (filePath != null) {
                // 将日志文件路径设置为Logback上下文属性
                loggerContext.putProperty(ARTHAS_LOG_PATH, filePath);
            }

            // 创建Logback配置器
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            // 加载logback配置文件
            configurator.doConfigure(configFile.toURI().toURL());

            // 查找名为"ARTHAS"的日志文件appender
            Iterator<Appender<ILoggingEvent>> appenders = loggerContext.getLogger("root").iteratorForAppenders();

            while (appenders.hasNext()) {
                Appender<ILoggingEvent> appender = appenders.next();
                // 检查是否为滚动文件追加器
                if (appender instanceof RollingFileAppender) {
                    RollingFileAppender fileAppender = (RollingFileAppender) appender;
                    // 检查appender名称是否为"ARTHAS"
                    if ("ARTHAS".equalsIgnoreCase(fileAppender.getName())) {
                        // 保存日志文件的规范路径
                        logFile = new File(fileAppender.getFile()).getCanonicalPath();
                    }
                }
            }

            return loggerContext;
        } catch (Throwable e) {
            // 捕获所有异常，输出错误信息
            AnsiLog.error("try to load arthas logging config file error: " + configFile, e);
        }
        return null;
    }

    /**
     * 获取日志文件名
     *
     * @return 日志文件名，如果未配置则返回"arthas.log"
     */
    public static String loggingFile() {
        // 如果日志文件路径为null或空，返回默认值
        if (logFile == null || logFile.trim().isEmpty()) {
            return "arthas.log";
        }
        return logFile;
    }

    /**
     * 获取日志文件所在目录
     *
     * @return 日志文件目录，如果获取失败则返回当前工作目录
     */
    public static String loggingDir() {
        // 如果日志文件路径不为空
        if (logFile != null && !logFile.isEmpty()) {
            // 获取日志文件的父目录
            String parent = new File(logFile).getParent();
            if (parent != null) {
                return parent;
            }
        }
        // 返回当前工作目录
        return new File("").getAbsolutePath();
    }

    /**
     * 获取缓存目录
     * 在日志目录的父目录下创建arthas-cache目录
     *
     * @return 缓存目录的绝对路径
     */
    public static String cacheDir() {
        // 获取日志目录的父目录
        File logsDir = new File(loggingDir()).getParentFile();
        if (logsDir.exists()) {
            // 在父目录下创建arthas-cache目录
            File arthasCacheDir = new File(logsDir, "arthas-cache");
            // 创建目录（包括所有不存在的父目录）
            arthasCacheDir.mkdirs();
            return arthasCacheDir.getAbsolutePath();
        } else {
            // 如果父目录不存在，在当前目录创建arthas-cache
            File arthasCacheDir = new File("arthas-cache");
            arthasCacheDir.mkdirs();
            return arthasCacheDir.getAbsolutePath();
        }
    }

    /**
     * 获取用于记录结果的Logger
     *
     * @return 名为"result"的Logger实例
     */
    public static Logger getResultLogger() {
        return LoggerFactory.getLogger("result");
    }
}
