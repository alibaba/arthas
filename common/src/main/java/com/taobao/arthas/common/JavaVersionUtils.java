package com.taobao.arthas.common;

import java.util.Properties;

/**
 *
 * @author hengyunabc 2018-11-21
 *
 */
public class JavaVersionUtils {
    private static final String VERSION_PROP_NAME = "java.specification.version";
    private static final String JAVA_VERSION_STR = System.getProperty(VERSION_PROP_NAME);
    private static final float JAVA_VERSION = Float.parseFloat(JAVA_VERSION_STR);

    private JavaVersionUtils() {
    }

    public static String javaVersionStr() {
        return JAVA_VERSION_STR;
    }

    public static String javaVersionStr(Properties props) {
        return (null != props) ? props.getProperty(VERSION_PROP_NAME): null;
    }

    public static float javaVersion() {
        return JAVA_VERSION;
    }

    public static boolean isJava6() {
        return JAVA_VERSION_STR.equals("1.6");
    }

    public static boolean isJava7() {
        return JAVA_VERSION_STR.equals("1.7");
    }

    public static boolean isJava8() {
        return JAVA_VERSION_STR.equals("1.8");
    }

    public static boolean isJava9() {
        return JAVA_VERSION_STR.equals("9");
    }

    public static boolean isLessThanJava9() {
        return JAVA_VERSION < 9.0f;
    }

    public static boolean isGreaterThanJava7() {
        return JAVA_VERSION > 1.7f;
    }

    public static boolean isGreaterThanJava8() {
        return JAVA_VERSION > 1.8f;
    }

    public static boolean isGreaterThanJava11() {
        return JAVA_VERSION > 11.0f;
    }
}
