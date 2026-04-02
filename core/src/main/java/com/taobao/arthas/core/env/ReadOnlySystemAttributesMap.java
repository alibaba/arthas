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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 只读系统属性 Map 实现
 *
 * <p>这是一个只读的 {@code Map<String, String>} 实现，底层由系统属性
 * 或环境变量支持。此类设计为抽象类，子类需要实现具体的系统属性获取逻辑。
 *
 * <p>使用场景：
 * 当 {@link SecurityManager} 禁止访问 {@link System#getProperties()} 或
 * {@link System#getenv()} 时，此类提供了安全的访问方式。由于安全限制，
 * {@link #keySet()}、{@link #entrySet()} 和 {@link #values()} 方法总是返回
 * 空集合，即使 {@link #get(Object)} 方法可能返回非 null 值（如果当前
 * 安全管理器允许访问单个键）。
 *
 * <p>设计考虑：
 * <ul>
 * <li>只读操作：不支持修改操作（put、remove、clear 等）</li>
 * <li>安全性：在受限的安全环境下也能正常工作</li>
 * <li>惰性访问：只在真正需要时才访问系统属性</li>
 * <li>避免枚举：返回空的集合视图，避免暴露敏感信息</li>
 * </ul>
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.0
 */
abstract class ReadOnlySystemAttributesMap implements Map<String, String> {

    /**
     * 检查是否包含指定的键
     *
     * <p>此方法通过调用 get 方法来判断键是否存在。
     * 如果 get 方法返回非 null 值，则认为该键存在。
     *
     * <p>注意：由于安全管理器的限制，即使某个属性存在，
     * 此方法也可能返回 false（如果无法访问该属性）。
     *
     * @param key 要检查的键
     * @return 如果映射包含指定键的映射关系则返回 true，否则返回 false
     */
    @Override
    public boolean containsKey(Object key) {
        // 通过 get 方法是否返回非 null 来判断键是否存在
        return (get(key) != null);
    }

    /**
     * 返回指定键映射到的值，如果此映射不包含该键的映射关系则返回 null
     *
     * <p>此方法会验证键的类型，必须是 String 类型，然后调用
     * 抽象方法 getSystemAttribute 来获取实际的系统属性值。
     *
     * <p>类型安全：此方法强制要求键必须是 String 类型，
     * 这是因为系统属性和环境变量都使用字符串作为键。
     *
     * @param key 要检索的系统属性名称
     * @return 返回映射到的值，如果未找到则返回 null
     * @throws IllegalArgumentException 如果给定的键不是 String 类型
     */
    @Override
    public String get(Object key) {
        // 检查键是否为 String 类型
        if (!(key instanceof String)) {
            throw new IllegalArgumentException(
                    "Type of key [" + key.getClass().getName() + "] must be java.lang.String");
        }
        // 调用抽象方法获取系统属性
        return getSystemAttribute((String) key);
    }

    /**
     * 检查此映射是否为空
     *
     * <p>此方法始终返回 false，表示映射不为空。
     *
     * <p>设计原因：由于安全限制，我们无法枚举所有的系统属性，
     * 但可以安全地假设系统中有至少一些属性存在。因此总是返回 false，
     * 而不是尝试确定实际的属性数量。
     *
     * @return 始终返回 false
     */
    @Override
    public boolean isEmpty() {
        // 总是返回 false，假设系统中总有属性存在
        return false;
    }

    /**
     * 模板方法，返回底层的系统属性
     *
     * <p>这是一个抽象方法，子类必须实现此方法来提供具体的系统属性获取逻辑。
     * 典型的实现会调用：
     * <ul>
     * <li>{@link System#getProperty(String)} - 获取系统属性</li>
     * <li>{@link System#getenv(String)} - 获取环境变量</li>
     * </ul>
     *
     * <p>实现注意事项：
     * <ul>
     * <li>此方法可能返回 null（如果属性不存在）</li>
     * <li>此方法可能抛出 SecurityException（如果安全管理器禁止访问）</li>
     * <li>此方法应该处理各种异常情况</li>
     * </ul>
     *
     * @param attributeName 要获取的系统属性名称
     * @return 返回系统属性的值，如果不存在则返回 null
     */
    protected abstract String getSystemAttribute(String attributeName);

    // ===== 以下是不支持的操作 =====

    /**
     * 获取映射的大小（不支持）
     *
     * <p>由于安全限制和性能考虑，此操作不被支持。
     * 枚举所有系统属性可能会暴露敏感信息，并且代价高昂。
     *
     * @throws UnsupportedOperationException 始终抛出此异常
     */
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    /**
     * 添加映射关系（不支持）
     *
     * <p>系统属性是只读的，不允许通过此接口修改。
     * 如需修改系统属性，应使用 System.setProperty() 方法。
     *
     * @throws UnsupportedOperationException 始终抛出此异常
     */
    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException();
    }

    /**
     * 检查是否包含指定值（不支持）
     *
     * <p>由于安全限制，无法枚举所有值进行检查。
     *
     * @throws UnsupportedOperationException 始终抛出此异常
     */
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * 移除映射关系（不支持）
     *
     * <p>系统属性是只读的，不允许通过此接口删除。
     *
     * @throws UnsupportedOperationException 始终抛出此异常
     */
    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * 清空映射（不支持）
     *
     * <p>系统属性是只读的，不允许通过此接口清空。
     *
     * @throws UnsupportedOperationException 始终抛出此异常
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取键集合视图
     *
     * <p>由于安全限制，返回空集合以避免暴露敏感信息。
     * 即使实际上可能存在可访问的属性。
     *
     * @return 返回空的不可修改集合
     */
    @Override
    public Set<String> keySet() {
        return Collections.emptySet();
    }

    /**
     * 批量添加映射关系（不支持）
     *
     * <p>系统属性是只读的，不允许通过此接口修改。
     *
     * @throws UnsupportedOperationException 始终抛出此异常
     */
    @Override
    public void putAll(Map<? extends String, ? extends String> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取值集合视图
     *
     * <p>由于安全限制，返回空集合以避免暴露敏感信息。
     *
     * @return 返回空的不可修改集合
     */
    @Override
    public Collection<String> values() {
        return Collections.emptySet();
    }

    /**
     * 获取映射关系集合视图
     *
     * <p>由于安全限制，返回空集合以避免暴露敏感信息。
     *
     * @return 返回空的不可修改集合
     */
    @Override
    public Set<Entry<String, String>> entrySet() {
        return Collections.emptySet();
    }

}
