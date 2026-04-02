package com.taobao.arthas.common;

/**
 * VM工具库加载工具类
 * 负责根据当前操作系统和CPU架构检测并返回正确的本地库文件名
 * Arthas通过JNI调用本地库来实现某些高级功能
 *
 * @author hengyunabc 2021-04-27
 *
 */
public class VmToolUtils {
    /**
     * 本地库文件名
     * 根据操作系统和架构自动初始化为对应的库文件名
     */
    private static String libName = null;

    // 静态初始化块
    // 在类加载时根据操作系统和CPU架构设置正确的库文件名
    static {
        // Mac系统
        if (OSUtils.isMac()) {
            // Mac系统使用dylib格式的动态库
            libName = "libArthasJniLibrary.dylib";
        }

        // Linux系统
        if (OSUtils.isLinux()) {
            // ARM 32位架构
            if (OSUtils.isArm32()) {
                libName = "libArthasJniLibrary-arm.so";
            }
            // ARM 64位架构（如Apple Silicon M1/M2等）
            else if (OSUtils.isArm64()) {
                libName = "libArthasJniLibrary-aarch64.so";
            }
            // x86_64架构（64位Intel/AMD处理器）
            else if (OSUtils.isX86_64()) {
                libName = "libArthasJniLibrary-x64.so";
            }
            // LoongArch64架构（龙芯64位架构）
            else if (OSUtils.isLoongArch64()) {
                libName = "libArthasJniLibrary-loongarch64.so";
            }
            // 其他架构，使用通用格式
            else {
                libName = "libArthasJniLibrary-" + OSUtils.arch() + ".so";
            }
        }

        // Windows系统
        if (OSUtils.isWindows()) {
            // 默认使用64位DLL
            libName = "libArthasJniLibrary-x64.dll";
            // 如果是32位x86架构
            if (OSUtils.isX86()) {
                libName = "libArthasJniLibrary-x86.dll";
            }
        }
    }

    /**
     * 检测并返回当前系统对应的本地库文件名
     * 库文件名在类加载时已经根据操作系统和CPU架构自动确定
     *
     * @return 本地库文件名，如"libArthasJniLibrary-x64.so"
     */
    public static String detectLibName() {
        return libName;
    }
}
