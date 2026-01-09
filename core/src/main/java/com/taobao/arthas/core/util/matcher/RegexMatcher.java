package com.taobao.arthas.core.util.matcher;

import com.taobao.arthas.core.util.RegexCacheManager;
import java.util.regex.Pattern;

/**
 * regex matcher
 * @author ralf0131 2017-01-06 13:16.
 */
public class RegexMatcher implements Matcher<String> {

    private final Pattern pattern;

    public RegexMatcher(String pattern) {
        // 使用正则表达式缓存
        this.pattern = RegexCacheManager.getInstance().getPattern(pattern);
    }

    @Override
    public boolean matching(String target) {
        return null != target
                && null != pattern
                && pattern.matcher(target).matches();
    }
}