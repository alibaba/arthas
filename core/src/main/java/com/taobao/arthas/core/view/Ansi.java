package com.taobao.arthas.core.view;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * ANSI转义序列生成器
 * 提供流式API用于生成ANSI转义序列，用于在终端中控制颜色、光标位置等
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @since 1.0
 */
public class Ansi {

    // ANSI转义序列的第一个字符：ESC（escape字符，ASCII码27）
    private static final char FIRST_ESC_CHAR = 27;
    // ANSI转义序列的第二个字符：[
    private static final char SECOND_ESC_CHAR = '[';

    /**
     * 颜色枚举
     * 定义了ANSI支持的标准颜色及其对应的索引值
     */
    public static enum Color {
        // 黑色
        BLACK(0, "BLACK"),
        // 红色
        RED(1, "RED"),
        // 绿色
        GREEN(2, "GREEN"),
        // 黄色
        YELLOW(3, "YELLOW"),
        // 蓝色
        BLUE(4, "BLUE"),
        // 品红色/洋红色
        MAGENTA(5, "MAGENTA"),
        // 青色
        CYAN(6, "CYAN"),
        // 白色
        WHITE(7, "WHITE"),
        // 默认颜色
        DEFAULT(9, "DEFAULT");

        // 颜色的索引值
        private final int value;
        // 颜色的名称
        private final String name;

        /**
         * 构造颜色枚举
         * @param index 颜色的索引值
         * @param name 颜色的名称
         */
        Color(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * 获取颜色的索引值
         * @return 颜色索引值
         */
        public int value() {
            return value;
        }

        /**
         * 获取前景色（文字颜色）的ANSI码
         * 标准前景色使用30-37范围
         * @return 前景色ANSI码
         */
        public int fg() {
            return value + 30;
        }

        /**
         * 获取背景色的ANSI码
         * 标准背景色使用40-47范围
         * @return 背景色ANSI码
         */
        public int bg() {
            return value + 40;
        }

        /**
         * 获取亮前景色（亮文字颜色）的ANSI码
         * 亮前景色使用90-97范围
         * @return 亮前景色ANSI码
         */
        public int fgBright() {
            return value + 90;
        }

        /**
         * 获取亮背景色的ANSI码
         * 亮背景色使用100-107范围
         * @return 亮背景色ANSI码
         */
        public int bgBright() {
            return value + 100;
        }
    }

    /**
     * 文本属性枚举
     * 定义了ANSI支持的各种文本显示属性，如粗体、斜体、下划线等
     */
    public static enum Attribute {
        // 重置所有属性
        RESET(0, "RESET"),
        // 粗体/高亮
        INTENSITY_BOLD(1, "INTENSITY_BOLD"),
        // 暗淡
        INTENSITY_FAINT(2, "INTENSITY_FAINT"),
        // 斜体
        ITALIC(3, "ITALIC_ON"),
        // 下划线
        UNDERLINE(4, "UNDERLINE_ON"),
        // 慢速闪烁
        BLINK_SLOW(5, "BLINK_SLOW"),
        // 快速闪烁
        BLINK_FAST(6, "BLINK_FAST"),
        // 反色显示（前景色和背景色互换）
        NEGATIVE_ON(7, "NEGATIVE_ON"),
        // 隐藏（文字不可见，但可以被复制）
        CONCEAL_ON(8, "CONCEAL_ON"),
        // 删除线
        STRIKETHROUGH_ON(9, "STRIKETHROUGH_ON"),
        // 双下划线
        UNDERLINE_DOUBLE(21, "UNDERLINE_DOUBLE"),
        // 关闭粗体
        INTENSITY_BOLD_OFF(22, "INTENSITY_BOLD_OFF"),
        // 关闭斜体
        ITALIC_OFF(23, "ITALIC_OFF"),
        // 关闭下划线
        UNDERLINE_OFF(24, "UNDERLINE_OFF"),
        // 关闭闪烁
        BLINK_OFF(25, "BLINK_OFF"),
        // 关闭反色
        NEGATIVE_OFF(27, "NEGATIVE_OFF"),
        // 关闭隐藏
        CONCEAL_OFF(28, "CONCEAL_OFF"),
        // 关闭删除线
        STRIKETHROUGH_OFF(29, "STRIKETHROUGH_OFF");

