package com.taobao.arthas.core.util.matcher;

/**
 * 全匹配匹配器
 *
 * 一个总是返回 true 的匹配器实现。无论传入什么目标对象，
 * 匹配操作都会成功。这在需要匹配所有对象的场景中非常有用。
 *
 * @param <T> 要匹配的目标对象类型
 * @author ralf0131 2017-01-06 13:48.
 */
public final class TrueMatcher<T> implements Matcher<T> {

    /**
     * 匹配目标对象
     *
     * 此方法总是返回 true，表示任何目标对象都匹配成功。
     * 这使得该匹配器可以用作默认匹配器或在需要匹配所有对象的场景中使用。
     *
     * @param target 要匹配的目标对象（参数会被忽略）
     * @return 始终返回 true，表示匹配成功
     */
    @Override
    public boolean matching(T target) {
        // 无论传入什么对象，都返回 true，表示匹配成功
        return true;
    }

}