/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Map;

import org.apache.logging.log4j.util.PropertiesPropertySource;

/**
 * Map 属性源
 *
 * 从 {@code Map} 对象读取键值对的 {@link PropertySource} 实现。
 *
 * <p>
 * 这个类将 Map 对象封装为 PropertySource，使得 Map 中的键值对可以通过
 * 统一的属性源接口进行访问。Map 的键被视为属性名，值被视为属性值。
 *
 * <p>
 * 特点：
 * <ul>
 * <li>支持枚举所有属性名称（继承自 EnumerablePropertySource）</li>
 * <li>高效的属性查找（直接使用 Map 的 containsKey 方法）</li>
 * <li>线程安全性取决于底层的 Map 实现</li>
 * </ul>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertiesPropertySource
 * @see EnumerablePropertySource
 */
public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {

    /**
     * 构造一个新的 MapPropertySource
     *
     * @param name 属性源的名称，用于标识这个属性源
     * @param source 作为属性源的 Map 对象，键为属性名（String），值为属性值（Object）
     */
    public MapPropertySource(String name, Map<String, Object> source) {
        // 调用父类构造函数，设置属性源名称和源对象
        super(name, source);
    }

    /**
     * 从底层 Map 中获取指定名称的属性值
     *
     * @param name 要获取的属性名称
     * @return 属性值，如果不存在则返回 null（取决于 Map 的实现）
     */
    @Override
    public Object getProperty(String name) {
        // 直接从 Map 中获取值
        return this.source.get(name);
    }

    /**
     * 检查底层 Map 是否包含指定名称的属性
     *
     * <p>
     * 此实现直接使用 Map 的 containsKey 方法，效率很高。
     *
     * @param name 要检查的属性名称
     * @return 如果 Map 包含该键则返回 true，否则返回 false
     */
    @Override
    public boolean containsProperty(String name) {
        // 直接使用 Map 的 containsKey 方法检查
        return this.source.containsKey(name);
    }

    /**
     * 返回底层 Map 中所有属性的名称
     *
     * <p>
     * 此方法返回 Map 的所有键的字符串数组。
     * 数组的顺序取决于底层 Map 的实现（如 HashMap 不会保证顺序，
     * 而 LinkedHashMap 会保持插入顺序）。
     *
     * @return 包含所有属性名称的数组
     */
    @Override
    public String[] getPropertyNames() {
        // 将 Map 的键集转换为字符串数组
        return this.source.keySet().toArray(new String[0]);
    }

}