        // 属性的ANSI码值
        private final int value;
        // 属性的名称
        private final String name;

        /**
         * 构造属性枚举
         * @param index 属性的ANSI码值
         * @param name 属性的名称
         */
        Attribute(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * 获取属性的ANSI码值
         * @return ANSI码值
         */
        public int value() {
            return value;
        }

    }

    /**
     * 擦除模式枚举
     * 定义了屏幕或行擦除的范围
     */
    public static enum Erase {
        // 从光标位置擦除到屏幕/行末尾
        FORWARD(0, "FORWARD"),
        // 从光标位置擦除到屏幕/行开头
        BACKWARD(1, "BACKWARD"),
        // 擦除整个屏幕/行
        ALL(2, "ALL");

        // 擦除模式的ANSI码值
        private final int value;
        // 擦除模式的名称
        private final String name;

        /**
         * 构造擦除模式枚举
         * @param index 擦除模式的ANSI码值
         * @param name 擦除模式的名称
         */
        Erase(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        /**
         * 获取擦除模式的ANSI码值
         * @return ANSI码值
         */
        public int value() {
            return value;
        }
    }

    // 禁用ANSI输出的系统属性名
    public static final String DISABLE = Ansi.class.getName() + ".disable";

    // ANSI检测器，用于检测当前终端是否支持ANSI
    private static Callable<Boolean> detector = new Callable<Boolean>() {
        public Boolean call() throws Exception {
            // 如果系统属性设置为禁用，则返回false，否则返回true
            return !Boolean.getBoolean(DISABLE);
        }
    };

    /**
     * 设置自定义的ANSI检测器
     * @param detector 检测器，不能为null
     */
    public static void setDetector(final Callable<Boolean> detector) {
        if (detector == null) {
            throw new IllegalArgumentException();
        }
        Ansi.detector = detector;
    }

    /**
     * 检测当前终端是否支持ANSI
     * @return 如果支持返回true，否则返回false
     */
    public static boolean isDetected() {
        try {
            return detector.call();
        } catch (Exception e) {
            // 如果检测失败，默认认为支持ANSI
            return true;
        }
    }

    // 线程局部的ANSI启用状态，可被子线程继承
    private static final InheritableThreadLocal<Boolean> holder = new InheritableThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            // 初始值由检测器决定
            return isDetected();
        }
    };

    /**
     * 设置是否启用ANSI输出
     * @param flag true表示启用，false表示禁用
     */
    public static void setEnabled(final boolean flag) {
        holder.set(flag);
    }

    /**
     * 检查当前线程是否启用了ANSI输出
     * @return 如果启用返回true，否则返回false
     */
    public static boolean isEnabled() {
        return holder.get();
    }

    /**
     * 创建一个新的Ansi实例
     * 如果ANSI被禁用，则返回NoAnsi实例
     * @return Ansi实例
     */
    public static Ansi ansi() {
        if (isEnabled()) {
            return new Ansi();
        } else {
            return new NoAnsi();
        }
    }

    /**
     * 创建一个新的Ansi实例，使用指定的StringBuilder
     * 如果ANSI被禁用，则返回NoAnsi实例
     * @param builder 用于构建输出字符串的StringBuilder
     * @return Ansi实例
     */
    public static Ansi ansi(StringBuilder builder) {
        if (isEnabled()) {
            return new Ansi(builder);
        } else {
            return new NoAnsi(builder);
        }
    }

    /**
     * 创建一个新的Ansi实例，使用指定初始大小的StringBuilder
     * 如果ANSI被禁用，则返回NoAnsi实例
     * @param size StringBuilder的初始大小
     * @return Ansi实例
     */
    public static Ansi ansi(int size) {
        if (isEnabled()) {
            return new Ansi(size);
        } else {
            return new NoAnsi(size);
        }
    }

