/*
 * Copyright 2002-2015 the original author or authors.
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

/**
 * {@link MapPropertySource} 的专门化实现，设计用于与
 * {@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}
 * 一起使用。该类弥补了 Bash 和其他 shell 的限制，这些 shell 不允许变量名中
 * 包含点号(.)和/或连字符(-)；同时也允许属性名使用大写变体，以符合 shell 的使用习惯。
 *
 * <p>
 * 例如，调用 {@code getProperty("foo.bar")} 将尝试查找原始属性或任何"等效"属性的值，
 * 返回第一个找到的值：
 * <ul>
 * <li>{@code foo.bar} - 原始名称</li>
 * <li>{@code foo_bar} - 用下划线替换点号（如果有）</li>
 * <li>{@code FOO.BAR} - 原始名称，但使用大写</li>
 * <li>{@code FOO_BAR} - 使用下划线和大写</li>
 * </ul>
 * 上述任何连字符变体也可以工作，甚至是点号和连字符混合的变体。
 *
 * <p>
 * 同样的逻辑也适用于 {@link #containsProperty(String)} 的调用，如果上述任何属性存在，
 * 则返回 {@code true}，否则返回 {@code false}。
 *
 * <p>
 * 当将活动或默认配置文件指定为环境变量时，此特性特别有用。以下语法在 Bash 中是不允许的：
 *
 * <pre class="code">
 * spring.profiles.active=p1 java -classpath ... MyApp
 * </pre>
 *
 * 然而，以下语法是允许的，并且也更符合惯例：
 *
 * <pre class="code">
 * SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp
 * </pre>
 *
 * <p>
 * 为此类（或包）启用 debug 或 trace 级别的日志记录，可以查看解释何时发生这些"属性名称解析"的消息。
 *
 * <p>
 * 此属性源默认包含在 {@link StandardEnvironment} 及其所有子类中。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see StandardEnvironment
 * @see AbstractEnvironment#getSystemEnvironment()
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {

    /**
     * 创建一个新的 {@code SystemEnvironmentPropertySource} 实例
     *
     * @param name 属性源的名称
     * @param source 属性源的数据映射表，包含系统环境变量
     */
    public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    /**
     * 检查此属性源中是否存在给定名称或任何下划线/大写变体的属性
     *
     * @param name 要检查的属性名称
     * @return 如果属性存在返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * 获取属性值，通过解析属性名称来支持下划线/大写变体
     * 如果给定名称或其任何下划线/大写变体的属性存在于此属性源中，则返回对应的值
     *
     * @param name 要获取的属性名称
     * @return 属性值，如果不存在则返回 {@code null}
     */
    @Override
    public Object getProperty(String name) {
        // 解析属性名称，转换为实际在环境变量中存在的名称
        String actualName = resolvePropertyName(name);
        return super.getProperty(actualName);
    }

    /**
     * 检查此属性源是否包含给定名称或任何下划线/大写变体的属性
     * 如果找到匹配的属性，返回解析后的名称，否则返回原始名称
     * 该方法永远不会返回 {@code null}
     *
     * @param name 要解析的属性名称
     * @return 解析后的属性名称，如果找不到匹配项则返回原始名称
     */
    protected final String resolvePropertyName(String name) {
        // 首先检查原始名称
        String resolvedName = checkPropertyName(name);
        if (resolvedName != null) {
            return resolvedName;
        }
        // 如果原始名称未找到，尝试转大写后的名称
        String uppercasedName = name.toUpperCase();
        if (!name.equals(uppercasedName)) {
            resolvedName = checkPropertyName(uppercasedName);
            if (resolvedName != null) {
                return resolvedName;
            }
        }
        // 如果都找不到，返回原始名称
        return name;
    }

    /**
     * 检查属性名称的各种变体是否存在于属性源中
     * 按照以下顺序检查：
     * 1. 原始名称
     * 2. 将点号(.)替换为下划线(_)的名称
     * 3. 将连字符(-)替换为下划线(_)的名称
     * 4. 将点号和连字符都替换为下划线的名称
     *
     * @param name 要检查的属性名称
     * @return 如果找到匹配项返回找到的名称，否则返回 {@code null}
     */
    private String checkPropertyName(String name) {
        // 检查原始名称
        if (containsKey(name)) {
            return name;
        }
        // 检查仅替换点号的名称
        String noDotName = name.replace('.', '_');
        if (!name.equals(noDotName) && containsKey(noDotName)) {
            return noDotName;
        }
        // 检查仅替换连字符的名称
        String noHyphenName = name.replace('-', '_');
        if (!name.equals(noHyphenName) && containsKey(noHyphenName)) {
            return noHyphenName;
        }
        // 检查同时替换点号和连字符的名称
        String noDotNoHyphenName = noDotName.replace('-', '_');
        if (!noDotName.equals(noDotNoHyphenName) && containsKey(noDotNoHyphenName)) {
            return noDotNoHyphenName;
        }
        // 未找到任何匹配项
        return null;
    }

    /**
     * 检查属性源中是否包含指定的键
     * 如果存在安全管理器，使用 keySet().contains() 方法以避免权限问题
     * 否则使用 containsKey() 方法
     *
     * @param name 要检查的键名
     * @return 如果包含该键返回 {@code true}，否则返回 {@code false}
     */
    private boolean containsKey(String name) {
        return (isSecurityManagerPresent() ? this.source.keySet().contains(name) : this.source.containsKey(name));
    }

    /**
     * 检查是否存在安全管理器
     *
     * @return 如果存在安全管理器返回 {@code true}，否则返回 {@code false}
     */
    protected boolean isSecurityManagerPresent() {
        return (System.getSecurityManager() != null);
    }

}
