package com.taobao.arthas.core.util.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author ralf0131 2017-01-06 13:29.
 */
public interface GroupMatcher<T> extends Matcher<T> {

    /**
     * 追加匹配器
     *
     * @param matcher 匹配器
     */
    void add(Matcher<T> matcher);

    /**
     * 与关系组匹配
     *
     * @param <T> 匹配类型
     */
    class And<T> implements GroupMatcher<T> {

        private final Collection<Matcher<T>> matchers;

        /**
         * 与关系组匹配构造<br/>
         * 当且仅当目标符合匹配组的所有条件时才判定匹配成功
         *
         * @param matchers 待进行与关系组匹配的匹配集合
         */
        public And(Matcher<T>... matchers) {
            this.matchers = Arrays.asList(matchers);
        }

        @Override
        public boolean matching(T target) {
            for (Matcher<T> matcher : matchers) {
                if (!matcher.matching(target)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void add(Matcher<T> matcher) {
            matchers.add(matcher);
        }
    }

    /**
     * 或关系组匹配
     *
     * @param <T> 匹配类型
     */
    class Or<T> implements GroupMatcher<T> {

        private final Collection<Matcher<T>> matchers;

        public Or() {
            this.matchers = new ArrayList<Matcher<T>>();
        }

        /**
         * 或关系组匹配构造<br/>
         * 当且仅当目标符合匹配组的任一条件时就判定匹配成功
         *
         * @param matchers 待进行或关系组匹配的匹配集合
         */
        public Or(Matcher<T>... matchers) {
            this.matchers = Arrays.asList(matchers);
        }

        public Or(Collection<Matcher<T>> matchers) {
            this.matchers = matchers;
        }

        @Override
        public boolean matching(T target) {
            for (Matcher<T> matcher : matchers) {
                if (matcher.matching(target)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void add(Matcher<T> matcher) {
            matchers.add(matcher);
        }
    }

}
