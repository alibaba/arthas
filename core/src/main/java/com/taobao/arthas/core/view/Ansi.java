package com.taobao.arthas.core.view;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Provides a fluent API for generating ANSI escape sequences.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @since 1.0
 */
public class Ansi {

    private static final char FIRST_ESC_CHAR = 27;
    private static final char SECOND_ESC_CHAR = '[';

    public static enum Color {
        BLACK(0, "BLACK"),
        RED(1, "RED"),
        GREEN(2, "GREEN"),
        YELLOW(3, "YELLOW"),
        BLUE(4, "BLUE"),
        MAGENTA(5, "MAGENTA"),
        CYAN(6, "CYAN"),
        WHITE(7, "WHITE"),
        DEFAULT(9, "DEFAULT");

        private final int value;
        private final String name;

        Color(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public int value() {
            return value;
        }

        public int fg() {
            return value + 30;
        }

        public int bg() {
            return value + 40;
        }

        public int fgBright() {
            return value + 90;
        }

        public int bgBright() {
            return value + 100;
        }
    }

    public static enum Attribute {
        RESET(0, "RESET"),
        INTENSITY_BOLD(1, "INTENSITY_BOLD"),
        INTENSITY_FAINT(2, "INTENSITY_FAINT"),
        ITALIC(3, "ITALIC_ON"),
        UNDERLINE(4, "UNDERLINE_ON"),
        BLINK_SLOW(5, "BLINK_SLOW"),
        BLINK_FAST(6, "BLINK_FAST"),
        NEGATIVE_ON(7, "NEGATIVE_ON"),
        CONCEAL_ON(8, "CONCEAL_ON"),
        STRIKETHROUGH_ON(9, "STRIKETHROUGH_ON"),
        UNDERLINE_DOUBLE(21, "UNDERLINE_DOUBLE"),
        INTENSITY_BOLD_OFF(22, "INTENSITY_BOLD_OFF"),
        ITALIC_OFF(23, "ITALIC_OFF"),
        UNDERLINE_OFF(24, "UNDERLINE_OFF"),
        BLINK_OFF(25, "BLINK_OFF"),
        NEGATIVE_OFF(27, "NEGATIVE_OFF"),
        CONCEAL_OFF(28, "CONCEAL_OFF"),
        STRIKETHROUGH_OFF(29, "STRIKETHROUGH_OFF");

        private final int value;
        private final String name;

        Attribute(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public int value() {
            return value;
        }

    }

    public static enum Erase {
        FORWARD(0, "FORWARD"),
        BACKWARD(1, "BACKWARD"),
        ALL(2, "ALL");

        private final int value;
        private final String name;

        Erase(int index, String name) {
            this.value = index;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public int value() {
            return value;
        }
    }

    public static final String DISABLE = Ansi.class.getName() + ".disable";

    private static Callable<Boolean> detector = new Callable<Boolean>() {
        public Boolean call() throws Exception {
            return !Boolean.getBoolean(DISABLE);
        }
    };

    public static void setDetector(final Callable<Boolean> detector) {
        if (detector == null) {
            throw new IllegalArgumentException();
        }
        Ansi.detector = detector;
    }

    public static boolean isDetected() {
        try {
            return detector.call();
        } catch (Exception e) {
            return true;
        }
    }

    private static final InheritableThreadLocal<Boolean> holder = new InheritableThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return isDetected();
        }
    };

    public static void setEnabled(final boolean flag) {
        holder.set(flag);
    }

    public static boolean isEnabled() {
        return holder.get();
    }

    public static Ansi ansi() {
        if (isEnabled()) {
            return new Ansi();
        } else {
            return new NoAnsi();
        }
    }

    public static Ansi ansi(StringBuilder builder) {
        if (isEnabled()) {
            return new Ansi(builder);
        } else {
            return new NoAnsi(builder);
        }
    }

    public static Ansi ansi(int size) {
        if (isEnabled()) {
            return new Ansi(size);
        } else {
            return new NoAnsi(size);
        }
    }