    /**
     * 禁用ANSI的内部类
     * 当ANSI被禁用时，所有操作都不产生任何ANSI转义序列
     */
    private static class NoAnsi
            extends Ansi {
        // 默认构造函数
        public NoAnsi() {
            super();
        }

        // 带初始大小的构造函数
        public NoAnsi(int size) {
            super(size);
        }

        // 带StringBuilder的构造函数
        public NoAnsi(StringBuilder builder) {
            super(builder);
        }

        @Override
        public Ansi fg(Color color) {
            // NoAnsi模式下不设置前景色
            return this;
        }

        @Override
        public Ansi bg(Color color) {
            // NoAnsi模式下不设置背景色
            return this;
        }

        @Override
        public Ansi fgBright(Color color) {
            // NoAnsi模式下不设置亮前景色
            return this;
        }

        @Override
        public Ansi bgBright(Color color) {
            // NoAnsi模式下不设置亮背景色
            return this;
        }

        @Override
        public Ansi a(Attribute attribute) {
            // NoAnsi模式下不设置文本属性
            return this;
        }

        @Override
        public Ansi cursor(int x, int y) {
            // NoAnsi模式下不移动光标
            return this;
        }

        @Override
        public Ansi cursorToColumn(int x) {
            // NoAnsi模式下不移动光标到指定列
            return this;
        }

        @Override
        public Ansi cursorUp(int y) {
            // NoAnsi模式下不向上移动光标
            return this;
        }

        @Override
        public Ansi cursorRight(int x) {
            // NoAnsi模式下不向右移动光标
            return this;
        }

        @Override
        public Ansi cursorDown(int y) {
            // NoAnsi模式下不向下移动光标
            return this;
        }

        @Override
        public Ansi cursorLeft(int x) {
            // NoAnsi模式下不向左移动光标
            return this;
        }

        @Override
        public Ansi cursorDownLine() {
            // NoAnsi模式下不移动到下一行
            return this;
        }

        @Override
        public Ansi cursorDownLine(final int n) {
            // NoAnsi模式下不向下移动n行
            return this;
        }

        @Override
        public Ansi cursorUpLine() {
            // NoAnsi模式下不移动到上一行
            return this;
        }

        @Override
        public Ansi cursorUpLine(final int n) {
            // NoAnsi模式下不向上移动n行
            return this;
        }

        @Override
        public Ansi eraseScreen() {
            // NoAnsi模式下不擦除屏幕
            return this;
        }

        @Override
        public Ansi eraseScreen(Erase kind) {
            // NoAnsi模式下不擦除屏幕
            return this;
        }

        @Override
        public Ansi eraseLine() {
            // NoAnsi模式下不擦除行
            return this;
        }

        @Override
        public Ansi eraseLine(Erase kind) {
            // NoAnsi模式下不擦除行
            return this;
        }

        @Override
        public Ansi scrollUp(int rows) {
            // NoAnsi模式下不向上滚动
            return this;
        }

        @Override
        public Ansi scrollDown(int rows) {
            // NoAnsi模式下不向下滚动
            return this;
        }

        @Override
        public Ansi saveCursorPosition() {
            // NoAnsi模式下不保存光标位置
            return this;
        }

        @Override
        @Deprecated
        public Ansi restorCursorPosition() {
            // NoAnsi模式下不恢复光标位置（拼写错误的版本，已废弃）
            return this;
        }

        @Override
        public Ansi restoreCursorPosition() {
            // NoAnsi模式下不恢复光标位置
            return this;
        }

        @Override
        public Ansi reset() {
            // NoAnsi模式下不重置属性
            return this;
        }
    }

    // 用于构建ANSI输出字符串的StringBuilder
    private final StringBuilder builder;
    // 存储待应用的属性选项（颜色、文本属性等）
    private final ArrayList<Integer> attributeOptions = new ArrayList<Integer>(5);

    /**
     * 默认构造函数
     * 创建一个使用默认大小StringBuilder的Ansi实例
     */
    public Ansi() {
        this(new StringBuilder());
    }

