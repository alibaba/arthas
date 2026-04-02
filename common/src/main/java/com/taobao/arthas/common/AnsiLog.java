package com.taobao.arthas.common;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.regex.Matcher;

/**
 * ANSI彩色日志输出工具类
 *
 * <p>该类提供了一个简单的日志输出框架，支持ANSI颜色代码，可以在支持颜色的终端中输出彩色日志。
 * 日志级别映射关系：
 *
 * <pre>
 * FINEST  -> TRACE  (最详细的跟踪信息)
 * FINER   -> DEBUG  (调试信息)
 * FINE    -> DEBUG  (调试信息)
 * CONFIG  -> INFO   (配置信息)
 * INFO    -> INFO   (一般信息)
 * WARNING -> WARN   (警告信息)
 * SEVERE  -> ERROR  (错误信息)
 * </pre>
 *
 * <p>该类特点：
 * <ul>
 *   <li>支持ANSI颜色代码，可以在终端中显示彩色日志</li>
 *   <li>自动检测操作系统和终端类型，在不支持颜色的环境下自动禁用颜色</li>
 *   <li>支持Windows、Linux、MacOS等平台</li>
 *   <li>支持Cygwin和MinGW环境</li>
 *   <li>可以自定义日志输出流</li>
 *   <li>支持日志级别过滤</li>
 *   <li>支持占位符格式的日志输出</li>
 * </ul>
 *
 * @see org.slf4j.bridge.SLF4JBridgeHandler
 * @author hengyunabc 2017-05-03
 */
public abstract class AnsiLog {

    /**
     * 是否启用彩色输出标志
     *
     * <p>该标志控制日志输出是否使用ANSI颜色代码。
     * 在静态初始化块中会根据操作系统和终端类型自动设置此值。
     * Windows DOS默认不支持颜色，但Cygwin和MinGW环境支持颜色。
     */
    static boolean enableColor;

    /**
     * 日志消息输出流
     *
     * <p>默认值为System.out，即标准输出流。
     * 使用volatile关键字确保多线程环境下的可见性。
     * 可以通过out(PrintStream)方法修改此输出流。
     */
    private static volatile PrintStream out = System.out;

    /**
     * 当前日志级别
     *
     * <p>只有等于或高于此级别的日志才会被输出。
     * 默认级别为CONFIG（INFO级别），可以通过level(Level)方法修改。
     */
    public static java.util.logging.Level LEVEL = java.util.logging.Level.CONFIG;

    /**
     * ANSI重置代码
     *
     * <p>用于重置终端的颜色、样式等属性到默认值。
     * 所有的彩色输出后都需要加上此代码，否则会影响后续的输出。
     */
    private static final String RESET = "\033[0m";

    // ANSI前景色颜色代码常量
    // 这些数字是ANSI转义序列中用于设置文本颜色的标准代码

    /**
     * 默认颜色代码（39）
     *
     * <p>将文本颜色重置为终端默认颜色
     */
    private static final int DEFAULT = 39;

    /**
     * 黑色颜色代码（30）
     */
    private static final int BLACK = 30;

    /**
     * 红色颜色代码（31）
     *
     * <p>通常用于错误信息或重要提示
     */
    private static final int RED = 31;

    /**
     * 绿色颜色代码（32）
     *
     * <p>通常用于成功信息或一般日志级别标识
     */
    private static final int GREEN = 32;

    /**
     * 黄色颜色代码（33）
     *
     * <p>通常用于警告信息
     */
    private static final int YELLOW = 33;

    /**
     * 蓝色颜色代码（34）
     */
    private static final int BLUE = 34;

    /**
     * 品红色颜色代码（35）
     *
     * <p>也称为洋红色或紫红色
     */
    private static final int MAGENTA = 35;

    /**
     * 青色颜色代码（36）
     *
     * <p>也称为蓝绿色
     */
    private static final int CYAN = 36;

    /**
     * 白色颜色代码（37）
     */
    private static final int WHITE = 37;

    // 日志级别前缀常量（无颜色版本）
    // 用于不支持颜色的终端环境

    /**
     * TRACE级别日志前缀（无颜色）
     */
    private static final String TRACE_PREFIX = "[TRACE] ";

    /**
     * DEBUG级别日志前缀（无颜色）
     */
    private static final String DEBUG_PREFIX = "[DEBUG] ";

