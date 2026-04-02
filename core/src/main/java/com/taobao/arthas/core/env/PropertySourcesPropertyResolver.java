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
 * 基于属性源集合的属性解析器实现
 *
 * <p>此类实现了 {@link PropertyResolver} 接口，通过遍历底层的
 * {@link PropertySources} 集合来解析属性值。解析器会按照属性源的顺序
 * 依次查找，直到找到第一个匹配的属性值。
 *
 * <p>主要功能：
 * <ul>
 * <li>从多个属性源中查找属性值</li>
 * <li>支持属性值的类型转换</li>
 * <li>支持嵌套占位符的解析（如 ${user.home}）</li>
 * <li>支持必要属性的检查</li>
 * </ul>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

    /**
     * 属性源集合，用于存储和管理多个属性源
     *
     * <p>此字段持有多个属性源对象，解析器会按照迭代顺序依次查找属性。
     * 排在前面的属性源优先级更高，会覆盖后面属性源中的同名属性。
     */
    private final PropertySources propertySources;

    /**
     * 创建一个新的属性解析器实例
     *
     * <p>构造函数接收一个属性源集合作为参数，该集合将作为属性查找的来源。
     * 属性源的顺序很重要，排在前面的优先级更高。
     *
     * @param propertySources 要使用的属性源对象集合
     */
    public PropertySourcesPropertyResolver(PropertySources propertySources) {
        this.propertySources = propertySources;
    }

    /**
     * 检查是否包含指定名称的属性
     *
     * <p>此方法会遍历所有属性源，查找是否存在指定的属性键。
     * 只要任意一个属性源包含该属性，即返回 true。
     *
     * @param key 要查找的属性键
     * @return 如果找到指定属性则返回 true，否则返回 false
     */
    @Override
    public boolean containsProperty(String key) {
        // 检查属性源集合是否为空
        if (this.propertySources != null) {
            // 遍历所有属性源
            for (PropertySource<?> propertySource : this.propertySources) {
                // 如果当前属性源包含该属性键，返回 true
                if (propertySource.containsProperty(key)) {
                    return true;
                }
            }
        }
        // 未找到匹配的属性，返回 false
        return false;
    }

    /**
     * 获取指定属性的字符串值
     *
     * <p>此方法从属性源中查找指定键的值，并自动解析嵌套的占位符。
     * 如果未找到该属性，则返回 null。
     *
     * @param key 要获取的属性键
     * @return 返回属性的字符串值，如果未找到则返回 null
     */
    @Override
    public String getProperty(String key) {
        // 调用通用方法，目标类型为 String，并解析嵌套占位符
        return getProperty(key, String.class, true);
    }

    /**
     * 获取指定属性并转换为指定类型的值
     *
     * <p>此方法从属性源中查找指定键的值，并将其转换为目标类型。
     * 转换过程中会自动解析嵌套的占位符。
     *
     * @param <T> 目标值的泛型类型
     * @param key 要获取的属性键
     * @param targetValueType 目标值的类型（如 Integer.class, Boolean.class 等）
     * @return 返回转换后的属性值，如果未找到或转换失败则返回 null
     */
    @Override
    public <T> T getProperty(String key, Class<T> targetValueType) {
        // 调用通用方法，指定目标类型，并解析嵌套占位符
        return getProperty(key, targetValueType, true);
    }

    /**
     * 获取指定属性的原始字符串值（不解析嵌套占位符）
     *
     * <p>此方法用于获取属性的原始值，不会对值中的占位符进行解析。
     * 这在需要保留属性原始格式时很有用。
     *
     * @param key 要获取的属性键
     * @return 返回属性的原始字符串值，如果未找到则返回 null
     */
    @Override
    protected String getPropertyAsRawString(String key) {
        // 调用通用方法，目标类型为 String，但不解析嵌套占位符
        return getProperty(key, String.class, false);
    }

    /**
     * 注释掉的旧版本实现
     *
     * <p>这是早期的实现方式，使用了 convertValueIfNecessary 方法。
     * 新版本使用 conversionService 进行类型转换，提供了更好的类型转换支持。
     */
//	protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
//		if (this.propertySources != null) {
//			for (PropertySource<?> propertySource : this.propertySources) {
//				Object value = propertySource.getProperty(key);
//				if (value != null) {
//					if (resolveNestedPlaceholders && value instanceof String) {
//						value = resolveNestedPlaceholders((String) value);
//					}
//					logKeyFound(key, propertySource, value);
//					return convertValueIfNecessary(value, targetValueType);
//				}
//			}
//		}
//		return null;
//	}

    /**
     * 核心属性获取方法，支持类型转换和嵌套占位符解析
     *
     * <p>此方法实现了属性解析的核心逻辑：
     * <ol>
     * <li>遍历所有属性源，按顺序查找属性</li>
     * <li>找到属性后，如果需要则解析嵌套占位符</li>
     * <li>检查是否可以转换为目标类型</li>
     * <li>执行类型转换并返回结果</li>
     * </ol>
     *
     * @param <T> 目标值的泛型类型
     * @param key 要获取的属性键
     * @param targetValueType 目标值的类型
     * @param resolveNestedPlaceholders 是否解析嵌套占位符（如 ${another.property}）
     * @return 返回转换后的属性值，如果未找到则返回 null
     * @throws IllegalArgumentException 如果无法将属性值转换为目标类型
     */
    protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
        // 检查属性源集合是否为空
        if (this.propertySources != null) {
            // 遍历所有属性源
            for (PropertySource<?> propertySource : this.propertySources) {
                Object value;
                // 尝试从当前属性源获取属性值
                if ((value = propertySource.getProperty(key)) != null) {
                    // 获取值的实际类型
                    Class<?> valueType = value.getClass();
                    // 如果需要解析嵌套占位符且值是字符串类型
                    if (resolveNestedPlaceholders && value instanceof String) {
                        // 解析字符串中的嵌套占位符（如 ${user.home}）
                        value = resolveNestedPlaceholders((String) value);
                    }
                    // 检查转换服务是否能够执行类型转换
                    if (!this.conversionService.canConvert(valueType, targetValueType)) {
                        // 如果无法转换，抛出异常
                        throw new IllegalArgumentException(
                                String.format("Cannot convert value [%s] from source type [%s] to target type [%s]",
                                        value, valueType.getSimpleName(), targetValueType.getSimpleName()));
                    }
                    // 执行类型转换并返回结果
                    return this.conversionService.convert(value, targetValueType);
                }
            }
        }
        // 未找到匹配的属性，返回 null
        return null;
    }

    /**
     * 记录属性键找到的日志
     *
     * <p>此方法用于在找到属性时记录调试日志。
     * 注意：从 4.3.3 版本开始，为了避免意外记录敏感配置信息，
     * 默认实现不再记录属性的具体值。
     *
     * <p>子类可以重写此方法来自定义日志行为，
     * 例如更改日志级别或记录属性值（如果安全的话）。
     *
     * @param key 找到的属性键
     * @param propertySource 找到该属性的属性源对象
     * @param value 对应的属性值
     * @since 4.3.1
     */
    protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
        // 默认实现为空，不记录日志
        // 之前的实现被注释掉，以避免记录敏感信息
//		if (logger.isDebugEnabled()) {
//			logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
//					"' with value of type " + value.getClass().getSimpleName());
//		}
    }

}
