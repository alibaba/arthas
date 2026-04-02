package com.taobao.arthas.common;

/**
 * 支持的操作系统平台枚举
 * <p>
 * 该枚举定义了Arthas支持的所有操作系统平台类型。
 * 用于识别当前运行环境所在的操作系统，以便执行平台相关的操作。
 * </p>
 *
 */
public enum PlatformEnum {
    /**
     * Microsoft Windows操作系统
     * 包括Windows XP、Windows 7、Windows 10、Windows 11等各个版本
     */
    WINDOWS,

    /**
     * Linux操作系统
     * 包括各种Linux发行版，如Ubuntu、CentOS、Debian等
     */
    LINUX,

    /**
     * macOS操作系统（也称为OS X）
     * 苹果公司的桌面操作系统
     */
    MACOSX,

    /**
     * 未知操作系统
     * 当无法识别当前操作系统时使用此值
     */
    UNKNOWN
}