    /**
     * INFO级别日志前缀（无颜色）
     */
    private static final String INFO_PREFIX = "[INFO] ";

    /**
     * WARN级别日志前缀（无颜色）
     */
    private static final String WARN_PREFIX = "[WARN] ";

    /**
     * ERROR级别日志前缀（无颜色）
     */
    private static final String ERROR_PREFIX = "[ERROR] ";

    // 日志级别前缀常量（彩色版本）
    // 用于支持颜色的终端环境，使用绿色作为日志级别标识的颜色

    /**
     * TRACE级别日志前缀（彩色版本）
     *
     * <p>使用绿色显示"TRACE"标识
     */
    private static final String TRACE_COLOR_PREFIX = "[" + colorStr("TRACE", GREEN) + "] ";

    /**
     * DEBUG级别日志前缀（彩色版本）
     *
     * <p>使用绿色显示"DEBUG"标识
     */
    private static final String DEBUG_COLOR_PREFIX = "[" + colorStr("DEBUG", GREEN) + "] ";

    /**
     * INFO级别日志前缀（彩色版本）
     *
     * <p>使用绿色显示"INFO"标识
     */
    private static final String INFO_COLOR_PREFIX = "[" + colorStr("INFO", GREEN) + "] ";

    /**
     * WARN级别日志前缀（彩色版本）
     *
     * <p>使用黄色显示"WARN"标识，以突出警告信息
     */
    private static final String WARN_COLOR_PREFIX = "[" + colorStr("WARN", YELLOW) + "] ";

    /**
     * ERROR级别日志前缀（彩色版本）
     *
     * <p>使用红色显示"ERROR"标识，以突出错误信息
     */
    private static final String ERROR_COLOR_PREFIX = "[" + colorStr("ERROR", RED) + "] ";

    /**
     * 静态初始化块
     *
     * <p>在类加载时执行，用于自动检测当前环境是否支持ANSI颜色代码。
     * 检测逻辑：
     * <ol>
     *   <li>首先检查是否有控制台（System.console()不为null）</li>
     *   <li>如果是Windows DOS环境，禁用颜色（因为Windows DOS默认不支持ANSI颜色）</li>
     *   <li>如果是Cygwin或MinGW环境，启用颜色（这些环境模拟Unix终端，支持ANSI颜色）</li>
     * </ol>
     */
    static {
        try {
            // 检查是否有控制台，有控制台才考虑启用颜色
            if (System.console() != null) {
                enableColor = true;
                // Windows DOS不支持颜色，禁用
                if (OSUtils.isWindows()) {
                    enableColor = false;
                }
            }
            // Cygwin和MinGW模拟Unix终端，支持颜色
            if (OSUtils.isCygwinOrMinGW()) {
                enableColor = true;
            }
        } catch (Throwable t) {
            // 忽略任何异常，保持默认禁用状态
            // 这里捕获异常是为了保证在特殊环境下不会因为初始化失败导致类无法加载
        }
    }

    /**
     * 私有构造函数
     *
     * <p>防止实例化，因为这是一个工具类，所有方法都是静态的
     */
    private AnsiLog() {
    }

    /**
     * 获取当前是否启用彩色输出
     *
     * @return 如果启用彩色输出返回true，否则返回false
     */
    public static boolean enableColor() {
        return enableColor;
    }

    /**
     * 设置日志输出流
     *
     * <p>允许将日志输出重定向到任意PrintStream，例如文件流。
     * 如果传入null，则重置为System.out。
     *
     * @param printStream 新的输出流，传入null时使用System.out
     * @return 设置之前的输出流，方便调用者恢复
     */
    public static PrintStream out(PrintStream printStream) {
        // 保存旧的输出流，用于返回给调用者
        PrintStream old = out;
        // 如果传入null，使用System.out；否则使用传入的流
        out = printStream == null ? System.out : printStream;
        return old;
    }

    /**
     * 获取当前日志输出流
     *
     * @return 当前正在使用的输出流
     */
    public static PrintStream out() {
        return out;
    }

