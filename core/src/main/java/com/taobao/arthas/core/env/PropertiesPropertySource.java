/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.Properties;

/**
 * 基于 Properties 对象的属性源实现
 *
 * <p>此类是 {@link PropertySource} 接口的实现，从 {@link java.util.Properties}
 * 对象中提取属性。它继承自 {@link MapPropertySource}，将 Properties 对象
 * 作为 Map 进行处理。
 *
 * <p>重要说明：
 * 由于 {@code Properties} 对象在技术上是 {@code <Object, Object>} 类型的
 * {@link java.util.Hashtable Hashtable}，可能包含非 {@code String} 类型的
 * 键或值。但此实现限制只能访问基于 {@code String} 的键和值，使用方式与
 * {@link Properties#getProperty} 和 {@link Properties#setProperty} 相同。
 *
 * <p>使用场景：
 * <ul>
 * <li>从 .properties 文件加载的配置</li>
 * <li>系统属性</li>
 * <li>任何以 Properties 对象形式存储的配置</li>
 * </ul>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class PropertiesPropertySource extends MapPropertySource {

    /**
     * 创建一个基于 Properties 对象的属性源
     *
     * <p>此构造函数接收一个 Properties 对象作为数据源，并将其作为
     * Map 传递给父类。Properties 对象会被强制转换为 Map 类型，
     * 因为 Properties 继承自 Hashtable<Object,Object>，实现了 Map 接口。
     *
     * <p>注意：使用 @SuppressWarnings 注解来抑制未检查的类型转换警告，
     * 这里的转换是安全的，因为 Properties 内部只使用 String 类型的键和值。
     *
     * @param name 属性源的名称，用于标识这个配置源
     * @param source Properties 对象，作为属性的来源
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public PropertiesPropertySource(String name, Properties source) {
        // 将 Properties 对象强制转换为 Map 并传递给父类
        // 这里使用 raw type 和 unchecked cast 是因为 Properties 的泛型定义
        super(name, (Map) source);
    }

    /**
     * 创建一个基于 Map 的属性源（受保护的构造函数）
     *
     * <p>此构造函数用于子类或内部使用，允许直接使用 Map<String, Object>
     * 作为数据源。这提供了更大的灵活性，可以使用任何实现了 Map 接口的
     * 对象作为属性源。
     *
     * @param name 属性源的名称，用于标识这个配置源
     * @param source Map 对象，作为属性的来源，键和值分别为 String 和 Object 类型
     */
    protected PropertiesPropertySource(String name, Map<String, Object> source) {
        // 直接传递 Map 对象给父类构造函数
        super(name, source);
    }

    /**
     * 获取所有属性名称
     *
     * <p>此方法返回当前属性源中所有属性的名称数组。
     *
     * <p>重要：此方法使用了同步机制。因为 Properties 对象继承自 Hashtable，
     * 它是线程安全的，但在迭代时仍需要额外的同步保护。这里使用 synchronized
     * 块确保在获取属性名称列表时，其他线程不会修改底层的 Properties 对象。
     *
     * @return 包含所有属性名称的字符串数组，如果没有属性则返回空数组
     */
    @Override
    public String[] getPropertyNames() {
        // 对数据源进行同步，确保线程安全
        synchronized (this.source) {
            // 调用父类方法获取属性名称数组
            return super.getPropertyNames();
        }
    }

}
