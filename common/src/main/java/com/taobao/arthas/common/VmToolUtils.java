package com.taobao.arthas.common;

/**
 * 
 * @author hengyunabc 2021-04-27
 *
 */
public class VmToolUtils {
    private static String libName = null;
    static {
        if (OSUtils.isMac()) {
            libName = "libArthasJniLibrary-x64.dylib";
        }
        if (OSUtils.isLinux()) {
            libName = "libArthasJniLibrary-x64.so";
            if (OSUtils.isArm32()) {
                libName = "libArthasJniLibrary-arm.so";
            } else if (OSUtils.isArm64()) {
                libName = "libArthasJniLibrary-aarch64.so";
            }
        }
        if (OSUtils.isWindows()) {
            libName = "libArthasJniLibrary-x64.dll";
        }
    }

    public static String detectLibName() {
        return libName;
    }
}
