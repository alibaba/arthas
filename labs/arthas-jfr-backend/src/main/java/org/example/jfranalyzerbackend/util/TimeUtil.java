
package org.example.jfranalyzerbackend.util;

import static java.util.concurrent.TimeUnit.*;

public class TimeUtil {
    public static long parseTimespan(String s) {
        long timeSpan = Long.parseLong(s.substring(0, s.length() - 2).trim());
        if (s.endsWith("ns")) {
            return timeSpan;
        }
        if (s.endsWith("us")) {
            return NANOSECONDS.convert(timeSpan, MICROSECONDS);
        }
        if (s.endsWith("ms")) {
            return NANOSECONDS.convert(timeSpan, MILLISECONDS);
        }
        timeSpan = Long.parseLong(s.substring(0, s.length() - 1).trim());
        if (s.endsWith("s")) {
            return NANOSECONDS.convert(timeSpan, SECONDS);
        }
        if (s.endsWith("m")) {
            return 60 * NANOSECONDS.convert(timeSpan, SECONDS);
        }
        if (s.endsWith("h")) {
            return 60 * 60 * NANOSECONDS.convert(timeSpan, SECONDS);
        }
        if (s.endsWith("d")) {
            return 24 * 60 * 60 * NANOSECONDS.convert(timeSpan, SECONDS);
        }

        try {
            return Long.parseLong(s);
        } catch (NumberFormatException nfe) {
            // 只接受带单位的数值
            throw new NumberFormatException("Timespan '" + s + "' is invalid. Valid units are ns, us, s, m, h and d.");
        }
    }
}
