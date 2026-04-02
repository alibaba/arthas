package com.taobao.arthas.core.command.logger;

/**
 * Logger助手接口
 *
 * 定义了Logger信息中使用的各种属性名称常量。
 * 这些常量用于在Map中存储和访问Logger、Appender的相关信息。
 *
 * @author hengyunabc 2019-09-06
 *
 */
public interface LoggerHelper {
    /**
     * 类对象属性名
     */
    public static final String clazz = "class";

    /**
     * ClassLoader对象属性名
     */
    public static final String classLoader = "classLoader";

    /**
     * ClassLoader哈希码属性名
     */
    public static final String classLoaderHash = "classLoaderHash";

    /**
     * 代码源位置属性名（jar包路径或class文件路径）
     */
    public static final String codeSource = "codeSource";

    // logger info - Logger相关信息

    /**
     * 日志级别属性名
     */
    public static final String level = "level";

    /**
     * 实际有效的日志级别属性名
     */
    public static final String effectiveLevel = "effectiveLevel";

    // log4j2 only - 仅Log4j2使用

    /**
     * 配置文件路径属性名（仅Log4j2使用）
     */
    public static final String config = "config";

    // type boolean - 布尔类型属性

    /**
     * Logger的additivity属性名（是否继承父Logger的Appender）
     */
    public static final String additivity = "additivity";

    /**
     * Appender列表属性名
     */
    public static final String appenders = "appenders";

    // appender info - Appender相关信息

    /**
     * 名称属性名（Logger或Appender的名称）
     */
    public static final String name = "name";

    /**
     * 文件路径属性名（文件Appender的输出文件路径）
     */
    public static final String file = "file";

    /**
     * 阻塞标识属性名（异步Appender是否可能阻塞）
     */
    public static final String blocking = "blocking";

    // type List<String> - 字符串列表类型

    /**
     * Appender引用列表属性名（异步Appender引用的其他Appender名称列表）
     */
    public static final String appenderRef = "appenderRef";

    /**
     * 目标属性名（控制台Appender的输出目标：System.out或System.err）
     */
    public static final String target = "target";

}
