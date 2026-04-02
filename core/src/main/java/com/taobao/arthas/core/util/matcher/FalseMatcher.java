package com.taobao.arthas.core.util.matcher;

/**
 * 永假匹配器
 * <p>
 * 该匹配器对任何目标对象都返回false，即永远不匹配。
 * 这是一个特殊的匹配器实现，通常用作默认值或占位符。
 * </p>
 * <p>
 * 使用场景包括：
 * <ul>
 *   <li>作为某些配置的默认匹配器，表示默认不匹配任何对象</li>
 *   <li>在组合匹配器中表示"不匹配"的语义</li>
 *   <li>作为空对象模式的实现，避免null检查</li>
 * </ul>
 * </p>
 *
 * @param <T> 待匹配的目标对象类型
 * @author ralf0131
 * @since 2017-01-06
 */
public class FalseMatcher<T> implements Matcher<T> {

    /**
     * 判断目标对象是否匹配
     * <p>
     * 该方法总是返回false，无论传入的目标对象是什么。
     * </p>
     * <p>
     * 注意：该方法不检查目标对象是否为null，也不使用目标对象的任何属性，
     * 因此即使传入null也会返回false。
     * </p>
     *
     * @param target 待匹配的目标对象（该参数会被忽略）
     * @return 总是返回false，表示永远不匹配
     */
    @Override
    public boolean matching(T target) {
        return false;
    }
}
