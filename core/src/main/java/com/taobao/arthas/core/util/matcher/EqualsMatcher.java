package com.taobao.arthas.core.util.matcher;

import com.taobao.arthas.core.util.ArthasCheckUtils;

/**
 * 全等匹配器
 * <p>
 * 该匹配器用于判断目标对象是否与指定的模式对象完全相等。
 * 使用{@link ArthasCheckUtils#isEquals(Object, Object)}方法进行相等性判断，
 * 该方法能够正确处理null值的情况。
 * </p>
 * <p>
 * 该匹配器通常用于需要精确匹配的场景，例如精确匹配类名、方法名等。
 * </p>
 *
 * @param <T> 待匹配的目标对象类型
 * @author ralf0131
 * @since 2017-01-06
 */
public class EqualsMatcher<T> implements Matcher<T> {

    /**
     * 用于匹配的模式对象
     * <p>
     * 该字段存储了匹配器要对比的目标值，
     * 只有当待匹配对象与该值完全相等时，才认为匹配成功。
     * 使用final修饰确保模式对象在构造后不可变。
     * </p>
     */
    private final T pattern;

    /**
     * 构造全等匹配器
     * <p>
     * 创建一个新的全等匹配器，指定用于匹配的模式对象。
     * </p>
     *
     * @param pattern 用于匹配的模式对象，目标对象必须与该对象完全相等
     */
    public EqualsMatcher(T pattern) {
        this.pattern = pattern;
    }

    /**
     * 判断目标对象是否与模式对象完全相等
     * <p>
     * 该方法通过调用{@link ArthasCheckUtils#isEquals(Object, Object)}
     * 来判断目标对象是否与模式对象相等。
     * </p>
     * <p>
     * 该方法能够正确处理以下情况：
     * <ul>
     *   <li>两个对象都为null，返回true</li>
     *   <li>一个对象为null，另一个不为null，返回false</li>
     *   <li>两个对象都不为null，使用equals方法比较</li>
     * </ul>
     * </p>
     *
     * @param target 待匹配的目标对象
     * @return 如果目标对象与模式对象完全相等则返回true，否则返回false
     */
    @Override
    public boolean matching(T target) {
        return ArthasCheckUtils.isEquals(target, pattern);
    }
}