    /**
     * 设置日志级别
     *
     * <p>只有等于或高于设置级别的日志才会被输出。
     * 例如，设置为Level.INFO时，INFO、WARN、ERROR级别的日志会输出，
     * 而TRACE和DEBUG级别的日志会被过滤掉。
     *
     * @param level 新的日志级别
     * @return 设置之前的日志级别，方便调用者恢复
     * @see java.util.logging.Level
     */
    public static Level level(Level level) {
        // 保存旧的日志级别，用于返回给调用者
        Level old = LEVEL;
        // 设置新的日志级别
        LEVEL = level;
        return old;
    }

    /**
     * 获取当前日志级别
     *
     * @return 当前日志级别
     */
    public static Level level() {
        return LEVEL;
    }

    /**
     * 将文本设置为黑色
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI黑色代码的文本；否则返回原文本
     */
    public static String black(String msg) {
        if (enableColor) {
            return colorStr(msg, BLACK);
        } else {
            return msg;
        }
    }

    /**
     * 将文本设置为红色
     *
     * <p>通常用于错误信息或需要突出显示的重要文本
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI红色代码的文本；否则返回原文本
     */
    public static String red(String msg) {
        if (enableColor) {
            return colorStr(msg, RED);
        } else {
            return msg;
        }
    }

    /**
     * 将文本设置为绿色
     *
     * <p>通常用于成功信息或一般性提示文本
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI绿色代码的文本；否则返回原文本
     */
    public static String green(String msg) {
        if (enableColor) {
            return colorStr(msg, GREEN);
        } else {
            return msg;
        }
    }

    /**
     * 将文本设置为黄色
     *
     * <p>通常用于警告信息
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI黄色代码的文本；否则返回原文本
     */
    public static String yellow(String msg) {
        if (enableColor) {
            return colorStr(msg, YELLOW);
        } else {
            return msg;
        }
    }

    /**
     * 将文本设置为蓝色
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI蓝色代码的文本；否则返回原文本
     */
    public static String blue(String msg) {
        if (enableColor) {
            return colorStr(msg, BLUE);
        } else {
            return msg;
        }
    }

    /**
     * 将文本设置为品红色（洋红色）
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI品红色代码的文本；否则返回原文本
     */
    public static String magenta(String msg) {
        if (enableColor) {
            return colorStr(msg, MAGENTA);
        } else {
            return msg;
        }
    }

    /**
     * 将文本设置为青色（蓝绿色）
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI青色代码的文本；否则返回原文本
     */
    public static String cyan(String msg) {
        if (enableColor) {
            return colorStr(msg, CYAN);
        } else {
            return msg;
        }
    }

    /**
     * 将文本设置为白色
     *
     * @param msg 要着色的文本
     * @return 如果启用颜色，返回带ANSI白色代码的文本；否则返回原文本
     */
    public static String white(String msg) {
        if (enableColor) {
            return colorStr(msg, WHITE);
        } else {
            return msg;
        }
    }

    /**
     * 为文本添加ANSI颜色代码
     *
     * <p>这是一个内部方法，用于生成带颜色转义序列的文本。
     * ANSI转义序列格式：\033[颜色代码m文本\033[0m
     * 其中\033[是ESC字符的八进制表示，[是CSI（控制序列引导符），
     * m表示设置图形模式，0m表示重置所有属性。
     *
     * @param msg 要着色的文本
     * @param colorCode ANSI颜色代码（30-37）
     * @return 带ANSI颜色代码的文本，末尾带有重置代码
     */
    private static String colorStr(String msg, int colorCode) {
        // 构造ANSI颜色转义序列：\033[颜色代码m + 文本 + 重置代码
        return "\033[" + colorCode + "m" + msg + RESET;
    }

    /**
     * 输出TRACE级别日志
     *
     * <p>TRACE是最详细的跟踪信息，通常用于追踪程序的执行流程。
     * 只有当日志级别允许时才会输出。
     *
     * @param msg 日志消息
     */
    public static void trace(String msg) {
        // 检查是否可以输出此级别的日志
        if (canLog(Level.FINEST)) {
            if (enableColor) {
                // 使用彩色前缀输出
                out.println(TRACE_COLOR_PREFIX + msg);
            } else {
                // 使用普通前缀输出
                out.println(TRACE_PREFIX + msg);
            }
        }
    }