    /**
     * 复制构造函数
     * 从父Ansi实例复制状态
     * @param parent 父Ansi实例
     */
    public Ansi(Ansi parent) {
        this(new StringBuilder(parent.builder));
        // 复制父实例的所有属性选项
        attributeOptions.addAll(parent.attributeOptions);
    }

    /**
     * 带初始大小的构造函数
     * @param size StringBuilder的初始大小
     */
    public Ansi(int size) {
        this(new StringBuilder(size));
    }

    /**
     * 带StringBuilder的构造函数
     * @param builder 用于构建输出字符串的StringBuilder
     */
    public Ansi(StringBuilder builder) {
        this.builder = builder;
    }

    /**
     * 设置前景色（文字颜色）
     * @param color 颜色枚举值
     * @return this，支持链式调用
     */
    public Ansi fg(Color color) {
        attributeOptions.add(color.fg());
        return this;
    }

    /**
     * 设置前景色为黑色
     * @return this，支持链式调用
     */
    public Ansi fgBlack() {
        return this.fg(Color.BLACK);
    }

    /**
     * 设置前景色为蓝色
     * @return this，支持链式调用
     */
    public Ansi fgBlue() {
        return this.fg(Color.BLUE);
    }

    /**
     * 设置前景色为青色
     * @return this，支持链式调用
     */
    public Ansi fgCyan() {
        return this.fg(Color.CYAN);
    }

    /**
     * 设置前景色为默认颜色
     * @return this，支持链式调用
     */
    public Ansi fgDefault() {
        return this.fg(Color.DEFAULT);
    }

    /**
     * 设置前景色为绿色
     * @return this，支持链式调用
     */
    public Ansi fgGreen() {
        return this.fg(Color.GREEN);
    }

    /**
     * 设置前景色为品红色
     * @return this，支持链式调用
     */
    public Ansi fgMagenta() {
        return this.fg(Color.MAGENTA);
    }

    /**
     * 设置前景色为红色
     * @return this，支持链式调用
     */
    public Ansi fgRed() {
        return this.fg(Color.RED);
    }

    /**
     * 设置前景色为黄色
     * @return this，支持链式调用
     */
    public Ansi fgYellow() {
        return this.fg(Color.YELLOW);
    }

    /**
     * 设置背景色
     * @param color 颜色枚举值
     * @return this，支持链式调用
     */
    public Ansi bg(Color color) {
        attributeOptions.add(color.bg());
        return this;
    }

    /**
     * 设置背景色为青色
     * @return this，支持链式调用
     */
    public Ansi bgCyan() {
        return this.bg(Color.CYAN);
    }

    /**
     * 设置背景色为默认颜色
     * @return this，支持链式调用
     */
    public Ansi bgDefault() {
        return this.bg(Color.DEFAULT);
    }

    /**
     * 设置背景色为绿色
     * @return this，支持链式调用
     */
    public Ansi bgGreen() {
        return this.bg(Color.GREEN);
    }

    /**
     * 设置背景色为品红色
     * @return this，支持链式调用
     */
    public Ansi bgMagenta() {
        return this.bg(Color.MAGENTA);
    }

    /**
     * 设置背景色为红色
     * @return this，支持链式调用
     */
    public Ansi bgRed() {
        return this.bg(Color.RED);
    }

    /**
     * 设置背景色为黄色
     * @return this，支持链式调用
     */
    public Ansi bgYellow() {
        return this.bg(Color.YELLOW);
    }

    /**
     * 设置亮前景色（高亮文字颜色）
     * @param color 颜色枚举值
     * @return this，支持链式调用
     */
    public Ansi fgBright(Color color) {
        attributeOptions.add(color.fgBright());
        return this;
    }

    /**
     * 设置亮前景色为黑色（暗灰色）
     * @return this，支持链式调用
     */
    public Ansi fgBrightBlack() {
        return this.fgBright(Color.BLACK);
    }

    /**
     * 设置亮前景色为蓝色（亮蓝色）
     * @return this，支持链式调用
     */
    public Ansi fgBrightBlue() {
        return this.fgBright(Color.BLUE);
    }

