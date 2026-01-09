package com.taobao.arthas.core.util.matcher;

import com.taobao.arthas.core.util.RegexCacheManager;
import java.util.regex.Pattern;

/**
 * regex matcher
 * @author ralf0131 2017-01-06 13:16.
 */
public class RegexMatcher implements Matcher<String> {

    private final String pattern;
    private Pattern compiledPattern;

    public RegexMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matching(String target) {
        if (null == target || null == pattern) {
            return false;
        }

        // 在第一次matching时才编译正则表达式
        if (compiledPattern == null) {
            compiledPattern = RegexCacheManager.getInstance().getPattern(pattern);
        }
        
        return compiledPattern != null && compiledPattern.matcher(target).matches();
    }
}