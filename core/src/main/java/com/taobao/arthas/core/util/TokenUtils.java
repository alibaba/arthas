package com.taobao.arthas.core.util;

import java.util.List;

import com.taobao.arthas.core.shell.cli.CliToken;

/**
 * tokenizer helper
 *
 * @author gehui 2017-07-27 11:39:56
 */
public class TokenUtils {

    /**
     * find the first text token
     */
    public static CliToken findFirstTextToken(List<CliToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        CliToken first = null;
        for (CliToken token : tokens) {
            if (token != null && token.isText()) {
                first = token;
                break;
            }
        }
        return first;
    }

    /**
     * find the last text token
     */
    public static CliToken findLastTextToken(List<CliToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        //#165
        for (int i = tokens.size() - 1; i >= 0; i--) {
            CliToken token = tokens.get(i);
            if (token != null && token.isText()) {
                return token;
            }
        }
        return null;
    }

    /**
     * find the second text token's text
     */
    public static String findSecondTokenText(List<CliToken> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return null;
        }
        boolean first = true;
        for (CliToken token : tokens) {
            if (token != null && token.isText()) {
                if (first) {
                    first = false;
                } else {
                    return token.value();
                }
            }
        }
        return null;
    }

    public static CliToken getLast(List<CliToken> tokens){
        if (tokens == null || tokens.isEmpty()) {
            return null;
        } else {
            return tokens.get(tokens.size() -1);
        }
    }
}