    /**
     * 设置亮前景色为青色（亮青色）
     * @return this，支持链式调用
     */
    public Ansi fgBrightCyan() {
        return this.fgBright(Color.CYAN);
    }

    /**
     * 设置亮前景色为默认颜色
     * @return this，支持链式调用
     */
    public Ansi fgBrightDefault() {
        return this.fgBright(Color.DEFAULT);
    }

    /**
     * 设置亮前景色为绿色（亮绿色）
     * @return this，支持链式调用
     */
    public Ansi fgBrightGreen() {
        return this.fgBright(Color.GREEN);
    }

    /**
     * 设置亮前景色为品红色（亮品红色）
     * @return this，支持链式调用
     */
    public Ansi fgBrightMagenta() {
        return this.fgBright(Color.MAGENTA);
    }

    /**
     * 设置亮前景色为红色（亮红色）
     * @return this，支持链式调用
     */
    public Ansi fgBrightRed() {
        return this.fgBright(Color.RED);
    }

    /**
     * 设置亮前景色为黄色（亮黄色）
     * @return this，支持链式调用
     */
    public Ansi fgBrightYellow() {
        return this.fgBright(Color.YELLOW);
    }

    /**
     * 设置亮背景色
     * @param color 颜色枚举值
     * @return this，支持链式调用
     */
    public Ansi bgBright(Color color) {
        attributeOptions.add(color.bgBright());
        return this;
    }

    /**
     * 设置亮背景色为青色（注意：这里代码有bug，调用了fgBright）
     * @return this，支持链式调用
     */
    public Ansi bgBrightCyan() {
        return this.fgBright(Color.CYAN);
    }

    /**
     * 设置亮背景色为默认颜色
     * @return this，支持链式调用
     */
    public Ansi bgBrightDefault() {
        return this.bgBright(Color.DEFAULT);
    }

    /**
     * 设置亮背景色为绿色
     * @return this，支持链式调用
     */
    public Ansi bgBrightGreen() {
        return this.bgBright(Color.GREEN);
    }

    /**
     * 设置亮背景色为品红色（注意：这里代码有bug，调用了bg而不是bgBright）
     * @return this，支持链式调用
     */
    public Ansi bgBrightMagenta() {
        return this.bg(Color.MAGENTA);
    }

    /**
     * 设置亮背景色为红色
     * @return this，支持链式调用
     */
    public Ansi bgBrightRed() {
        return this.bgBright(Color.RED);
    }

    /**
     * 设置亮背景色为黄色
     * @return this，支持链式调用
     */
    public Ansi bgBrightYellow() {
        return this.bgBright(Color.YELLOW);
    }

    /**
     * 设置文本属性（如粗体、斜体、下划线等）
     * @param attribute 文本属性枚举值
     * @return this，支持链式调用
     */
    public Ansi a(Attribute attribute) {
        attributeOptions.add(attribute.value());
        return this;
    }

    /**
     * 移动光标到指定位置
     * @param x 列号（从1开始）
     * @param y 行号（从1开始）
     * @return this，支持链式调用
     */
    public Ansi cursor(final int x, final int y) {
        return appendEscapeSequence('H', x, y);
    }

    /**
     * 移动光标到指定列
     * @param x 列号（从1开始）
     * @return this，支持链式调用
     */
    public Ansi cursorToColumn(final int x) {
        return appendEscapeSequence('G', x);
    }

    /**
     * 向上移动光标
     * @param y 移动的行数
     * @return this，支持链式调用
     */
    public Ansi cursorUp(final int y) {
        return appendEscapeSequence('A', y);
    }

    /**
     * 向下移动光标
     * @param y 移动的行数
     * @return this，支持链式调用
     */
    public Ansi cursorDown(final int y) {
        return appendEscapeSequence('B', y);
    }

    /**
     * 向右移动光标
     * @param x 移动的列数
     * @return this，支持链式调用
     */
    public Ansi cursorRight(final int x) {
        return appendEscapeSequence('C', x);
    }

    /**
     * 向左移动光标
     * @param x 移动的列数
     * @return this，支持链式调用
     */
    public Ansi cursorLeft(final int x) {
        return appendEscapeSequence('D', x);
    }

