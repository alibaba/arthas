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
            libName = "libArthasJniLibrary.dylib";
        }
        if (OSUtils.isLinux()) {
            if (OSUtils.isArm32()) {
                libName = "libArthasJniLibrary-arm.so";
            } else if (OSUtils.isArm64()) {
                libName = "libArthasJniLibrary-aarch64.so";
            } else if (OSUtils.isX86_64()) {
                libName = "libArthasJniLibrary-x64.so";
            }else {
                libName = "libArthasJniLibrary-" + OSUtils.arch() + ".so";
            }
        }
        if (OSUtils.isWindows()) {
            libName = "libArthasJniLibrary-x64.dll";
            if (OSUtils.isX86()) {
                libName = "libArthasJniLibrary-x86.dll";
            }
        }
    }

    public static String detectLibName() {
        return libName;
    }
}
