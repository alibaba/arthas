package com.taobao.arthas.core.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 * <p>
 * 提供日期时间格式化和获取的相关工具方法，包括获取当前时间、格式化时间、获取JVM启动时间等
 * </p>
 *
 * @author diecui1202 on 2017/10/25.
 */
public final class DateUtils {

    /**
     * 私有构造函数，防止实例化
     * <p>
     * 这是一个工具类，只包含静态方法，不应该被实例化。
     * 如果尝试实例化，将抛出AssertionError异常
     * </p>
     *
     * @throws AssertionError 当尝试实例化时抛出
     */
    private DateUtils() {
        throw new AssertionError();
    }

    /**
     * 日期时间格式化器
     * <p>
     * 使用固定的日期时间格式：yyyy-MM-dd HH:mm:ss.SSS
     * 例如：2026-04-01 15:30:45.123
     * </p>
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 获取当前日期时间字符串
     * <p>
     * 返回格式化后的当前系统时间，格式为：yyyy-MM-dd HH:mm:ss.SSS
     * </p>
     *
     * @return 格式化后的当前日期时间字符串
     */
    public static String getCurrentDateTime() {
        // 使用预定义的格式化器格式化当前时间
        return DATE_TIME_FORMATTER.format(LocalDateTime.now());
    }

    /**
     * 格式化指定的日期时间
     * <p>
     * 将LocalDateTime对象格式化为指定格式的字符串
     * </p>
     *
     * @param dateTime 要格式化的日期时间对象
     * @return 格式化后的日期时间字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        // 使用预定义的格式化器格式化指定时间
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    /**
     * 获取JVM启动时间
     * <p>
     * 通过RuntimeMXBean获取Java虚拟机的启动时间，并格式化为字符串。
     * 如果获取失败（例如某些环境限制），则返回"unknown"
     * </p>
     *
     * @return JVM启动时间的格式化字符串，格式为：yyyy-MM-dd HH:mm:ss.SSS，
     *         如果获取失败则返回"unknown"
     */
    public static String getStartDateTime() {
        try {
            // 获取运行时管理Bean，用于访问JVM运行时信息
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            // 获取JVM启动时间戳（毫秒）
            long startTime = runtimeMXBean.getStartTime();
            // 将时间戳转换为Instant对象
            Instant startInstant = Instant.ofEpochMilli(startTime);
            // 使用系统默认时区将Instant转换为LocalDateTime
            LocalDateTime startDateTime = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault());
            // 格式化并返回启动时间
            return DATE_TIME_FORMATTER.format(startDateTime);
        } catch (Throwable e) {
            // 如果获取过程中出现任何异常，返回"unknown"
            return "unknown";
        }
    }
}
