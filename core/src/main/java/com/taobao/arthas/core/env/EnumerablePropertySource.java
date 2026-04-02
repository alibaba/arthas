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

/**
 * 可枚举属性源
 *
 * 一个能够查询其底层源对象以枚举所有可能属性名/值对的 {@link PropertySource} 实现。
 * 暴露 {@link #getPropertyNames()} 方法，允许调用者在不访问底层源对象的情况下检查可用的属性。
 * 这也促进了 {@link #containsProperty(String)} 的更高效实现，因为它可以调用
 * {@link #getPropertyNames()} 并遍历返回的数组，而不是尝试调用可能开销更大的
 * {@link #getProperty(String)}。实现可以考虑缓存 {@link #getPropertyNames()} 的结果，
 * 以充分利用这个性能优势。
 *
 * <p>
 * 大多数框架提供的 {@code PropertySource} 实现都是可枚举的；
 * 一个反例是 {@code JndiPropertySource}，由于 JNDI 的特性，
 * 在任何给定时间都无法确定所有可能的属性名；
 * 只能尝试访问属性（通过 {@link #getProperty(String)}）来评估它是否存在。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @param <T> 源对象类型
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

    /**
     * 构造一个带有名称和源对象的可枚举属性源
     *
     * @param name 属性源的名称
     * @param source 属性源对象
     */
    public EnumerablePropertySource(String name, T source) {
        super(name, source);
    }

    /**
     * 构造一个带有名称但没有源对象的可枚举属性源
     * 用于子类扩展，源对象可能稍后设置
     *
     * @param name 属性源的名称
     */
    protected EnumerablePropertySource(String name) {
        super(name);
    }

    /**
     * 检查此 {@code PropertySource} 是否包含具有给定名称的属性
     *
     * <p>
     * 此实现通过检查 {@link #getPropertyNames()} 数组中是否存在给定名称来判断。
     * 这种实现方式比直接调用 {@link #getProperty(String)} 更高效，因为后者可能
     * 涉及更复杂的查找逻辑。
     *
     * @param name 要查找的属性名称
     * @return 如果包含该属性则返回 true，否则返回 false
     */
    @Override
    public boolean containsProperty(String name) {
        // 获取所有属性名称
        String[] propertyNames = getPropertyNames();
        // 如果属性名称数组为空，直接返回 false
        if (propertyNames == null) {
            return false;
        }
        // 遍历所有属性名称，查找是否匹配
        for (String temp : propertyNames) {
            if (temp.equals(name)) {
                // 找到匹配的属性名称，返回 true
                return true;
            }
        }
        // 未找到匹配的属性名称，返回 false
        return false;

    }

    /**
     * 返回 {@linkplain #getSource() 源} 对象包含的所有属性的名称（永不为 {@code null}）
     *
     * <p>
     * 子类必须实现此方法以提供所有可用的属性名称。
     * 返回的数组不应包含 null 元素。
     * 如果源对象中没有属性，应返回空数组而不是 null。
     *
     * @return 所有属性的名称数组
     */
    public abstract String[] getPropertyNames();

}
