package com.taobao.arthas.boot;

/**
 *
 * @author hengyunabc 2018-11-08
 *
 */
public class OSUtils {

    static PlatformEnum platform;
    static {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            platform = PlatformEnum.LINUX;
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            platform = PlatformEnum.MACOSX;
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            platform = PlatformEnum.MACOSX;
        } else if (osName.startsWith("Windows")) {
            platform = PlatformEnum.WINDOWS;
        } else {
            platform = PlatformEnum.UNKNOWN;
        }
    }

    public static boolean isWindows() {
        return platform == PlatformEnum.WINDOWS;
    }

    public static boolean isLinux() {
        return platform == PlatformEnum.LINUX;
    }

    public static boolean isMac() {
        return platform == PlatformEnum.MACOSX;
    }

}
