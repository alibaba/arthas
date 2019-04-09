package com.taobao.arthas.common;

import java.util.logging.Level;
import java.util.regex.Matcher;

/**
 *
 * <pre>
 * FINEST  -> TRACE
 * FINER   -> DEBUG
 * FINE    -> DEBUG
 * CONFIG  -> INFO
 * INFO    -> INFO
 * WARNING -> WARN
 * SEVERE  -> ERROR
 * </pre>
 *
 * @see org.slf4j.bridge.SLF4JBridgeHandler
 * @author hengyunabc 2017-05-03
 *
 */
public abstract class AnsiLog {

    static boolean enableColor;

    public static java.util.logging.Level LEVEL = java.util.logging.Level.CONFIG;

    private static final String RESET = "\033[0m";

    private static final int DEFAULT = 39;
    private static final int BLACK = 30;
    private static final int RED = 31;
    private static final int GREEN = 32;
    private static final int YELLOW = 33;
    private static final int BLUE = 34;
    private static final int MAGENTA = 35;
    private static final int CYAN = 36;
    private static final int WHITE = 37;

    private static final String TRACE_PREFIX = "[TRACE] ";
    private static final String TRACE_COLOR_PREFIX = "[" + colorStr("TRACE", GREEN) + "] ";

    private static final String DEBUG_PREFIX = "[DEBUG] ";
    private static final String DEBUG_COLOR_PREFIX = "[" + colorStr("DEBUG", GREEN) + "] ";

    private static final String INFO_PREFIX = "[INFO] ";
    private static final String INFO_COLOR_PREFIX = "[" + colorStr("INFO", GREEN) + "] ";

    private static final String WARN_PREFIX = "[WARN] ";
    private static final String WARN_COLOR_PREFIX = "[" + colorStr("WARN", YELLOW) + "] ";

    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final String ERROR_COLOR_PREFIX = "[" + colorStr("ERROR", RED) + "] ";

    static {
        if (System.console() != null) {
            enableColor = true;
            // windows dos, do not support color
            if (OSUtils.isWindows()) {
                enableColor = false;
            }
        }
        // cygwin and mingw support color
        if (OSUtils.isCygwinOrMinGW()) {
            enableColor = true;
        }
    }

    public static boolean enableColor() {
        return enableColor;
    }

    /**
     * set logger Level
     *
     * @see java.util.logging.Level
     * @param level
     * @return
     */
    public static Level level(Level level) {
        Level old = LEVEL;
        LEVEL = level;
        return old;
    }

    /**
     * get current logger Level
     *
     * @return
     */
    public static Level level() {
        return LEVEL;
    }

    public static String black(String msg) {
        if (enableColor) {
            return colorStr(msg, BLACK);
        } else {
            return msg;
        }
    }

    public static String red(String msg) {
        if (enableColor) {
            return colorStr(msg, RED);
        } else {
            return msg;
        }
    }

    public static String green(String msg) {
        if (enableColor) {
            return colorStr(msg, GREEN);
        } else {
            return msg;
        }
    }

    public static String yellow(String msg) {
        if (enableColor) {
            return colorStr(msg, YELLOW);
        } else {
            return msg;
        }
    }

    public static String blue(String msg) {
        if (enableColor) {
            return colorStr(msg, BLUE);
        } else {
            return msg;
        }
    }

    public static String magenta(String msg) {
        if (enableColor) {
            return colorStr(msg, MAGENTA);
        } else {
            return msg;
        }
    }

    public static String cyan(String msg) {
        if (enableColor) {
            return colorStr(msg, CYAN);
        } else {
            return msg;
        }
    }

    public static String white(String msg) {
        if (enableColor) {
            return colorStr(msg, WHITE);
        } else {
            return msg;
        }
    }

    private static String colorStr(String msg, int colorCode) {
        return "\033[" + colorCode + "m" + msg + RESET;
    }

    public static void trace(String msg) {
        if (canLog(Level.FINEST)) {
            if (enableColor) {
                System.out.println(TRACE_COLOR_PREFIX + msg);
            } else {
                System.out.println(TRACE_PREFIX + msg);
            }
        }
    }

    public static void trace(String format, Object... arguments) {
        if (canLog(Level.FINEST)) {
            trace(format(format, arguments));
        }
    }

    public static void trace(Throwable t) {
        if (canLog(Level.FINEST)) {
            t.printStackTrace(System.out);
        }
    }

    public static void debug(String msg) {
        if (canLog(Level.FINER)) {
            if (enableColor) {
                System.out.println(DEBUG_COLOR_PREFIX + msg);
            } else {
                System.out.println(DEBUG_PREFIX + msg);
            }
        }
    }

    public static void debug(String format, Object... arguments) {
        if (canLog(Level.FINER)) {
            debug(format(format, arguments));
        }
    }

    public static void debug(Throwable t) {
        if (canLog(Level.FINER)) {
            t.printStackTrace(System.out);
        }
    }

    public static void info(String msg) {
        if (canLog(Level.CONFIG)) {
            if (enableColor) {
                System.out.println(INFO_COLOR_PREFIX + msg);
            } else {
                System.out.println(INFO_PREFIX + msg);
            }
        }
    }

    public static void info(String format, Object... arguments) {
        if (canLog(Level.CONFIG)) {
            info(format(format, arguments));
        }
    }

    public static void info(Throwable t) {
        if (canLog(Level.CONFIG)) {
            t.printStackTrace(System.out);
        }
    }

    public static void warn(String msg) {
        if (canLog(Level.WARNING)) {
            if (enableColor) {
                System.out.println(WARN_COLOR_PREFIX + msg);
            } else {
                System.out.println(WARN_PREFIX + msg);
            }
        }
    }

    public static void warn(String format, Object... arguments) {
        if (canLog(Level.WARNING)) {
            warn(format(format, arguments));
        }
    }

    public static void warn(Throwable t) {
        if (canLog(Level.WARNING)) {
            t.printStackTrace(System.out);
        }
    }

    public static void error(String msg) {
        if (canLog(Level.SEVERE)) {
            if (enableColor) {
                System.out.println(ERROR_COLOR_PREFIX + msg);
            } else {
                System.out.println(ERROR_PREFIX + msg);
            }
        }
    }

    public static void error(String format, Object... arguments) {
        if (canLog(Level.SEVERE)) {
            error(format(format, arguments));
        }
    }

    public static void error(Throwable t) {
        if (canLog(Level.SEVERE)) {
            t.printStackTrace(System.out);
        }
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

    private static boolean canLog(Level level) {
        return level.intValue() >= LEVEL.intValue();
    }
}