    /**
     * 输出带格式化参数的TRACE级别日志
     *
     * <p>支持占位符{}，会被参数依次替换。
     * 例如：trace("Hello {}", "world") 会输出 "Hello world"
     *
     * @param format 格式化字符串，使用{}作为占位符
     * @param arguments 可变参数，用于替换占位符
     */
    public static void trace(String format, Object... arguments) {
        if (canLog(Level.FINEST)) {
            // 格式化消息后调用单参数版本的trace方法
            trace(format(format, arguments));
        }
    }

    /**
     * 输出异常堆栈跟踪信息（TRACE级别）
     *
     * <p>将异常的堆栈跟踪信息输出到当前输出流
     *
     * @param t 要输出的异常对象
     */
    public static void trace(Throwable t) {
        if (canLog(Level.FINEST)) {
            // 将异常堆栈输出到配置的输出流，而不是System.err
            t.printStackTrace(out);
        }
    }

    /**
     * 输出DEBUG级别日志
     *
     * <p>DEBUG级别用于调试信息，通常在开发和测试阶段使用。
     *
     * @param msg 日志消息
     */
    public static void debug(String msg) {
        // 检查是否可以输出此级别的日志
        if (canLog(Level.FINER)) {
            if (enableColor) {
                // 使用彩色前缀输出
                out.println(DEBUG_COLOR_PREFIX + msg);
            } else {
                // 使用普通前缀输出
                out.println(DEBUG_PREFIX + msg);
            }
        }
    }

    /**
     * 输出带格式化参数的DEBUG级别日志
     *
     * <p>支持占位符{}，会被参数依次替换。
     * 例如：debug("Value: {}", 42) 会输出 "Value: 42"
     *
     * @param format 格式化字符串，使用{}作为占位符
     * @param arguments 可变参数，用于替换占位符
     */
    public static void debug(String format, Object... arguments) {
        if (canLog(Level.FINER)) {
            // 格式化消息后调用单参数版本的debug方法
            debug(format(format, arguments));
        }
    }

    /**
     * 输出异常堆栈跟踪信息（DEBUG级别）
     *
     * <p>将异常的堆栈跟踪信息输出到当前输出流
     *
     * @param t 要输出的异常对象
     */
    public static void debug(Throwable t) {
        if (canLog(Level.FINER)) {
            // 将异常堆栈输出到配置的输出流
            t.printStackTrace(out);
        }
    }

    /**
     * 输出INFO级别日志
     *
     * <p>INFO级别用于一般性信息，是默认的日志级别。
     * 通常用于记录程序正常运行过程中的重要信息。
     *
     * @param msg 日志消息
     */
    public static void info(String msg) {
        // 检查是否可以输出此级别的日志
        if (canLog(Level.CONFIG)) {
            if (enableColor) {
                // 使用彩色前缀输出
                out.println(INFO_COLOR_PREFIX + msg);
            } else {
                // 使用普通前缀输出
                out.println(INFO_PREFIX + msg);
            }
        }
    }

    /**
     * 输出带格式化参数的INFO级别日志
     *
     * <p>支持占位符{}，会被参数依次替换。
     * 例如：info("Server started on port {}", 8080) 会输出 "Server started on port 8080"
     *
     * @param format 格式化字符串，使用{}作为占位符
     * @param arguments 可变参数，用于替换占位符
     */
    public static void info(String format, Object... arguments) {
        if (canLog(Level.CONFIG)) {
            // 格式化消息后调用单参数版本的info方法
            info(format(format, arguments));
        }
    }

    /**
     * 输出异常堆栈跟踪信息（INFO级别）
     *
     * <p>将异常的堆栈跟踪信息输出到当前输出流
     *
     * @param t 要输出的异常对象
     */
    public static void info(Throwable t) {
        if (canLog(Level.CONFIG)) {
            // 将异常堆栈输出到配置的输出流
            t.printStackTrace(out);
        }
    }

    /**
     * 输出WARN级别日志
     *
     * <p>WARN级别用于警告信息，表示可能出现问题但程序仍可继续运行。
     *
     * @param msg 日志消息
     */
    public static void warn(String msg) {
        // 检查是否可以输出此级别的日志
        if (canLog(Level.WARNING)) {
            if (enableColor) {
                // 使用彩色前缀输出（黄色）
                out.println(WARN_COLOR_PREFIX + msg);
            } else {
                // 使用普通前缀输出
                out.println(WARN_PREFIX + msg);
            }
        }
    }

