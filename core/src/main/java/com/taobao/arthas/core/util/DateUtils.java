package com.taobao.arthas.core.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author diecui1202 on 2017/10/25.
 */
public class DateUtils {

    private static final ThreadLocal<SimpleDateFormat> dataFormat = new ThreadLocal<SimpleDateFormat>() {

        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static String getCurrentDate() {
        return dataFormat.get().format(new Date());
    }

    public static String formatDate(Date date) {
        return dataFormat.get().format(date);
    }
}
