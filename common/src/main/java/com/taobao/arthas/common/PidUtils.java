package com.taobao.arthas.common;

import java.lang.management.ManagementFactory;

/**
 *
 * @author hengyunabc 2019-02-16
 *
 */
public class PidUtils {
    private static String PID = "-1";
    private static long pid = -1;

    private static String MAIN_CLASS = "";

    static {
        // https://stackoverflow.com/a/7690178
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            int index = jvmName.indexOf('@');

            if (index > 0) {
                PID = Long.toString(Long.parseLong(jvmName.substring(0, index)));
                pid = Long.parseLong(PID);
            }
        } catch (Throwable e) {
            // ignore
        }

        try {
            MAIN_CLASS = System.getProperty("sun.java.command", "");
        } catch (Throwable e) {
            // ignore
        }

    }

    private PidUtils() {
    }

    public static String currentPid() {
        return PID;
    }

    public static long currentLongPid() {
        return pid;
    }

    public static String mainClass() {
        return MAIN_CLASS;
    }
}
