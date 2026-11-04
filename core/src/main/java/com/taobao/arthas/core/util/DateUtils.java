package com.taobao.arthas.core.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author diecui1202 on 2017/10/25.
 */
public final class DateUtils {

    private DateUtils() {
        throw new AssertionError();
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public static String getCurrentDateTime() {
        return DATE_TIME_FORMATTER.format(LocalDateTime.now());
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    public static String getStartDateTime() {
        try {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            long startTime = runtimeMXBean.getStartTime();
            Instant startInstant = Instant.ofEpochMilli(startTime);
            LocalDateTime startDateTime = LocalDateTime.ofInstant(startInstant, ZoneId.systemDefault());
            return DATE_TIME_FORMATTER.format(startDateTime);
        } catch (Throwable e) {
            return "unknown";
        }
    }
}
