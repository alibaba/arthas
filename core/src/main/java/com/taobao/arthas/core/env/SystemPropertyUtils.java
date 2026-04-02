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
 * 用于解析文本中的占位符的辅助类。通常应用于文件路径。
 *
 * <p>
 * 文本可能包含 {@code ${...}} 占位符，这些占位符将被解析为系统属性：
 * 例如 {@code ${user.dir}}。可以使用 ":" 分隔符在键和值之间提供默认值。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @since 1.2.5
 * @see #PLACEHOLDER_PREFIX
 * @see #PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 */
public abstract class SystemPropertyUtils {

    /** 系统属性占位符前缀: "${" */
    public static final String PLACEHOLDER_PREFIX = "${";

    /** 系统属性占位符后缀: "}" */
    public static final String PLACEHOLDER_SUFFIX = "}";

    /** 系统属性占位符的值分隔符: ":" */
    public static final String VALUE_SEPARATOR = ":";

    /** 严格模式的占位符解析助手，遇到无法解析的占位符会抛出异常 */
    private static final PropertyPlaceholderHelper strictHelper = new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX,
            PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, false);

    /** 非严格模式的占位符解析助手，遇到无法解析的占位符会忽略并保留原样 */
    private static final PropertyPlaceholderHelper nonStrictHelper = new PropertyPlaceholderHelper(PLACEHOLDER_PREFIX,
            PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, true);

    /**
     * 解析给定文本中的 {@code ${...}} 占位符，将其替换为相应的系统属性值
     * 使用严格模式，遇到无法解析的占位符会抛出异常
     *
     * @param text 要解析的字符串
     * @return 解析后的字符串
     * @throws IllegalArgumentException 如果存在无法解析的占位符
     * @see #PLACEHOLDER_PREFIX
     * @see #PLACEHOLDER_SUFFIX
     */
    public static String resolvePlaceholders(String text) {
        return resolvePlaceholders(text, false);
    }

    /**
     * 解析给定文本中的 {@code ${...}} 占位符，将其替换为相应的系统属性值
     * 如果标志设置为 {@code true}，无法解析且没有默认值的占位符将被忽略并保持不变
     *
     * @param text                           要解析的字符串
     * @param ignoreUnresolvablePlaceholders 是否忽略无法解析的占位符
     * @return 解析后的字符串
     * @throws IllegalArgumentException 如果存在无法解析的占位符且 "ignoreUnresolvablePlaceholders" 标志为 {@code false}
     * @see #PLACEHOLDER_PREFIX
     * @see #PLACEHOLDER_SUFFIX
     */
    public static String resolvePlaceholders(String text, boolean ignoreUnresolvablePlaceholders) {
        // 根据是否忽略无法解析的占位符选择相应的助手
        PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
        // 使用系统属性占位符解析器进行替换
        return helper.replacePlaceholders(text, new SystemPropertyPlaceholderResolver(text));
    }

    /**
     * 占位符解析器实现，根据系统属性和系统环境变量进行解析
     */
    private static class SystemPropertyPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        /** 当前正在解析的文本 */
        private final String text;

        /**
         * 创建系统属性占位符解析器
         *
         * @param text 要解析的文本
         */
        public SystemPropertyPlaceholderResolver(String text) {
            this.text = text;
        }

        /**
         * 解析占位符
         * 首先尝试从系统属性中获取，如果找不到则从系统环境变量中获取
         *
         * @param placeholderName 占位符名称
         * @return 解析后的值，如果找不到则返回 {@code null}
         */
        @Override
        public String resolvePlaceholder(String placeholderName) {
            try {
                // 首先尝试从系统属性中获取
                String propVal = System.getProperty(placeholderName);
                if (propVal == null) {
                    // 如果系统属性中找不到，回退到系统环境变量中搜索
                    propVal = System.getenv(placeholderName);
                }
                return propVal;
            } catch (Throwable ex) {
                // 如果解析过程中出现异常，打印错误信息并返回 null
                System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" + this.text
                        + "] as system property: " + ex);
                return null;
            }
        }
    }

}
