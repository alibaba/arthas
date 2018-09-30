package com.taobao.arthas.core.util;

import java.lang.management.ManagementFactory;

/**
 *
 * @author hengyunabc 2018-09-30
 *
 */
public class ApplicationUtils {

    private static String PID = "-1";

    static {
        // https://stackoverflow.com/a/7690178
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        int index = jvmName.indexOf('@');

        if (index > 0) {
            try {
                PID = Long.toString(Long.parseLong(jvmName.substring(0, index)));
            } catch (Throwable e) {
                // ignore
            }
        }
    }

    public static String getPid() {
        return PID;
    }

}
