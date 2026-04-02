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
 * 属性源持有者接口，包含一个或多个 {@link PropertySource} 对象
 *
 * <p>此接口作为属性源（PropertySource）的容器，用于管理和访问多个属性源。
 * 继承自 Iterable 接口，支持迭代遍历所有包含的属性源。
 *
 * <p>使用场景：
 * <ul>
 * <li>当需要从多个来源（如配置文件、系统属性、环境变量等）访问属性时</li>
 * <li>当需要按照优先级顺序查找属性时</li>
 * <li>当需要统一管理多个配置源时</li>
 * </ul>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

    /**
     * 判断是否包含指定名称的属性源
     *
     * <p>此方法用于检查容器中是否存在具有特定名称的属性源。
     * 属性源的名称通常是唯一的，用于标识不同的配置来源。
     *
     * @param name 要查找的属性源名称（参考 {@link PropertySource#getName()}）
     * @return 如果包含指定名称的属性源则返回 true，否则返回 false
     */
    boolean contains(String name);

    /**
     * 获取指定名称的属性源
     *
     * <p>此方法根据名称从容器中获取对应的属性源对象。
     * 如果未找到匹配的属性源，则返回 null。
     *
     * @param name 要查找的属性源名称（参考 {@link PropertySource#getName()}）
     * @return 返回匹配的属性源对象，如果未找到则返回 null
     */
    PropertySource<?> get(String name);

}
