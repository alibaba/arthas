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

import com.taobao.arthas.core.env.convert.ConfigurableConversionService;

/**
 * 可配置的属性解析器接口
 * <p>
 * 这是一个配置接口，应该由大多数（如果不是全部）{@link PropertyResolver} 类型实现
 * 提供了访问和自定义 ConversionService 的功能，用于在属性值之间进行类型转换
 * <p>
 * 该接口扩展了 PropertyResolver 接口，增加了以下配置能力：
 * <ul>
 * <li>自定义类型转换服务（ConversionService）</li>
 * <li>配置占位符的前缀和后缀</li>
 * <li>配置占位符默认值的分隔符</li>
 * <li>控制嵌套占位符的解析行为</li>
 * <li>设置和验证必需的属性</li>
 * </ul>
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

    /**
     * 返回用于属性类型转换的 {@link ConfigurableConversionService}
     * <p>
     * 返回的转换服务是可配置的，允许方便地添加或删除单个 {@code Converter} 实例：
     *
     * <pre class="code">
     * ConfigurableConversionService cs = env.getConversionService();
     * cs.addConverter(new FooConverter());
     * </pre>
     *
     * @return 可配置的转换服务实例
     * @see PropertyResolver#getProperty(String, Class)
     * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
     */
    ConfigurableConversionService getConversionService();

    /**
     * 设置用于属性类型转换的 {@link ConfigurableConversionService}
     * <p>
     * <strong>注意：</strong>作为完全替换 {@code ConversionService} 的替代方案，
     * 可以考虑通过 {@link #getConversionService()} 获取转换服务实例，
     * 然后调用诸如 {@code #addConverter} 之类的方法来添加或删除单个转换器
     *
     * @param conversionService 要设置的转换服务
     * @see PropertyResolver#getProperty(String, Class)
     * @see #getConversionService()
     * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
     */
    void setConversionService(ConfigurableConversionService conversionService);

    /**
     * 设置占位符必须以指定的前缀开头
     * <p>
     * 例如：设置为 "${" 后，占位符格式为 ${propertyName}
     *
     * @param placeholderPrefix 占位符的前缀字符串
     */
    void setPlaceholderPrefix(String placeholderPrefix);

    /**
     * 设置占位符必须以指定的后缀结尾
     * <p>
     * 例如：设置为 "}" 后，占位符格式为 ${propertyName}
     *
     * @param placeholderSuffix 占位符的后缀字符串
     */
    void setPlaceholderSuffix(String placeholderSuffix);

    /**
     * 指定占位符与其关联的默认值之间的分隔符
     * <p>
     * 如果不需要处理默认值，可以设置为 {@code null}
     * <p>
     * 例如：设置为 ":" 后，格式为 ${property:defaultValue}
     *
     * @param valueSeparator 值分隔符字符串，如果为 null 则不处理默认值
     */
    void setValueSeparator(String valueSeparator);

    /**
     * 设置在遇到无法解析的嵌套占位符时是否抛出异常
     * <p>
     * {@code false} 表示严格模式，即会抛出异常
     * {@code true} 表示无法解析的嵌套占位符应以未解析的 ${...} 形式传递
     * <p>
     * {@link #getProperty(String)} 及其变体方法的实现必须检查此处设置的值，
     * 以确定当属性值包含无法解析的占位符时的正确行为
     *
     * @param ignoreUnresolvableNestedPlaceholders 是否忽略无法解析的嵌套占位符
     * @since 3.2
     */
    void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

    /**
     * 指定必须存在的属性
     * <p>
     * 这些属性将由 {@link #validateRequiredProperties()} 方法进行验证
     *
     * @param requiredProperties 必须存在的属性名称数组
     */
    void setRequiredProperties(String... requiredProperties);

    /**
     * 验证由 {@link #setRequiredProperties} 指定的每个属性是否存在且解析为非 {@code null} 值
     * <p>
     * 如果有任何必需属性无法解析，将抛出异常
     *
     * @throws MissingRequiredPropertiesException 如果有任何必需属性无法解析
     */
    void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
