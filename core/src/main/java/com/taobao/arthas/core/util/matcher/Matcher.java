package com.taobao.arthas.core.util.matcher;

/**
 * 匹配器接口
 * <p>
 * 定义了匹配器的通用行为，用于判断目标对象是否满足特定的匹配条件。
 * 这是一个泛型接口，可以用于匹配任何类型的对象。
 * </p>
 * <p>
 * 该接口被Arthas用于类匹配、方法匹配等各种场景，
 * 提供了灵活的匹配机制来筛选目标对象。
 * </p>
 *
 * @param <T> 待匹配的目标对象类型
 * @author vlinux
 * @since 2015-05-17
 */
public interface Matcher<T> {

    /**
     * 判断目标对象是否匹配
     * <p>
     * 该方法是匹配器的核心方法，用于判断给定的目标对象
     * 是否满足当前匹配器定义的匹配条件。
     * </p>
     *
     * @param target 待匹配的目标对象
     * @return 如果目标对象匹配则返回true，否则返回false
     */
    boolean matching(T target);

}
