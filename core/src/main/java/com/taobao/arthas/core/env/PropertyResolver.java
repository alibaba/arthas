/*
 * Copyright 2002-2016 the original author or authors.
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
 * 用于从任何底层源解析属性的接口。
 * 提供了获取属性值、检查属性存在性以及解析占位符的方法。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see Environment
 * @see PropertySourcesPropertyResolver
 */
public interface PropertyResolver {

    /**
     * 返回给定的属性键是否可用于解析，即给定键的值是否不为 {@code null}。
     *
     * @param key 要检查的属性键
     * @return 如果属性存在且不为 null 则返回 true，否则返回 false
     */
    boolean containsProperty(String key);

    /**
     * 返回与给定键关联的属性值，如果键无法解析则返回 {@code null}。
     *
     * @param key 要解析的属性名称
     * @return 属性值，如果未找到则返回 null
     * @see #getProperty(String, String)
     * @see #getProperty(String, Class)
     * @see #getRequiredProperty(String)
     */
    String getProperty(String key);

    /**
     * 返回与给定键关联的属性值，如果键无法解析则返回 {@code defaultValue}。
     *
     * @param key 要解析的属性名称
     * @param defaultValue 如果未找到值则返回的默认值
     * @return 属性值或默认值
     * @see #getRequiredProperty(String)
     * @see #getProperty(String, Class)
     */
    String getProperty(String key, String defaultValue);

    /**
     * 返回与给定键关联的属性值，并将其转换为指定的目标类型。
     * 如果键无法解析则返回 {@code null}。
     *
     * @param key 要解析的属性名称
     * @param targetType 属性值的期望类型
     * @return 转换后的属性值，如果未找到则返回 null
     * @see #getRequiredProperty(String, Class)
     */
    <T> T getProperty(String key, Class<T> targetType);

    /**
     * 返回与给定键关联的属性值，并将其转换为指定的目标类型。
     * 如果键无法解析则返回 {@code defaultValue}。
     *
     * @param key 要解析的属性名称
     * @param targetType 属性值的期望类型
     * @param defaultValue 如果未找到值则返回的默认值
     * @return 转换后的属性值或默认值
     * @see #getRequiredProperty(String, Class)
     */
    <T> T getProperty(String key, Class<T> targetType, T defaultValue);

    /**
     * 返回与给定键关联的属性值（从不返回 {@code null}）。
     * 如果键无法解析，将抛出异常。
     *
     * @param key 要解析的属性名称
     * @return 属性值
     * @throws IllegalStateException 如果键无法解析
     * @see #getRequiredProperty(String, Class)
     */
    String getRequiredProperty(String key) throws IllegalStateException;

    /**
     * 返回与给定键关联的属性值，并将其转换为指定的目标类型（从不返回 {@code null}）。
     * 如果键无法解析，将抛出异常。
     *
     * @param key 要解析的属性名称
     * @param targetType 属性值的期望类型
     * @return 转换后的属性值
     * @throws IllegalStateException 如果给定键无法解析
     */
    <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

    /**
     * 解析给定文本中的 ${...} 占位符，将其替换为由 {@link #getProperty} 解析的相应属性值。
     * 无法解析且没有默认值的占位符将被忽略并保持不变。
     *
     * @param text 要解析的字符串
     * @return 解析后的字符串（从不返回 {@code null}）
     * @throws IllegalArgumentException 如果给定文本为 {@code null}
     * @see #resolveRequiredPlaceholders
     * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String)
     */
    String resolvePlaceholders(String text);

    /**
     * 解析给定文本中的 ${...} 占位符，将其替换为由 {@link #getProperty} 解析的相应属性值。
     * 无法解析且没有默认值的占位符将导致抛出 IllegalArgumentException。
     *
     * @param text 要解析的字符串
     * @return 解析后的字符串（从不返回 {@code null}）
     * @throws IllegalArgumentException 如果给定文本为 {@code null} 或有任何占位符无法解析
     * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String, boolean)
     */
    String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
