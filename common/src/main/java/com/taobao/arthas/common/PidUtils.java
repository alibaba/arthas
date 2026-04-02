package com.taobao.arthas.common;

import java.lang.management.ManagementFactory;

/**
 * 进程ID工具类
 * <p>
 * 该工具类用于获取当前Java进程的进程ID（PID）和主类名称。
 * 通过JVM的管理API自动获取进程信息，在类加载时自动初始化。
 * </p>
 *
 * @author hengyunabc 2019-02-16
 *
 */
public class PidUtils {
    /**
     * 当前进程的PID字符串形式
     * 默认值为"-1"，表示无法获取或初始化失败
     */
    private static String PID = "-1";

    /**
     * 当前进程的PID长整型形式
     * 默认值为-1，表示无法获取或初始化失败
     */
    private static long pid = -1;

    /**
     * 当前进程的主类名称
     * 默认值为空字符串，表示无法获取或初始化失败
     */
    private static String MAIN_CLASS = "";

    /**
     * 静态初始化块
     * <p>
     * 在类加载时自动执行，用于初始化PID和主类名称。
     * 参考链接：https://stackoverflow.com/a/7690178
     * </p>
     */
    static {
        // 尝试获取当前JVM的进程ID
        try {
            // 获取运行时MXBean，其getName()方法返回的格式为"pid@hostname"
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            int index = jvmName.indexOf('@');

            // 如果找到@符号，说明格式正确，提取@符号之前的部分作为PID
            if (index > 0) {
                PID = Long.toString(Long.parseLong(jvmName.substring(0, index)));
                pid = Long.parseLong(PID);
            }
        } catch (Throwable e) {
            // 忽略所有异常，保持默认值-1
        }

        // 尝试获取主类名称
        try {
            // 从系统属性sun.java.command获取启动命令
            // 该属性包含主类名及其参数，格式为"MainClass arg1 arg2 ..."
            String command = System.getProperty("sun.java.command", "");
            // 只需要主类名，因此只取第一个空格之前的部分
            int spaceIndex = command.indexOf(' ');
            MAIN_CLASS = spaceIndex != -1 ? command.substring(0, spaceIndex) : command;
        } catch (Throwable e) {
            // 忽略所有异常，保持默认值空字符串
        }

    }

    /**
     * 私有构造函数
     * <p>
     * 防止实例化该工具类，所有方法均为静态方法。
     * </p>
     */
    private PidUtils() {
    }

    /**
     * 获取当前进程的PID字符串形式
     *
     * @return 当前进程的PID字符串，如果无法获取则返回"-1"
     */
    public static String currentPid() {
        return PID;
    }

    /**
     * 获取当前进程的PID长整型形式
     *
     * @return 当前进程的PID长整型值，如果无法获取则返回-1
     */
    public static long currentLongPid() {
        return pid;
    }

    /**
     * 获取当前进程的主类名称
     *
     * @return 主类名称，如果无法获取则返回空字符串
     */
    public static String mainClass() {
        return MAIN_CLASS;
    }
}
