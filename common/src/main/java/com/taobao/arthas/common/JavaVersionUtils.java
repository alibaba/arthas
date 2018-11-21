package com.taobao.arthas.common;

/**
 *
 * @author hengyunabc 2018-11-21
 *
 */
public class JavaVersionUtils {
    public static void main(String[] args) {
        System.err.println(JavaVersionUtils.isJava8());
    }

    private static final float javaVersion = Float.parseFloat(System.getProperty("java.specification.version"));

    public static float javaVersion() {
        return javaVersion;
    }

    public static boolean isJava6() {
        return Float.toString(javaVersion).equals("1.6");
    }

    public static boolean isJava7() {
        return Float.toString(javaVersion).equals("1.7");
    }

    public static boolean isJava8() {
        return Float.toString(javaVersion).equals("1.8");
    }

    public static boolean isJava9() {
        return Float.toString(javaVersion).equals("9");
    }

    public static boolean isLessThanJava9() {
        return javaVersion < 9.0f;
    }

    public static boolean isGreaterThanJava8() {
        return javaVersion > 1.8f;
    }
}
