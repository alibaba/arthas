package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.cli.impl.CliTokenImpl;

import java.util.List;

/**
 * @author beiwei30 on 09/11/2016.
 */
public class CliTokens {
    /**
     * Create a text token.
     *
     * @param text the text
     * @return the token
     */
    public static CliToken createText(String text) {
        return new CliTokenImpl(true, text, text);
    }

    /**
     * Create a new blank token.
     *
     * @param blank the blank value
     * @return the token
     */
    public static CliToken createBlank(String blank) {
        return new CliTokenImpl(false, blank, blank);
    }

    /**
     * Tokenize the string argument and return a list of tokens.
     *
     * @param s the tokenized string
     * @return the tokens
     */
    public static List<CliToken> tokenize(String s) {
        return CliTokenImpl.tokenize(s);
    }
}
