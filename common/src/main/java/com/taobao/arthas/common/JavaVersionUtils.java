package com.taobao.arthas.common;

import java.util.Properties;

/**
 * Java版本工具类
 * 提供获取和判断当前Java运行环境版本的工具方法
 * 用于检测Java版本，以便在不同版本的Java上执行不同的逻辑
 *
 * @author hengyunabc 2018-11-21
 *
 */
public class JavaVersionUtils {
    /**
     * Java版本属性名称
     * 用于从系统属性中获取Java规范版本
     */
    private static final String VERSION_PROP_NAME = "java.specification.version";

    /**
     * Java版本字符串
     * 从系统属性中获取的原始版本字符串，如"1.8"、"9"、"11"等
     */
    private static final String JAVA_VERSION_STR = System.getProperty(VERSION_PROP_NAME);

    /**
     * Java版本浮点数
     * 将版本字符串转换为浮点数，便于版本比较
     * 例如：1.6 -> 1.6f, 1.8 -> 1.8f, 9 -> 9.0f, 11 -> 11.0f
     */
    private static final float JAVA_VERSION = Float.parseFloat(JAVA_VERSION_STR);

    /**
     * 私有构造函数，防止实例化
     * 这是一个工具类，所有方法都是静态的，不需要创建实例
     */
    private JavaVersionUtils() {
    }

    /**
     * 获取当前Java版本字符串
     *
     * @return Java版本字符串，如"1.8"、"9"、"11"等
     */
    public static String javaVersionStr() {
        return JAVA_VERSION_STR;
    }

    /**
     * 从指定的属性对象中获取Java版本字符串
     * 用于获取其他Java进程的版本信息
     *
     * @param props 包含Java系统属性的Properties对象
     * @return Java版本字符串，如果props为null则返回null
     */
    public static String javaVersionStr(Properties props) {
        return (null != props) ? props.getProperty(VERSION_PROP_NAME): null;
    }

    /**
     * 获取当前Java版本浮点数
     *
     * @return Java版本浮点数，如1.6f、1.8f、9.0f、11.0f等
     */
    public static float javaVersion() {
        return JAVA_VERSION;
    }

    /**
     * 判断当前是否运行在Java 6上
     *
     * @return 如果是Java 6返回true，否则返回false
     */
    public static boolean isJava6() {
        return "1.6".equals(JAVA_VERSION_STR);
    }

    /**
     * 判断当前是否运行在Java 7上
     *
     * @return 如果是Java 7返回true，否则返回false
     */
    public static boolean isJava7() {
        return "1.7".equals(JAVA_VERSION_STR);
    }

    /**
     * 判断当前是否运行在Java 8上
     *
     * @return 如果是Java 8返回true，否则返回false
     */
    public static boolean isJava8() {
        return "1.8".equals(JAVA_VERSION_STR);
    }

    /**
     * 判断当前是否运行在Java 9上
     *
     * @return 如果是Java 9返回true，否则返回false
     */
    public static boolean isJava9() {
        return "9".equals(JAVA_VERSION_STR);
    }

    /**
     * 判断当前Java版本是否低于Java 9
     * 包括Java 6、7、8等版本
     *
     * @return 如果版本低于Java 9返回true，否则返回false
     */
    public static boolean isLessThanJava9() {
        return JAVA_VERSION < 9.0f;
    }

    /**
     * 判断当前Java版本是否高于Java 7
     * 包括Java 8、9、10、11等版本
     *
     * @return 如果版本高于Java 7返回true，否则返回false
     */
    public static boolean isGreaterThanJava7() {
        return JAVA_VERSION > 1.7f;
    }

    /**
     * 判断当前Java版本是否高于Java 8
     * 包括Java 9、10、11等版本
     *
     * @return 如果版本高于Java 8返回true，否则返回false
     */
    public static boolean isGreaterThanJava8() {
        return JAVA_VERSION > 1.8f;
    }

    /**
     * 判断当前Java版本是否高于Java 11
     * 包括Java 12、13、14等版本
     *
     * @return 如果版本高于Java 11返回true，否则返回false
     */
    public static boolean isGreaterThanJava11() {
        return JAVA_VERSION > 11.0f;
    }
}