    /**
     * 光标移动到下一行开头
     * @return this，支持链式调用
     */
    public Ansi cursorDownLine() {
        return appendEscapeSequence('E');
    }

    /**
     * 光标向下移动n行到行首
     * @param n 移动的行数
     * @return this，支持链式调用
     */
    public Ansi cursorDownLine(final int n) {
        return appendEscapeSequence('E', n);
    }

    /**
     * 光标移动到上一行开头
     * @return this，支持链式调用
     */
    public Ansi cursorUpLine() {
        return appendEscapeSequence('F');
    }

    /**
     * 光标向上移动n行到行首
     * @param n 移动的行数
     * @return this，支持链式调用
     */
    public Ansi cursorUpLine(final int n) {
        return appendEscapeSequence('F', n);
    }

    /**
     * 擦除整个屏幕
     * @return this，支持链式调用
     */
    public Ansi eraseScreen() {
        return appendEscapeSequence('J', Erase.ALL.value());
    }

    /**
     * 按指定模式擦除屏幕
     * @param kind 擦除模式（向前、向后或全部）
     * @return this，支持链式调用
     */
    public Ansi eraseScreen(final Erase kind) {
        return appendEscapeSequence('J', kind.value());
    }

    /**
     * 擦除当前行
     * @return this，支持链式调用
     */
    public Ansi eraseLine() {
        return appendEscapeSequence('K');
    }

    /**
     * 按指定模式擦除当前行
     * @param kind 擦除模式（向前、向后或全部）
     * @return this，支持链式调用
     */
    public Ansi eraseLine(final Erase kind) {
        return appendEscapeSequence('K', kind.value());
    }

    /**
     * 向上滚动屏幕
     * @param rows 滚动的行数
     * @return this，支持链式调用
     */
    public Ansi scrollUp(final int rows) {
        return appendEscapeSequence('S', rows);
    }

    /**
     * 向下滚动屏幕
     * @param rows 滚动的行数
     * @return this，支持链式调用
     */
    public Ansi scrollDown(final int rows) {
        return appendEscapeSequence('T', rows);
    }

    /**
     * 保存当前光标位置
     * @return this，支持链式调用
     */
    public Ansi saveCursorPosition() {
        return appendEscapeSequence('s');
    }

    /**
     * 恢复光标位置（拼写错误的版本，已废弃）
     * @return this，支持链式调用
     */
    @Deprecated
    public Ansi restorCursorPosition() {
        return appendEscapeSequence('u');
    }

    /**
     * 恢复光标位置
     * @return this，支持链式调用
     */
    public Ansi restoreCursorPosition() {
        return appendEscapeSequence('u');
    }

    /**
     * 重置所有属性为默认值
     * @return this，支持链式调用
     */
    public Ansi reset() {
        return a(Attribute.RESET);
    }

    /**
     * 设置文本为粗体
     * @return this，支持链式调用
     */
    public Ansi bold() {
        return a(Attribute.INTENSITY_BOLD);
    }

    /**
     * 取消粗体设置
     * @return this，支持链式调用
     */
    public Ansi boldOff() {
        return a(Attribute.INTENSITY_BOLD_OFF);
    }

