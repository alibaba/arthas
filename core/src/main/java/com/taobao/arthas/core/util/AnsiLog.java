package com.taobao.arthas.core.util;
import java.util.regex.Matcher;

/**
 *
 * @author hengyunabc 2017-05-03
 *
 */
public abstract class AnsiLog {

    static boolean enable = System.console() != null;

    /**
     *
     * @param msg
     */
    public static void red(String msg) {
        if (enable) {
            System.out.println("[31m" + msg + "[0m");
        } else {
            System.err.println(msg);
        }
    }

    public static void green(String msg) {
        if (enable) {
            System.out.println("[32m" + msg + "[0m");
        } else {
            System.out.println(msg);
        }
    }

    public static void yellow(String msg) {
        if (enable) {
            System.out.println("[33m" + msg + "[0m");
        } else {
            System.out.println(msg);
        }
    }

    public static void info(String msg) {
        green(msg);
    }

    public static void info(String format, Object... arguments) {
        green(format(format, arguments));
    }

    public static void warn(String msg) {
        yellow(msg);
    }

    public static void warn(String format, Object... arguments) {
        yellow(format(format, arguments));
    }

    public static void error(String msg) {
        red(msg);
    }

    public static void error(String format, Object... arguments) {
        red(format(format, arguments));
    }

    private static String format(String from, Object... arguments) {
        if (from != null) {
            String computed = from;
            if (arguments != null && arguments.length != 0) {
                for (Object argument : arguments) {
                    computed = computed.replaceFirst("\\{\\}", Matcher.quoteReplacement(argument.toString()));
                }
            }
            return computed;
        }
        return null;
    }
}
