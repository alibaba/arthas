package com.taobao.arthas.core.util;

import java.util.List;

import com.taobao.arthas.core.shell.cli.CliToken;

/**
 * tokens处理的辅助类
 *  
 * @author gehui 2017年7月27日 上午11:39:56
 */
public class TokenUtils {

    public static CliToken findFirstTextToken(List<CliToken> tokens) {
        CliToken first = null;
        for (CliToken token : tokens) {
            if (token.isText()) {
                first = token;
                break;
            }
        }
        return first;
    }

    public static CliToken findLastTextToken(List<CliToken> tokens) {
        for (int i = tokens.size() - 1; i > 0; i--) {
            CliToken token = tokens.get(i);
            if (token.isText()) {
                return token;
            }
        }
        return null;
    }

    public static String findSecondTokenText(List<CliToken> tokens) {
        boolean first = true;
        for (CliToken token : tokens) {
            if (token.isText()) {
                if (first) {
                    first = false;
                } else {
                    return token.value();
                }
            }
        }
        return null;
    }
}
