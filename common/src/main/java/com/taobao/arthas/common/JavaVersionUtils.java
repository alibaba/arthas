package com.taobao.arthas.common;

import java.util.Properties;

/**
 *
 * @author hengyunabc 2018-11-21
 *
 */
public class JavaVersionUtils {
    private static final String versionPropName = "java.specification.version";
    private static final String javaVersionStr = System.getProperty(versionPropName);
    private static final float javaVersion = Float.parseFloat(javaVersionStr);

    public static String javaVersionStr() {
        return javaVersionStr;
    }

    public static String javaVersionStr(Properties props) {
        return (null != props) ? props.getProperty(versionPropName): null;
    }

    public static float javaVersion() {
        return javaVersion;
    }

    public static boolean isJava6() {
        return javaVersionStr.equals("1.6");
    }

    public static boolean isJava7() {
        return javaVersionStr.equals("1.7");
    }

    public static boolean isJava8() {
        return javaVersionStr.equals("1.8");
    }

    public static boolean isJava9() {
        return javaVersionStr.equals("9");
    }

    public static boolean isLessThanJava9() {
        return javaVersion < 9.0f;
    }

    public static boolean isGreaterThanJava8() {
        return javaVersion > 1.8f;
    }
}
