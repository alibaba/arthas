/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taobao.arthas.core.env;

import java.util.Arrays;

/**
 * 表示名称/值属性对源的抽象基类。
 * 底层的 {@linkplain #getSource() 源对象} 可以是封装属性的任何类型 {@code T}。
 * 示例包括 {@link java.util.Properties} 对象、{@link java.util.Map} 对象、
 * {@code ServletContext} 和 {@code ServletConfig} 对象（用于访问初始化参数）。
 * 探索 {@code PropertySource} 类型层次结构以查看提供的实现。
 *
 * <p>
 * {@code PropertySource} 对象通常不单独使用，而是通过 {@link PropertySources} 对象使用，
 * 该对象聚合属性源，并结合 {@link PropertyResolver} 实现，
 * 可以在 {@code PropertySources} 集合中执行基于优先级的搜索。
 *
 * <p>
 * {@code PropertySource} 的标识不是基于封装属性的内容，
 * 而是仅基于 {@code PropertySource} 的 {@link #getName() 名称}。
 * 这对于在集合上下文中操作 {@code PropertySource} 对象很有用。
 * 有关详细信息，请参阅 {@link MutablePropertySources} 中的操作以及
 * {@link #named(String)} 和 {@link #toString()} 方法。
 *
 * <p>
 * 注意，在使用 @{@link org.springframework.context.annotation.Configuration Configuration}
 * 类时，@{@link org.springframework.context.annotation.PropertySource PropertySource}
 * 注解提供了一种方便且声明性的方式将属性源添加到封闭的 {@code Environment} 中。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <T> 源类型
 * @see PropertySources
 * @see PropertyResolver
 * @see PropertySourcesPropertyResolver
 * @see MutablePropertySources
 * @see org.springframework.context.annotation.PropertySource
 */
public abstract class PropertySource<T> {

    // 属性源的名称，用于唯一标识该属性源
    protected final String name;

    // 属性源的底层对象，封装了实际的属性数据
    protected final T source;

    /**
     * 使用给定的名称和源对象创建一个新的 {@code PropertySource}。
     *
     * @param name 属性源的名称
     * @param source 属性源的底层对象
     */
    public PropertySource(String name, T source) {
        this.name = name;
        this.source = source;
    }

    /**
     * 使用给定的名称创建一个新的 {@code PropertySource}，
     * 并使用一个新的 {@code Object} 实例作为底层源。
     * <p>
     * 通常在测试场景中很有用，当创建从不查询实际源而是返回硬编码值的匿名实现时。
     *
     * @param name 属性源的名称
     */
    @SuppressWarnings("unchecked")
    public PropertySource(String name) {
        this(name, (T) new Object());
    }

    /**
     * 返回此 {@code PropertySource} 的名称。
     *
     * @return 属性源的名称
     */
    public String getName() {
        return this.name;
    }

    /**
     * 返回此 {@code PropertySource} 的底层源对象。
     *
     * @return 底层源对象
     */
    public T getSource() {
        return this.source;
    }

    /**
     * 返回此 {@code PropertySource} 是否包含给定名称的属性。
     * <p>
     * 此实现仅检查从 {@link #getProperty(String)} 返回的值是否为 {@code null}。
     * 如果可能，子类可能希望实现更高效的算法。
     *
     * @param name 要查找的属性名称
     * @return 如果包含该属性则返回 true，否则返回 false
     */
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * 返回与给定名称关联的值，如果未找到则返回 {@code null}。
     *
     * @param name 要查找的属性
     * @return 属性值，如果未找到则返回 null
     * @see PropertyResolver#getRequiredProperty(String)
     */
    public abstract Object getProperty(String name);

    /**
     * 此 {@code PropertySource} 对象在以下情况下等于给定对象：
     * <ul>
     * <li>它们是同一个实例</li>
     * <li>两个对象的 {@code name} 属性相等</li>
     * </ul>
     * <p>
     * 除了 {@code name} 之外，不评估其他属性。
     *
     * @param other 要比较的对象
     * @return 如果对象相等则返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object other) {
        return (this == other
                || (other instanceof PropertySource && nullSafeEquals(this.name, ((PropertySource<?>) other).name)));
    }

    /**
     * 返回从此 {@code PropertySource} 对象的 {@code name} 属性派生的哈希码。
     *
     * @return 基于名称的哈希码
     */
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    /**
     * 生成简洁的输出（类型和名称）。
     * 如果当前日志级别不包含调试信息，则生成简洁输出。
     * 如果启用调试，则生成详细输出，包括 PropertySource 实例的哈希码和每个名称/值属性对。
     * <p>
     * 这种可变的详细程度很有用，因为诸如系统属性或环境变量之类的属性源
     * 可能包含任意数量的属性对，可能导致难以阅读的异常和日志消息。
     *
     * @return 对象的字符串表示
     * @see Log#isDebugEnabled()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " {name='" + this.name + "'}";
    }

    /**
     * 返回仅用于集合比较目的的 {@code PropertySource} 实现。
     * <p>
     * 主要用于内部使用，但给定 {@code PropertySource} 对象的集合，
     * 可以按如下方式使用：
     *
     * <pre class="code">
     * {
     *     &#64;code
     *     List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
     *     sources.add(new MapPropertySource("sourceA", mapA));
     *     sources.add(new MapPropertySource("sourceB", mapB));
     *     assert sources.contains(PropertySource.named("sourceA"));
     *     assert sources.contains(PropertySource.named("sourceB"));
     *     assert !sources.contains(PropertySource.named("sourceC"));
     * }
     * </pre>
     *
     * 如果调用了除 {@code equals(Object)}、{@code hashCode()} 和 {@code toString()} 之外的任何方法，
     * 返回的 {@code PropertySource} 将抛出 {@code UnsupportedOperationException}。
     *
     * @param name 要创建和返回的比较 {@code PropertySource} 的名称
     * @return 用于比较的属性源
     */
    public static PropertySource<?> named(String name) {
        return new ComparisonPropertySource(name);
    }

