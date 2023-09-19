package com.taobao.arthas.core.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author brijeshprasad89
 *
 */
public class DateUtilsTest {

    @Test
    public void testFormatDateTimeWithCorrectFormat() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"); // supported date format
        LocalDateTime dateTime = LocalDateTime.now();
        Assert.assertEquals(DateUtils.formatDateTime(dateTime), dateTimeFormatter.format(dateTime));
    }

    @Test
    public void testFormatDateTimeWithInCorrectFormat() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); // Not supported Date format
        LocalDateTime dateTime = LocalDateTime.now();
        Assert.assertNotEquals(DateUtils.formatDateTime(dateTime), dateTimeFormatter.format(dateTime));
    }

}