    /**
     * 追加字符串到输出
     * @param value 要追加的字符串
     * @return this，支持链式调用
     */
    public Ansi a(String value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加布尔值到输出
     * @param value 要追加的布尔值
     * @return this，支持链式调用
     */
    public Ansi a(boolean value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加字符到输出
     * @param value 要追加的字符
     * @return this，支持链式调用
     */
    public Ansi a(char value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加字符数组的一部分到输出
     * @param value 字符数组
     * @param offset 起始偏移量
     * @param len 要追加的长度
     * @return this，支持链式调用
     */
    public Ansi a(char[] value, int offset, int len) {
        flushAttributes();
        builder.append(value, offset, len);
        return this;
    }

    /**
     * 追加字符数组到输出
     * @param value 要追加的字符数组
     * @return this，支持链式调用
     */
    public Ansi a(char[] value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加字符序列的一部分到输出
     * @param value 字符序列
     * @param start 起始位置
     * @param end 结束位置
     * @return this，支持链式调用
     */
    public Ansi a(CharSequence value, int start, int end) {
        flushAttributes();
        builder.append(value, start, end);
        return this;
    }

    /**
     * 追加字符序列到输出
     * @param value 要追加的字符序列
     * @return this，支持链式调用
     */
    public Ansi a(CharSequence value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加double值到输出
     * @param value 要追加的double值
     * @return this，支持链式调用
     */
    public Ansi a(double value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加float值到输出
     * @param value 要追加的float值
     * @return this，支持链式调用
     */
    public Ansi a(float value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加int值到输出
     * @param value 要追加的int值
     * @return this，支持链式调用
     */
    public Ansi a(int value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加long值到输出
     * @param value 要追加的long值
     * @return this，支持链式调用
     */
    public Ansi a(long value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加对象到输出（调用对象的toString方法）
     * @param value 要追加的对象
     * @return this，支持链式调用
     */
    public Ansi a(Object value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加StringBuffer到输出
     * @param value 要追加的StringBuffer
     * @return this，支持链式调用
     */
    public Ansi a(StringBuffer value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    /**
     * 追加系统换行符到输出
     * @return this，支持链式调用
     */
    public Ansi newline() {
        flushAttributes();
        builder.append(System.getProperty("line.separator"));
        return this;
    }

    /**
     * 格式化输出字符串
     * @param pattern 格式化模式
     * @param args 格式化参数
     * @return this，支持链式调用
     */
    public Ansi format(String pattern, Object... args) {
        flushAttributes();
        builder.append(String.format(pattern, args));
        return this;
    }


    @Override
    public String toString() {
        // 先刷新所有待应用的属性
        flushAttributes();
        return builder.toString();
    }

    ///////////////////////////////////////////////////////////////////
    // 私有辅助方法
    ///////////////////////////////////////////////////////////////////

    /**
     * 追加不带参数的ANSI转义序列
     * @param command 转义序列的命令字符
     * @return this，支持链式调用
     */
    private Ansi appendEscapeSequence(char command) {
        flushAttributes();
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        builder.append(command);
        return this;
    }

    /**
     * 追加带一个参数的ANSI转义序列
     * @param command 转义序列的命令字符
     * @param option 转义序列的参数
     * @return this，支持链式调用
     */
    private Ansi appendEscapeSequence(char command, int option) {
        flushAttributes();
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        builder.append(option);
        builder.append(command);
        return this;
    }

    /**
     * 追加带多个参数的ANSI转义序列
     * @param command 转义序列的命令字符
     * @param options 转义序列的参数数组
     * @return this，支持链式调用
     */
    private Ansi appendEscapeSequence(char command, Object... options) {
        flushAttributes();
        return _appendEscapeSequence(command, options);
    }

    /**
     * 刷新（应用）所有待处理的属性选项
     * 将累积的颜色、文本属性等转换为ANSI转义序列并输出
     */
    private void flushAttributes() {
        if (attributeOptions.isEmpty()) {
            return;
        }
        // 如果只有重置属性（值为0），使用简化格式
        if (attributeOptions.size() == 1 && attributeOptions.get(0) == 0) {
            builder.append(FIRST_ESC_CHAR);
            builder.append(SECOND_ESC_CHAR);
            builder.append('m');
        } else {
            // 使用完整格式输出所有属性
            _appendEscapeSequence('m', attributeOptions.toArray());
        }
        // 清空已应用的属性选项
        attributeOptions.clear();
    }

    /**
     * 内部方法：追加带多个参数的ANSI转义序列
     * @param command 转义序列的命令字符
     * @param options 转义序列的参数数组
     * @return this，支持链式调用
     */
    private Ansi _appendEscapeSequence(char command, Object... options) {
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        int size = options.length;
        for (int i = 0; i < size; i++) {
            if (i != 0) {
                // 多个参数之间用分号分隔
                builder.append(';');
            }
            if (options[i] != null) {
                builder.append(options[i]);
            }
        }
        builder.append(command);
        return this;
    }

}