    private static class NoAnsi
            extends Ansi {
        public NoAnsi() {
            super();
        }

        public NoAnsi(int size) {
            super(size);
        }

        public NoAnsi(StringBuilder builder) {
            super(builder);
        }

        @Override
        public Ansi fg(Color color) {
            return this;
        }

        @Override
        public Ansi bg(Color color) {
            return this;
        }

        @Override
        public Ansi fgBright(Color color) {
            return this;
        }

        @Override
        public Ansi bgBright(Color color) {
            return this;
        }

        @Override
        public Ansi a(Attribute attribute) {
            return this;
        }

        @Override
        public Ansi cursor(int x, int y) {
            return this;
        }

        @Override
        public Ansi cursorToColumn(int x) {
            return this;
        }

        @Override
        public Ansi cursorUp(int y) {
            return this;
        }

        @Override
        public Ansi cursorRight(int x) {
            return this;
        }

        @Override
        public Ansi cursorDown(int y) {
            return this;
        }

        @Override
        public Ansi cursorLeft(int x) {
            return this;
        }

        @Override
        public Ansi cursorDownLine() {
            return this;
        }

        @Override
        public Ansi cursorDownLine(final int n) {
            return this;
        }

        @Override
        public Ansi cursorUpLine() {
            return this;
        }

        @Override
        public Ansi cursorUpLine(final int n) {
            return this;
        }

        @Override
        public Ansi eraseScreen() {
            return this;
        }

        @Override
        public Ansi eraseScreen(Erase kind) {
            return this;
        }

        @Override
        public Ansi eraseLine() {
            return this;
        }

        @Override
        public Ansi eraseLine(Erase kind) {
            return this;
        }

        @Override
        public Ansi scrollUp(int rows) {
            return this;
        }

        @Override
        public Ansi scrollDown(int rows) {
            return this;
        }

        @Override
        public Ansi saveCursorPosition() {
            return this;
        }

        @Override
        @Deprecated
        public Ansi restorCursorPosition() {
            return this;
        }

        @Override
        public Ansi restoreCursorPosition() {
            return this;
        }

        @Override
        public Ansi reset() {
            return this;
        }
    }

    private final StringBuilder builder;
    private final ArrayList<Integer> attributeOptions = new ArrayList<Integer>(5);

    public Ansi() {
        this(new StringBuilder());
    }

    public Ansi(Ansi parent) {
        this(new StringBuilder(parent.builder));
        attributeOptions.addAll(parent.attributeOptions);
    }

    public Ansi(int size) {
        this(new StringBuilder(size));
    }

    public Ansi(StringBuilder builder) {
        this.builder = builder;
    }

    public Ansi fg(Color color) {
        attributeOptions.add(color.fg());
        return this;
    }

    public Ansi fgBlack() {
        return this.fg(Color.BLACK);
    }

    public Ansi fgBlue() {
        return this.fg(Color.BLUE);
    }

    public Ansi fgCyan() {
        return this.fg(Color.CYAN);
    }

    public Ansi fgDefault() {
        return this.fg(Color.DEFAULT);
    }

    public Ansi fgGreen() {
        return this.fg(Color.GREEN);
    }

    public Ansi fgMagenta() {
        return this.fg(Color.MAGENTA);
    }

    public Ansi fgRed() {
        return this.fg(Color.RED);
    }

    public Ansi fgYellow() {
        return this.fg(Color.YELLOW);
    }

    public Ansi bg(Color color) {
        attributeOptions.add(color.bg());
        return this;
    }

    public Ansi bgCyan() {
        return this.bg(Color.CYAN);
    }

    public Ansi bgDefault() {
        return this.bg(Color.DEFAULT);
    }

    public Ansi bgGreen() {
        return this.bg(Color.GREEN);
    }

    public Ansi bgMagenta() {
        return this.bg(Color.MAGENTA);
    }

    public Ansi bgRed() {
        return this.bg(Color.RED);
    }

    public Ansi bgYellow() {
        return this.bg(Color.YELLOW);
    }

    public Ansi fgBright(Color color) {
        attributeOptions.add(color.fgBright());
        return this;
    }

    public Ansi fgBrightBlack() {
        return this.fgBright(Color.BLACK);
    }

    public Ansi fgBrightBlue() {
        return this.fgBright(Color.BLUE);
    }

    public Ansi fgBrightCyan() {
        return this.fgBright(Color.CYAN);
    }

    public Ansi fgBrightDefault() {
        return this.fgBright(Color.DEFAULT);
    }

    public Ansi fgBrightGreen() {
        return this.fgBright(Color.GREEN);
    }

    public Ansi fgBrightMagenta() {
        return this.fgBright(Color.MAGENTA);
    }

    public Ansi fgBrightRed() {
        return this.fgBright(Color.RED);
    }

    public Ansi fgBrightYellow() {
        return this.fgBright(Color.YELLOW);
    }

    public Ansi bgBright(Color color) {
        attributeOptions.add(color.bgBright());
        return this;
    }

    public Ansi bgBrightCyan() {
        return this.fgBright(Color.CYAN);
    }

    public Ansi bgBrightDefault() {
        return this.bgBright(Color.DEFAULT);
    }

    public Ansi bgBrightGreen() {
        return this.bgBright(Color.GREEN);
    }

