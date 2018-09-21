package com.taobao.arthas.core.util.matcher;

/**
 * regex matcher
 * @author ralf0131 2017-01-06 13:16.
 */
public class RegexMatcher implements Matcher<String> {

    private final String pattern;

    public RegexMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matching(String target) {
        return null != target
                && null != pattern
                && target.matches(pattern);
    }
}