    /**
     * 输出带格式化参数的WARN级别日志
     *
     * <p>支持占位符{}，会被参数依次替换。
     * 例如：warn("Deprecated API: {}", "oldMethod") 会输出 "Deprecated API: oldMethod"
     *
     * @param format 格式化字符串，使用{}作为占位符
     * @param arguments 可变参数，用于替换占位符
     */
    public static void warn(String format, Object... arguments) {
        if (canLog(Level.WARNING)) {
            // 格式化消息后调用单参数版本的warn方法
            warn(format(format, arguments));
        }
    }

    /**
     * 输出异常堆栈跟踪信息（WARN级别）
     *
     * <p>将异常的堆栈跟踪信息输出到当前输出流
     *
     * @param t 要输出的异常对象
     */
    public static void warn(Throwable t) {
        if (canLog(Level.WARNING)) {
            // 将异常堆栈输出到配置的输出流
            t.printStackTrace(out);
        }
    }

    /**
     * 输出ERROR级别日志
     *
     * <p>ERROR级别用于错误信息，表示程序遇到错误但可能仍能继续运行。
     *
     * @param msg 日志消息
     */
    public static void error(String msg) {
        // 检查是否可以输出此级别的日志
        if (canLog(Level.SEVERE)) {
            if (enableColor) {
                // 使用彩色前缀输出（红色）
                out.println(ERROR_COLOR_PREFIX + msg);
            } else {
                // 使用普通前缀输出
                out.println(ERROR_PREFIX + msg);
            }
        }
    }

    /**
     * 输出带格式化参数的ERROR级别日志
     *
     * <p>支持占位符{}，会被参数依次替换。
     * 例如：error("Failed to connect to {}: {}", "localhost", "Connection refused")
     *
     * @param format 格式化字符串，使用{}作为占位符
     * @param arguments 可变参数，用于替换占位符
     */
    public static void error(String format, Object... arguments) {
        if (canLog(Level.SEVERE)) {
            // 格式化消息后调用单参数版本的error方法
            error(format(format, arguments));
        }
    }

    /**
     * 输出异常堆栈跟踪信息（ERROR级别）
     *
     * <p>将异常的堆栈跟踪信息输出到当前输出流
     *
     * @param t 要输出的异常对象
     */
    public static void error(Throwable t) {
        if (canLog(Level.SEVERE)) {
            // 将异常堆栈输出到配置的输出流
            t.printStackTrace(out);
        }
    }

    /**
     * 格式化日志消息
     *
     * <p>将格式化字符串中的占位符{}替换为实际参数值。
     * 这是一个简单的占位符替换实现，按顺序替换每一个{}。
     *
     * <p>注意：
     * <ul>
     *   <li>占位符必须是{}的形式</li>
     *   <li>占位符按顺序被替换，每个{}对应一个参数</li>
     *   <li>如果参数中包含正则表达式特殊字符，使用Matcher.quoteReplacement进行处理</li>
     * </ul>
     *
     * @param from 格式化字符串，包含{}占位符
     * @param arguments 用于替换占位符的参数数组
     * @return 格式化后的字符串，如果from为null则返回null
     */
    private static String format(String from, Object... arguments) {
        if (from != null) {
            // 保存当前格式化状态
            String computed = from;
            // 如果有参数，依次替换占位符
            if (arguments != null && arguments.length != 0) {
                for (Object argument : arguments) {
                    // 使用正则表达式替换第一个{}占位符
                    // Matcher.quoteReplacement确保参数中的特殊字符被正确处理
                    computed = computed.replaceFirst("\\{\\}", Matcher.quoteReplacement(String.valueOf(argument)));
                }
            }
            return computed;
        }
        return null;
    }

    /**
     * 检查是否可以输出指定级别的日志
     *
     * <p>通过比较日志级别的整数值来判断。
     * Java util logging的Level使用整数表示级别，数值越大级别越高。
     * 例如：SEVERE(1000) > WARNING(900) > INFO(800)
     *
     * <p>只有当要输出的日志级别大于或等于当前设置的日志级别时才返回true。
     *
     * @param level 要检查的日志级别
     * @return 如果可以输出该级别的日志返回true，否则返回false
     */
    private static boolean canLog(Level level) {
        // 比较日志级别的整数值
        // 只有大于或等于当前级别的日志才能输出
        return level.intValue() >= LEVEL.intValue();
    }
}