    public Ansi bgBrightMagenta() {
        return this.bg(Color.MAGENTA);
    }

    public Ansi bgBrightRed() {
        return this.bgBright(Color.RED);
    }

    public Ansi bgBrightYellow() {
        return this.bgBright(Color.YELLOW);
    }

    public Ansi a(Attribute attribute) {
        attributeOptions.add(attribute.value());
        return this;
    }

    public Ansi cursor(final int x, final int y) {
        return appendEscapeSequence('H', x, y);
    }

    public Ansi cursorToColumn(final int x) {
        return appendEscapeSequence('G', x);
    }

    public Ansi cursorUp(final int y) {
        return appendEscapeSequence('A', y);
    }

    public Ansi cursorDown(final int y) {
        return appendEscapeSequence('B', y);
    }

    public Ansi cursorRight(final int x) {
        return appendEscapeSequence('C', x);
    }

    public Ansi cursorLeft(final int x) {
        return appendEscapeSequence('D', x);
    }

    public Ansi cursorDownLine() {
        return appendEscapeSequence('E');
    }

    public Ansi cursorDownLine(final int n) {
        return appendEscapeSequence('E', n);
    }

    public Ansi cursorUpLine() {
        return appendEscapeSequence('F');
    }

    public Ansi cursorUpLine(final int n) {
        return appendEscapeSequence('F', n);
    }

    public Ansi eraseScreen() {
        return appendEscapeSequence('J', Erase.ALL.value());
    }

    public Ansi eraseScreen(final Erase kind) {
        return appendEscapeSequence('J', kind.value());
    }

    public Ansi eraseLine() {
        return appendEscapeSequence('K');
    }

    public Ansi eraseLine(final Erase kind) {
        return appendEscapeSequence('K', kind.value());
    }

    public Ansi scrollUp(final int rows) {
        return appendEscapeSequence('S', rows);
    }

    public Ansi scrollDown(final int rows) {
        return appendEscapeSequence('T', rows);
    }

    public Ansi saveCursorPosition() {
        return appendEscapeSequence('s');
    }

    @Deprecated
    public Ansi restorCursorPosition() {
        return appendEscapeSequence('u');
    }

    public Ansi restoreCursorPosition() {
        return appendEscapeSequence('u');
    }

    public Ansi reset() {
        return a(Attribute.RESET);
    }

    public Ansi bold() {
        return a(Attribute.INTENSITY_BOLD);
    }

    public Ansi boldOff() {
        return a(Attribute.INTENSITY_BOLD_OFF);
    }

    public Ansi a(String value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(boolean value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(char value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(char[] value, int offset, int len) {
        flushAttributes();
        builder.append(value, offset, len);
        return this;
    }

    public Ansi a(char[] value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(CharSequence value, int start, int end) {
        flushAttributes();
        builder.append(value, start, end);
        return this;
    }

    public Ansi a(CharSequence value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(double value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(float value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(int value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(long value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(Object value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi a(StringBuffer value) {
        flushAttributes();
        builder.append(value);
        return this;
    }

    public Ansi newline() {
        flushAttributes();
        builder.append(System.getProperty("line.separator"));
        return this;
    }

    public Ansi format(String pattern, Object... args) {
        flushAttributes();
        builder.append(String.format(pattern, args));
        return this;
    }


    @Override
    public String toString() {
        flushAttributes();
        return builder.toString();
    }

    ///////////////////////////////////////////////////////////////////
    // Private Helper Methods
    ///////////////////////////////////////////////////////////////////

    private Ansi appendEscapeSequence(char command) {
        flushAttributes();
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        builder.append(command);
        return this;
    }

    private Ansi appendEscapeSequence(char command, int option) {
        flushAttributes();
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        builder.append(option);
        builder.append(command);
        return this;
    }

    private Ansi appendEscapeSequence(char command, Object... options) {
        flushAttributes();
        return _appendEscapeSequence(command, options);
    }

    private void flushAttributes() {
        if (attributeOptions.isEmpty()) {
            return;
        }
        if (attributeOptions.size() == 1 && attributeOptions.get(0) == 0) {
            builder.append(FIRST_ESC_CHAR);
            builder.append(SECOND_ESC_CHAR);
            builder.append('m');
        } else {
            _appendEscapeSequence('m', attributeOptions.toArray());
        }
        attributeOptions.clear();
    }

    private Ansi _appendEscapeSequence(char command, Object... options) {
        builder.append(FIRST_ESC_CHAR);
        builder.append(SECOND_ESC_CHAR);
        int size = options.length;
        for (int i = 0; i < size; i++) {
            if (i != 0) {
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