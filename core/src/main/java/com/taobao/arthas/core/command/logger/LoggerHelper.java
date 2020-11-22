package com.taobao.arthas.core.command.logger;

/**
 * 
 * @author hengyunabc 2019-09-06
 *
 */
public interface LoggerHelper {
    public static final String clazz = "class";
    public static final String classLoader = "classLoader";
    public static final String classLoaderHash = "classLoaderHash";
    public static final String codeSource = "codeSource";

    // logger info
    public static final String level = "level";
    public static final String effectiveLevel = "effectiveLevel";

    // log4j2 only
    public static final String config = "config";

    // type boolean
    public static final String additivity = "additivity";
    public static final String appenders = "appenders";

    // appender info
    public static final String name = "name";
    public static final String file = "file";
    public static final String blocking = "blocking";
    // type List<String>
    public static final String appenderRef = "appenderRef";
    public static final String target = "target";

}