    /**
     * 确定给定对象是否相等，如果两者都为 {@code null} 则返回 {@code true}，
     * 如果只有一个为 {@code null} 则返回 {@code false}。
     * <p>
     * 使用 {@code Arrays.equals} 比较数组，执行基于数组元素而不是数组引用的相等性检查。
     *
     * @param o1 要比较的第一个对象
     * @param o2 要比较的第二个对象
     * @return 给定对象是否相等
     * @see Object#equals(Object)
     * @see java.util.Arrays#equals
     */
    public static boolean nullSafeEquals(Object o1, Object o2) {
        // 如果引用相同，则相等
        if (o1 == o2) {
            return true;
        }
        // 如果只有一个为 null，则不相等
        if (o1 == null || o2 == null) {
            return false;
        }
        // 使用对象的 equals 方法比较
        if (o1.equals(o2)) {
            return true;
        }
        // 如果都是数组，使用数组比较方法
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            return arrayEquals(o1, o2);
        }
        return false;
    }

    /**
     * 使用 {@code Arrays.equals} 比较给定的数组，执行基于数组元素而不是数组引用的相等性检查。
     *
     * @param o1 要比较的第一个数组
     * @param o2 要比较的第二个数组
     * @return 给定对象是否相等
     * @see #nullSafeEquals(Object, Object)
     * @see java.util.Arrays#equals
     */
    private static boolean arrayEquals(Object o1, Object o2) {
        // 处理各种基本类型数组的比较
        if (o1 instanceof Object[] && o2 instanceof Object[]) {
            return Arrays.equals((Object[]) o1, (Object[]) o2);
        }
        if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
            return Arrays.equals((boolean[]) o1, (boolean[]) o2);
        }
        if (o1 instanceof byte[] && o2 instanceof byte[]) {
            return Arrays.equals((byte[]) o1, (byte[]) o2);
        }
        if (o1 instanceof char[] && o2 instanceof char[]) {
            return Arrays.equals((char[]) o1, (char[]) o2);
        }
        if (o1 instanceof double[] && o2 instanceof double[]) {
            return Arrays.equals((double[]) o1, (double[]) o2);
        }
        if (o1 instanceof float[] && o2 instanceof float[]) {
            return Arrays.equals((float[]) o1, (float[]) o2);
        }
        if (o1 instanceof int[] && o2 instanceof int[]) {
            return Arrays.equals((int[]) o1, (int[]) o2);
        }
        if (o1 instanceof long[] && o2 instanceof long[]) {
            return Arrays.equals((long[]) o1, (long[]) o2);
        }
        if (o1 instanceof short[] && o2 instanceof short[]) {
            return Arrays.equals((short[]) o1, (short[]) o2);
        }
        return false;
    }

    /**
     * 占位符属性源，用于在应用程序上下文创建时无法立即初始化实际属性源的情况。
     * 例如，基于 {@code ServletContext} 的属性源必须等待 {@code ServletContext} 对象
     * 对其封闭的 {@code ApplicationContext} 可用。在这种情况下，
     * 应使用存根来保存属性源的预期默认位置/顺序，然后在上下文刷新期间被替换。
     *
     * @see org.springframework.context.support.AbstractApplicationContext#initPropertySources()
     * @see org.springframework.web.context.support.StandardServletEnvironment
     * @see org.springframework.web.context.support.ServletContextPropertySource
     */
    public static class StubPropertySource extends PropertySource<Object> {

        /**
         * 创建一个新的存根属性源。
         *
         * @param name 属性源的名称
         */
        public StubPropertySource(String name) {
            super(name, new Object());
        }

        /**
         * 始终返回 {@code null}，因为这是一个占位符属性源。
         *
         * @param name 要查找的属性名称
         * @return 始终返回 null
         */
        @Override
        public String getProperty(String name) {
            return null;
        }
    }

    /**
     * 比较属性源，仅用于集合比较目的。
     *
     * @see PropertySource#named(String)
     */
    static class ComparisonPropertySource extends StubPropertySource {

        // 错误消息：此属性源仅用于集合比较
        private static final String USAGE_ERROR = "ComparisonPropertySource instances are for use with collection comparison only";

        /**
         * 创建一个新的比较属性源。
         *
         * @param name 属性源的名称
         */
        public ComparisonPropertySource(String name) {
            super(name);
        }

        /**
         * 抛出 UnsupportedOperationException，因为此属性源仅用于比较。
         *
         * @return 始终抛出异常
         * @throws UnsupportedOperationException 始终抛出
         */
        @Override
        public Object getSource() {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        /**
         * 抛出 UnsupportedOperationException，因为此属性源仅用于比较。
         *
         * @param name 属性名称
         * @return 始终抛出异常
         * @throws UnsupportedOperationException 始终抛出
         */
        @Override
        public boolean containsProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        /**
         * 抛出 UnsupportedOperationException，因为此属性源仅用于比较。
         *
         * @param name 属性名称
         * @return 始终抛出异常
         * @throws UnsupportedOperationException 始终抛出
         */
        @Override
        public String getProperty(String name) {
            throw new UnsupportedOperationException(USAGE_ERROR);
        }

        /**
         * 返回此比较属性源的字符串表示。
         *
         * @return 字符串表示
         */
        @Override
        public String toString() {
            return String.format("%s [name='%s']", getClass().getSimpleName(), this.name);
        }
    }
}
