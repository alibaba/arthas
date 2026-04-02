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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * 用于处理包含占位符的字符串的工具类。
 * 占位符采用 {@code ${name}} 的形式。使用 {@code PropertyPlaceholderHelper}
 * 可以将这些占位符替换为用户提供的值。
 * <p>
 * 替换值可以通过 {@link Properties} 实例或 {@link PlaceholderResolver} 提供。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 3.0
 */
public class PropertyPlaceholderHelper {

    // 已知简单前缀映射表，用于优化嵌套占位符的处理
    private static final Map<String, String> wellKnownSimplePrefixes = new HashMap<String, String>(4);

    static {
        // 配置常见的占位符后缀对应的前缀映射
        wellKnownSimplePrefixes.put("}", "{");
        wellKnownSimplePrefixes.put("]", "[");
        wellKnownSimplePrefixes.put(")", "(");
    }

    // 占位符前缀，例如 "${"
    private final String placeholderPrefix;

    // 占位符后缀，例如 "}"
    private final String placeholderSuffix;

    // 简单前缀，用于优化嵌套占位符的检测
    private final String simplePrefix;

    // 值分隔符，用于分隔占位符名称和默认值，例如 ":"
    private final String valueSeparator;

    // 是否忽略无法解析的占位符
    private final boolean ignoreUnresolvablePlaceholders;

