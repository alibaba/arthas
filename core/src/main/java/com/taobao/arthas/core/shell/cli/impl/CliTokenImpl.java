package com.taobao.arthas.core.shell.cli.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import io.termd.core.readline.LineStatus;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CliTokenImpl implements CliToken {
    private static final String PIPE = "|";
    private static final String REDIRECT = ">";
    private static final String REDIRECT_APPEND = ">>";

    final boolean text;
    final String raw;
    final String value;

    public CliTokenImpl(boolean text, String value) {
        this(text, value, value);
    }

    public CliTokenImpl(boolean text, String raw, String value) {
        this.text = text;
        this.raw = raw;
        this.value = value;
    }

    @Override
    public boolean isText() {
        return text;
    }

    @Override
    public boolean isBlank() {
        return !text;
    }

    public String raw() {
        return raw;
    }

    public String value() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof CliTokenImpl) {
            CliTokenImpl that = (CliTokenImpl) obj;
            return text == that.text && value.equals(that.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return "CliToken[text=" + text + ",value=" + value + "]";
    }

    public static List<CliToken> tokenize(String s) {

        List<CliToken> tokens = new LinkedList<CliToken>();

        tokenize(s, 0, tokens);

        tokens = adjustTokensForSpecialSymbols(tokens);
        return tokens;

    }

    /**
     * fix pipe char '|' problem: https://github.com/alibaba/arthas/issues/1151
     * supported:
     * 1) thread| grep xxx
     *   [thread|, grep]  -> [thread, |, grep]
     * 2) thread |grep xxx
     *   [thread, |grep] -> [thread, |, grep]
     *
     * unsupported:
     * 3) thread|grep xxx
     * 4) trace -E  classA|classB methodA|methodB|grep classA
     *
     * Also handles redirection of '>' and '>>' in the similar way
     *
     * @param tokens original tokens
     * @return adjusted tokens
     */
    private static List<CliToken> adjustTokensForSpecialSymbols(List<CliToken> tokens) {
        return separateLeadingAndTailingSymbol(tokens, PIPE, REDIRECT_APPEND, REDIRECT);
    }

    private static List<CliToken> separateLeadingAndTailingSymbol(List<CliToken> tokens, String... symbols) {
        List<CliToken> adjustedTokens = new ArrayList<>();
        for (CliToken token : tokens) {
            String value = token.value();
            String raw = token.raw();
            boolean handled = false;
            for (String symbol : symbols) {
                if (value.equals(symbol)) {
                    break;
                } else if (value.endsWith(symbol)) {
                    handled = true;
                    int lastIndexOfSymbol = raw.lastIndexOf(symbol);
                    adjustedTokens.add(new CliTokenImpl(
                            token.isText(),
                            raw.substring(0, lastIndexOfSymbol),
                            value.substring(0, value.length() - symbol.length())
                    ));
                    adjustedTokens.add(new CliTokenImpl(true, raw.substring(lastIndexOfSymbol), symbol));
                    break;
                } else if (value.startsWith(symbol)) {
                    handled = true;
                    int firstIndexOfSymbol = raw.indexOf(symbol);
                    adjustedTokens.add(new CliTokenImpl(true,
                            raw.substring(0, firstIndexOfSymbol + symbol.length()), symbol));
                    adjustedTokens.add(new CliTokenImpl(
                            token.isText(),
                            raw.substring(firstIndexOfSymbol + symbol.length()),
                            value.substring(symbol.length())
                    ));
                    break;
                }
            }
            if (!handled) {
                adjustedTokens.add(token);
            }
        }
        return adjustedTokens;
    }

    private static void tokenize(String s, int index, List<CliToken> builder) {
        while (index < s.length()) {
            char c = s.charAt(index);
            switch (c) {
                case ' ':
                case '\t':
                    index = blankToken(s, index, builder);
                    break;
                default:
                    index = textToken(s, index, builder);
                    break;
            }
        }
    }

    // Todo use code points and not chars
    private static int textToken(String s, int index, List<CliToken> builder) {
        LineStatus quoter = new LineStatus();
        int from = index;
        StringBuilder value = new StringBuilder();
        while (index < s.length()) {
            char c = s.charAt(index);
            quoter.accept(c);
            if (!quoter.isQuoted() && !quoter.isEscaped() && isBlank(c)) {
                break;
            }
            if (quoter.isCodePoint()) {
                if (quoter.isEscaped() && quoter.isWeaklyQuoted() && c != '"') {
                    value.append('\\');
                }
                value.append(c);
            }
            index++;
        }
        builder.add(new CliTokenImpl(true, s.substring(from, index), value.toString()));
        return index;
    }

    private static int blankToken(String s, int index, List<CliToken> builder) {
        int from = index;
        while (index < s.length() && isBlank(s.charAt(index))) {
            index++;
        }
        builder.add(new CliTokenImpl(false, s.substring(from, index)));
        return index;
    }

    private static boolean isBlank(char c) {
        return c == ' ' || c == '\t';
    }
}
