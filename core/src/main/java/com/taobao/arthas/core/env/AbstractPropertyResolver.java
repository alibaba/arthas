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
import java.util.LinkedHashSet;
import java.util.Set;

import com.taobao.arthas.core.env.convert.ConfigurableConversionService;
import com.taobao.arthas.core.env.convert.DefaultConversionService;

/**
 * 属性解析器的抽象基类，用于从任何底层源解析属性
 * <p>
 * 该类提供了属性解析的核心功能实现，包括：
 * <ul>
 * <li>属性值的类型转换</li>
 * <li>占位符的解析和处理（如 ${...}）</li>
 * <li>必要属性的验证</li>
 * <li>嵌套占位符的解析</li>
 * </ul>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

    /**
     * 可配置的转换服务，用于将属性值从一种类型转换为另一种类型
     * 默认使用 DefaultConversionService 实例
     */
    protected ConfigurableConversionService conversionService = new DefaultConversionService();

    /**
     * 非严格模式的占位符辅助处理器
     * 用于处理可以忽略无法解析占位符的场景
     */
    private PropertyPlaceholderHelper nonStrictHelper;

    /**
     * 严格模式的占位符辅助处理器
     * 用于遇到无法解析的占位符时抛出异常的场景
     */
    private PropertyPlaceholderHelper strictHelper;

    /**
     * 是否忽略无法解析的嵌套占位符
     * false 表示严格模式，遇到无法解析的嵌套占位符会抛出异常
     * true 表示非严格模式，无法解析的嵌套占位符会保留原样
     */
    private boolean ignoreUnresolvableNestedPlaceholders = false;

    /**
     * 占位符的前缀字符串
     * 默认值为 "${"
     */
    private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;

    /**
     * 占位符的后缀字符串
     * 默认值为 "}"
     */
    private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

    /**
     * 占位符默认值的分隔符
     * 默认值为 ":"
     * 例如：${property:defaultValue} 中的冒号就是分隔符
     */
    private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

    /**
     * 必须存在的属性名称集合
     * 使用 LinkedHashSet 保持插入顺序
     * 通过 validateRequiredProperties() 方法进行验证
     */
    private final Set<String> requiredProperties = new LinkedHashSet<String>();

    /**
     * 获取当前使用的类型转换服务
     *
     * @return ConfigurableConversionService 实例，用于属性值的类型转换
     */
    public ConfigurableConversionService getConversionService() {
        return this.conversionService;
    }

    /**
     * 设置类型转换服务
     * 允许自定义类型转换逻辑，替换默认的转换服务
     *
     * @param conversionService 要设置的可配置转换服务
     */
    public void setConversionService(ConfigurableConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * 设置占位符必须以指定的前缀开头
     * <p>
     * 默认值为 "${"
     *
     * @param placeholderPrefix 占位符的前缀字符串
     * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_PREFIX
     */
    @Override
    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    /**
     * 设置占位符必须以指定的后缀结尾
     * <p>
     * 默认值为 "}"
     *
     * @param placeholderSuffix 占位符的后缀字符串
     * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_SUFFIX
     */
    @Override
    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * 指定占位符与其关联的默认值之间的分隔符
     * <p>
     * 如果不需要处理默认值，可以设置为 {@code null}
     * <p>
     * 默认值为 ":"
     * 例如：${property:defaultValue} 中的冒号就是分隔符
     *
     * @param valueSeparator 值分隔符字符串，如果为 null 则不处理默认值
     * @see org.springframework.util.SystemPropertyUtils#VALUE_SEPARATOR
     */
    @Override
    public void setValueSeparator(String valueSeparator) {
        this.valueSeparator = valueSeparator;
    }

    /**
     * 设置在遇到无法解析的嵌套占位符时是否抛出异常
     * <p>
     * {@code false} 表示严格模式，会抛出异常
     * {@code true} 表示非严格模式，无法解析的嵌套占位符会以未解析的 ${...} 形式保留
     * <p>
     * 默认值为 {@code false}
     *
     * @param ignoreUnresolvableNestedPlaceholders 是否忽略无法解析的嵌套占位符
     * @since 3.2
     */
    @Override
    public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
        this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
    }

    /**
     * 设置必须存在的属性名称
     * <p>
     * 这些属性将在调用 {@link #validateRequiredProperties()} 时进行验证
     *
     * @param requiredProperties 必须存在的属性名称数组
     */
    @Override
    public void setRequiredProperties(String... requiredProperties) {
        this.requiredProperties.addAll(Arrays.asList(requiredProperties));
    }

    /**
     * 验证所有标记为必需的属性是否存在且解析为非 null 值
     * <p>
     * 如果有任何必需属性缺失，将抛出 MissingRequiredPropertiesException 异常
     *
     * @throws MissingRequiredPropertiesException 如果有任何必需属性无法解析
     */
    @Override
    public void validateRequiredProperties() {
        // 创建异常对象，用于收集所有缺失的必需属性
        MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
        // 遍历所有必需属性
        for (String key : this.requiredProperties) {
            // 检查属性是否存在
            if (this.getProperty(key) == null) {
                // 如果属性不存在，添加到异常对象中
                ex.addMissingRequiredProperty(key);
            }
        }
        // 如果有缺失的必需属性，抛出异常
        if (!ex.getMissingRequiredProperties().isEmpty()) {
            throw ex;
        }
    }

    /**
     * 检查是否包含指定的属性
     *
     * @param key 要检查的属性名称
     * @return 如果属性存在返回 true，否则返回 false
     */
    @Override
    public boolean containsProperty(String key) {
        return (getProperty(key) != null);
    }

    /**
     * 获取指定属性的字符串值
     *
     * @param key 属性名称
     * @return 属性值，如果不存在返回 null
     */
    @Override
    public String getProperty(String key) {
        return getProperty(key, String.class);
    }

    /**
     * 获取指定属性的字符串值，如果不存在则返回默认值
     *
     * @param key          属性名称
     * @param defaultValue 默认值
     * @return 属性值，如果不存在返回指定的默认值
     */
    @Override
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return (value != null ? value : defaultValue);
    }

    /**
     * 获取指定属性的值，并转换为目标类型，如果不存在则返回默认值
     *
     * @param key          属性名称
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @param <T>          目标类型的泛型参数
     * @return 属性值（转换为目标类型），如果不存在返回指定的默认值
     */
    @Override
    public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        T value = getProperty(key, targetType);
        return (value != null ? value : defaultValue);
    }

    /**
     * 获取必需的属性值（字符串类型）
     * <p>
     * 如果属性不存在，抛出 IllegalStateException 异常
     *
     * @param key 属性名称
     * @return 属性值
     * @throws IllegalStateException 如果属性不存在
     */
    @Override
    public String getRequiredProperty(String key) throws IllegalStateException {
        String value = getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Required key '" + key + "' not found");
        }
        return value;
    }

    /**
     * 获取必需的属性值，并转换为目标类型
     * <p>
     * 如果属性不存在，抛出 IllegalStateException 异常
     *
     * @param key       属性名称
     * @param valueType 目标类型
     * @param <T>       目标类型的泛型参数
     * @return 属性值（转换为目标类型）
     * @throws IllegalStateException 如果属性不存在
     */
    @Override
    public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
        T value = getProperty(key, valueType);
        if (value == null) {
            throw new IllegalStateException("Required key '" + key + "' not found");
        }
        return value;
    }

    /**
     * 解析文本中的占位符（非严格模式）
     * <p>
     * 无法解析的占位符将保留原样，不会抛出异常
     *
     * @param text 包含占位符的文本
     * @return 解析后的文本
     */
    @Override
    public String resolvePlaceholders(String text) {
        // 延迟初始化非严格模式的占位符辅助处理器
        if (this.nonStrictHelper == null) {
            this.nonStrictHelper = createPlaceholderHelper(true);
        }
        return doResolvePlaceholders(text, this.nonStrictHelper);
    }

    /**
     * 解析文本中的占位符（严格模式）
     * <p>
     * 如果有无法解析的占位符，将抛出 IllegalArgumentException 异常
     *
     * @param text 包含占位符的文本
     * @return 解析后的文本
     * @throws IllegalArgumentException 如果有无法解析的占位符
     */
    @Override
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        // 延迟初始化严格模式的占位符辅助处理器
        if (this.strictHelper == null) {
            this.strictHelper = createPlaceholderHelper(false);
        }
        return doResolvePlaceholders(text, this.strictHelper);
    }

    /**
     * 解析给定字符串中的嵌套占位符
     * <p>
     * 根据 {@link #setIgnoreUnresolvableNestedPlaceholders} 的值来决定
     * 无法解析的占位符是应该抛出异常还是被忽略
     * <p>
     * 该方法由 {@link #getProperty} 及其变体方法调用，隐式地解析嵌套占位符
     * 相比之下，{@link #resolvePlaceholders} 和 {@link #resolveRequiredPlaceholders}
     * 不会委托给此方法，而是根据各自方法的规范来处理无法解析的占位符
     *
     * @param value 包含可能嵌套占位符的字符串
     * @return 解析占位符后的字符串
     * @since 3.2
     * @see #setIgnoreUnresolvableNestedPlaceholders
     */
    protected String resolveNestedPlaceholders(String value) {
        // 根据设置决定使用严格模式还是非严格模式解析
        return (this.ignoreUnresolvableNestedPlaceholders ? resolvePlaceholders(value)
                : resolveRequiredPlaceholders(value));
    }

    /**
     * 创建占位符辅助处理器
     *
     * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符
     * @return 新创建的 PropertyPlaceholderHelper 实例
     */
    private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
        return new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix, this.valueSeparator,
                ignoreUnresolvablePlaceholders);
    }

    /**
     * 执行占位符解析的实际操作
     * <p>
     * 使用给定的辅助处理器替换文本中的占位符
     *
     * @param text   要解析的文本
     * @param helper 占位符辅助处理器
     * @return 解析后的文本
     */
    private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
        // 使用匿名内部类创建占位符解析器
        return helper.replacePlaceholders(text, new PropertyPlaceholderHelper.PlaceholderResolver() {
            /**
             * 解析单个占位符
             *
             * @param placeholderName 占位符名称
             * @return 解析后的属性值（原始字符串，不进行嵌套占位符解析）
             */
            public String resolvePlaceholder(String placeholderName) {
                return getPropertyAsRawString(placeholderName);
            }
        });
    }

    /**
     * 获取指定属性的原始字符串值，即不解析嵌套的占位符
     * <p>
     * 这是一个抽象方法，由子类实现具体的属性获取逻辑
     *
     * @param key 要解析的属性名称
     * @return 属性值，如果找不到返回 {@code null}
     */
    protected abstract String getPropertyAsRawString(String key);

}