    /**
     * 创建一个新的 {@code PropertyPlaceholderHelper}，使用指定的前缀和后缀。
     * 无法解析的占位符将被忽略。
     *
     * @param placeholderPrefix 表示占位符开始的前缀
     * @param placeholderSuffix 表示占位符结束的后缀
     */
    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix) {
        this(placeholderPrefix, placeholderSuffix, null, true);
    }

    /**
     * 创建一个新的 {@code PropertyPlaceholderHelper}，使用指定的前缀和后缀。
     *
     * @param placeholderPrefix 表示占位符开始的前缀
     * @param placeholderSuffix 表示占位符结束的后缀
     * @param valueSeparator 占位符变量与关联默认值之间的分隔字符（如果有）
     * @param ignoreUnresolvablePlaceholders 指示是否应忽略无法解析的占位符
     *                                      （{@code true}）或抛出异常（{@code false}）
     */
    public PropertyPlaceholderHelper(String placeholderPrefix, String placeholderSuffix, String valueSeparator,
            boolean ignoreUnresolvablePlaceholders) {

        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
        // 根据后缀查找对应的简单前缀，用于优化嵌套占位符检测
        String simplePrefixForSuffix = wellKnownSimplePrefixes.get(this.placeholderSuffix);
        if (simplePrefixForSuffix != null && this.placeholderPrefix.endsWith(simplePrefixForSuffix)) {
            this.simplePrefix = simplePrefixForSuffix;
        } else {
            this.simplePrefix = this.placeholderPrefix;
        }
        this.valueSeparator = valueSeparator;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    /**
     * 将所有格式为 {@code ${name}} 的占位符替换为给定的 {@link Properties} 中的对应属性值。
     *
     * @param value 包含要替换的占位符的值
     * @param properties 用于替换的 {@code Properties}
     * @return 替换了占位符的值
     */
    public String replacePlaceholders(String value, final Properties properties) {
        return replacePlaceholders(value, new PlaceholderResolver() {
            public String resolvePlaceholder(String placeholderName) {
                return properties.getProperty(placeholderName);
            }
        });
    }

    /**
     * 将所有格式为 {@code ${name}} 的占位符替换为从给定的 {@link PlaceholderResolver} 返回的值。
     *
     * @param value 包含要替换的占位符的值
     * @param placeholderResolver 用于替换的 {@code PlaceholderResolver}
     * @return 替换了占位符的值
     */
    public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
        return parseStringValue(value, placeholderResolver, null);
    }

    /**
     * 解析字符串值，替换其中的所有占位符。
     * 支持递归解析和嵌套占位符，并能检测循环引用。
     *
     * @param value 要解析的字符串值
     * @param placeholderResolver 用于解析占位符的解析器
     * @param visitedPlaceholders 已访问的占位符集合，用于检测循环引用
     * @return 解析后的字符串值
     * @throws IllegalArgumentException 如果检测到循环引用或无法解析占位符（且不配置忽略）
     */
    protected String parseStringValue(String value, PlaceholderResolver placeholderResolver,
            Set<String> visitedPlaceholders) {

        // 查找第一个占位符的起始位置
        int startIndex = value.indexOf(this.placeholderPrefix);
        if (startIndex == -1) {
            return value;
        }

        StringBuilder result = new StringBuilder(value);
        while (startIndex != -1) {
            // 查找占位符的结束位置
            int endIndex = findPlaceholderEndIndex(result, startIndex);
            if (endIndex != -1) {
                // 提取占位符名称（不包含前缀和后缀）
                String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                String originalPlaceholder = placeholder;
                if (visitedPlaceholders == null) {
                    visitedPlaceholders = new HashSet<String>(4);
                }
                // 检测循环引用：如果占位符已在访问集合中，说明存在循环
                if (!visitedPlaceholders.add(originalPlaceholder)) {
                    throw new IllegalArgumentException(
                            "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
                }
                // 递归调用，解析占位符键本身中包含的占位符
                placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
                // 现在获取完全解析后的键的值
                String propVal = placeholderResolver.resolvePlaceholder(placeholder);
                // 如果值为空且配置了值分隔符，尝试提取默认值
                if (propVal == null && this.valueSeparator != null) {
                    int separatorIndex = placeholder.indexOf(this.valueSeparator);
                    if (separatorIndex != -1) {
                        String actualPlaceholder = placeholder.substring(0, separatorIndex);
                        String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
                        propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
                        if (propVal == null) {
                            propVal = defaultValue;
                        }
                    }
                }
                if (propVal != null) {
                    // 递归调用，解析之前解析的占位符值中包含的占位符
                    propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
                    // 替换占位符为解析后的值
                    result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
                    // 继续查找下一个占位符
                    startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                } else if (this.ignoreUnresolvablePlaceholders) {
                    // 忽略无法解析的占位符，继续处理未处理的值
                    startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                } else {
                    throw new IllegalArgumentException(
                            "Could not resolve placeholder '" + placeholder + "'" + " in value \"" + value + "\"");
                }
                // 从访问集合中移除当前占位符
                visitedPlaceholders.remove(originalPlaceholder);
            } else {
                startIndex = -1;
            }
        }
        return result.toString();
    }

    /**
     * 查找占位符的结束索引位置。
     * 支持嵌套占位符的处理。
     *
     * @param buf 要搜索的字符序列
     * @param startIndex 开始搜索的索引位置
     * @return 占位符结束位置的索引，如果未找到则返回 -1
     */
    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            // 检查是否匹配后缀
            if (substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    // 如果在嵌套占位符中，减少嵌套层级
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                } else {
                    // 找到最外层占位符的结束位置
                    return index;
                }
            } else if (substringMatch(buf, index, this.simplePrefix)) {
                // 检测到嵌套占位符的开始，增加嵌套层级
                withinNestedPlaceholder++;
                index = index + this.simplePrefix.length();
            } else {
                index++;
            }
        }
        return -1;
    }

    /**
     * 测试给定字符串在给定索引处是否匹配给定的子字符串。
     *
     * @param str 原始字符串（或 StringBuilder）
     * @param index 原始字符串中开始匹配的索引位置
     * @param substring 要匹配的子字符串
     * @return 如果在指定索引处匹配则返回 true，否则返回 false
     */
    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        // 检查长度是否足够
        if (index + substring.length() > str.length()) {
            return false;
        }
        // 逐字符比较
        for (int i = 0; i < substring.length(); i++) {
            if (str.charAt(index + i) != substring.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 策略接口，用于解析字符串中包含的占位符的替换值。
     */
    public interface PlaceholderResolver {

        /**
         * 将提供的占位符名称解析为替换值。
         *
         * @param placeholderName 要解析的占位符名称
         * @return 替换值，如果不进行替换则返回 {@code null}
         */
        String resolvePlaceholder(String placeholderName);
    }

}
