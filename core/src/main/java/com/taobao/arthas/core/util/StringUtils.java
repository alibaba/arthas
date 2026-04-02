/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.taobao.arthas.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;

/**
 * 字符串工具类
 * 提供各种字符串操作工具方法
 */
public abstract class StringUtils {
    /** 日志记录器 */
    private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
    /** 用于生成随机字符串的字符集：数字、大写字母、小写字母 */
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * 获取异常的原因描述
     *
     * @param t 异常
     * @return 异常原因
     */
    public static String cause(Throwable t) {
        if (null != t.getCause()) {
            return cause(t.getCause());
        }
        return t.getMessage();
    }

    /**
     * 将一个对象转换为字符串
     *
     * @param obj 目标对象
     * @return 字符串
     */
    public static String objectToString(Object obj) {
        if (null == obj) {
            return Constants.EMPTY_STRING;
        }
        try {
            return obj.toString();
        } catch (Throwable t) {
            logger.error("objectToString error, obj class: {}", obj.getClass(), t);
            return "ERROR DATA!!! Method toString() throw exception. obj class: " + obj.getClass()
                    + ", exception class: " + t.getClass()
                    + ", exception message: " + t.getMessage();
        }
    }

    /**
     * 翻译类名称
     *
     * @param clazz Java类
     * @return 翻译值
     */
    public static String classname(Class<?> clazz) {
        if (clazz.isArray()) {
            StringBuilder sb = new StringBuilder(clazz.getName());
            sb.delete(0, 2);
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ';') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("[]");
            return sb.toString();
        } else {
            return clazz.getName();
        }
    }

    /**
     * 翻译类名称<br/>
     * 将 java/lang/String 的名称翻译成 java.lang.String
     *
     * @param className 类名称 java/lang/String
     * @return 翻译后名称 java.lang.String
     */
    public static String normalizeClassName(String className) {
        return StringUtils.replace(className, "/", ".");
    }

    /**
     * 将多个类名连接成一个字符串
     *
     * @param separator 分隔符
     * @param types     要连接的类数组
     * @return 连接后的字符串
     */
    public static String concat(String separator, Class<?>... types) {
        if (types == null || types.length == 0) {
            return Constants.EMPTY_STRING;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            builder.append(classname(types[i]));
            if (i < types.length - 1) {
                builder.append(separator);
            }
        }

        return builder.toString();
    }

    /**
     * 将多个字符串连接成一个字符串
     *
     * @param separator 分隔符
     * @param strs      要连接的字符串数组
     * @return 连接后的字符串
     */
    public static String concat(String separator, String... strs) {
        if (strs == null || strs.length == 0) {
            return Constants.EMPTY_STRING;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strs.length; i++) {
            builder.append(strs[i]);
            if (i < strs.length - 1) {
                builder.append(separator);
            }
        }

        return builder.toString();
    }

    /**
     * 翻译Modifier值
     *
     * @param mod modifier
     * @return 翻译值
     */
    public static String modifier(int mod, char splitter) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isAbstract(mod)) {
            sb.append("abstract").append(splitter);
        }
        if (Modifier.isFinal(mod)) {
            sb.append("final").append(splitter);
        }
        if (Modifier.isInterface(mod)) {
            sb.append("interface").append(splitter);
        }
        if (Modifier.isNative(mod)) {
            sb.append("native").append(splitter);
        }
        if (Modifier.isPrivate(mod)) {
            sb.append("private").append(splitter);
        }
        if (Modifier.isProtected(mod)) {
            sb.append("protected").append(splitter);
        }
        if (Modifier.isPublic(mod)) {
            sb.append("public").append(splitter);
        }
        if (Modifier.isStatic(mod)) {
            sb.append("static").append(splitter);
        }
        if (Modifier.isStrict(mod)) {
            sb.append("strict").append(splitter);
        }
        if (Modifier.isSynchronized(mod)) {
            sb.append("synchronized").append(splitter);
        }
        if (Modifier.isTransient(mod)) {
            sb.append("transient").append(splitter);
        }
        if (Modifier.isVolatile(mod)) {
            sb.append("volatile").append(splitter);
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * 自动换行
     *
     * @param string 字符串
     * @param width  行宽
     * @return 换行后的字符串
     */
    public static String wrap(String string, int width) {
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = string.toCharArray();
        int count = 0;
        for (char c : buffer) {

            if (count == width) {
                count = 0;
                sb.append('\n');
                if (c == '\n') {
                    continue;
                }
            }

            if (c == '\n') {
                count = 0;
            } else {
                count++;
            }

            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * <p>填充常量可以扩展的最大大小。</p>
     */
    private static final int PAD_LIMIT = 8192;

    /**
     * 表示索引搜索失败的返回值
     * @since 2.1
     */
    public static final int INDEX_NOT_FOUND = -1;

    /** 构造函数 */
    public StringUtils() {
    }

    /**
     * 检查对象是否为空
     *
     * @param str 要检查的对象
     * @return 如果对象为null或空字符串则返回true
     */
    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    /**
     * 检查字符序列是否有长度
     *
     * @param str 要检查的字符序列
     * @return 如果字符序列不为null且长度大于0则返回true
     */
    public static boolean hasLength(CharSequence str) {
        return str != null && str.length() > 0;
    }

    /**
     * 检查字符串是否有长度
     *
     * @param str 要检查的字符串
     * @return 如果字符串不为null且长度大于0则返回true
     */
    public static boolean hasLength(String str) {
        return hasLength((CharSequence)str);
    }

    /**
     * 检查字符序列是否包含实际文本（非空白字符）
     *
     * @param str 要检查的字符序列
     * @return 如果字符序列不为null、长度大于0且包含至少一个非空白字符则返回true
     */
    public static boolean hasText(CharSequence str) {
        if(!hasLength(str)) {
            return false;
        } else {
            int strLen = str.length();

            for(int i = 0; i < strLen; ++i) {
                if(!Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 检查字符串是否包含实际文本（非空白字符）
     *
     * @param str 要检查的字符串
     * @return 如果字符串不为null、长度大于0且包含至少一个非空白字符则返回true
     */
    public static boolean hasText(String str) {
        return hasText((CharSequence)str);
    }

    /**
     * 检查字符序列是否包含空白字符
     *
     * @param str 要检查的字符序列
     * @return 如果字符序列包含至少一个空白字符则返回true
     */
    public static boolean containsWhitespace(CharSequence str) {
        if(!hasLength(str)) {
            return false;
        } else {
            int strLen = str.length();

            for(int i = 0; i < strLen; ++i) {
                if(Character.isWhitespace(str.charAt(i))) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 检查字符串是否包含空白字符
     *
     * @param str 要检查的字符串
     * @return 如果字符串包含至少一个空白字符则返回true
     */
    public static boolean containsWhitespace(String str) {
        return containsWhitespace((CharSequence)str);
    }

    /**
     * 去除字符串两端的空白字符
     *
     * @param str 要处理的字符串
     * @return 去除两端空白字符后的字符串
     */
    public static String trimWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            // 删除开头的空白字符
            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
                sb.deleteCharAt(0);
            }

            // 删除结尾的空白字符
            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    /**
     * 去除字符串中所有的空白字符
     *
     * @param str 要处理的字符串
     * @return 去除所有空白字符后的字符串
     */
    public static String trimAllWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);
            int index = 0;

            // 遍历字符串，删除所有空白字符
            while(sb.length() > index) {
                if(Character.isWhitespace(sb.charAt(index))) {
                    sb.deleteCharAt(index);
                } else {
                    ++index;
                }
            }

            return sb.toString();
        }
    }

    /**
     * 去除字符串开头的空白字符
     *
     * @param str 要处理的字符串
     * @return 去除开头空白字符后的字符串
     */
    public static String trimLeadingWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            // 删除开头的空白字符
            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
                sb.deleteCharAt(0);
            }

            return sb.toString();
        }
    }

    /**
     * 去除字符串结尾的空白字符
     *
     * @param str 要处理的字符串
     * @return 去除结尾空白字符后的字符串
     */
    public static String trimTrailingWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            // 删除结尾的空白字符
            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    /**
     * 去除字符串开头的指定字符
     *
     * @param str              要处理的字符串
     * @param leadingCharacter 要去除的开头字符
     * @return 去除开头指定字符后的字符串
     */
    public static String trimLeadingCharacter(String str, char leadingCharacter) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            // 删除开头的指定字符
            while(sb.length() > 0 && sb.charAt(0) == leadingCharacter) {
                sb.deleteCharAt(0);
            }

            return sb.toString();
        }
    }

    /**
     * 去除字符串结尾的指定字符
     *
     * @param str               要处理的字符串
     * @param trailingCharacter 要去除的结尾字符
     * @return 去除结尾指定字符后的字符串
     */
    public static String trimTrailingCharacter(String str, char trailingCharacter) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            // 删除结尾的指定字符
            while(sb.length() > 0 && sb.charAt(sb.length() - 1) == trailingCharacter) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    /**
     * 检查字符串是否以指定前缀开头（忽略大小写）
     *
     * @param str    要检查的字符串
     * @param prefix 前缀
     * @return 如果字符串以指定前缀开头（忽略大小写）则返回true
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if(str != null && prefix != null) {
            // 先进行精确匹配，如果匹配则直接返回true
            if(str.startsWith(prefix)) {
                return true;
            } else if(str.length() < prefix.length()) {
                return false;
            } else {
                // 转换为小写进行比较
                String lcStr = str.substring(0, prefix.length()).toLowerCase();
                String lcPrefix = prefix.toLowerCase();
                return lcStr.equals(lcPrefix);
            }
        } else {
            return false;
        }
    }

    /**
     * 检查字符串是否以指定后缀结尾（忽略大小写）
     *
     * @param str    要检查的字符串
     * @param suffix 后缀
     * @return 如果字符串以指定后缀结尾（忽略大小写）则返回true
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if(str != null && suffix != null) {
            // 先进行精确匹配，如果匹配则直接返回true
            if(str.endsWith(suffix)) {
                return true;
            } else if(str.length() < suffix.length()) {
                return false;
            } else {
                // 转换为小写进行比较
                String lcStr = str.substring(str.length() - suffix.length()).toLowerCase();
                String lcSuffix = suffix.toLowerCase();
                return lcStr.equals(lcSuffix);
            }
        } else {
            return false;
        }
    }

    /**
     * 检查字符串从指定位置开始是否匹配指定的子串
     *
     * @param str      要检查的字符串
     * @param index    开始检查的位置
     * @param substring 要匹配的子串
     * @return 如果从指定位置开始匹配子串则返回true
     */
    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        for(int j = 0; j < substring.length(); ++j) {
            int i = index + j;
            // 检查是否超出字符串长度或字符不匹配
            if(i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取分隔符之后的子串
     *
     * @param str       原字符串
     * @param separator 分隔符
     * @return 分隔符之后的子串，如果找不到分隔符则返回空字符串
     */
    public static String substringAfter(String str, String separator) {
        if (isEmpty(str)) {
            return str;
        } else if (separator == null) {
            return "";
        } else {
            int pos = str.indexOf(separator);
            return pos == -1 ? "" : str.substring(pos + separator.length());
        }
    }

    /**
     * 获取最后一个分隔符之前的子串
     *
     * @param str       原字符串
     * @param separator 分隔符
     * @return 最后一个分隔符之前的子串，如果找不到分隔符则返回原字符串
     */
    public static String substringBeforeLast(String str, String separator) {
        if (!isEmpty(str) && !isEmpty(separator)) {
            int pos = str.lastIndexOf(separator);
            return pos == -1 ? str : str.substring(0, pos);
        } else {
            return str;
        }
    }

    /**
     * 获取第一个分隔符之前的子串
     *
     * @param str       原字符串
     * @param separator 分隔符
     * @return 第一个分隔符之前的子串，如果找不到分隔符则返回原字符串
     */
    public static String substringBefore(final String str, final String separator) {
        if (isEmpty(str) || separator == null) {
            return str;
        }
        if (separator.isEmpty()) {
            return Constants.EMPTY_STRING;
        }
        final int pos = str.indexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    /**
     * 获取最后一个分隔符之后的子串
     *
     * @param str       原字符串
     * @param separator 分隔符
     * @return 最后一个分隔符之后的子串，如果找不到分隔符则返回空字符串
     */
    public static String substringAfterLast(String str, String separator) {
        if (isEmpty(str)) {
            return str;
        } else if (isEmpty(separator)) {
            return "";
        } else {
            int pos = str.lastIndexOf(separator);
            return pos != -1 && pos != str.length() - separator.length() ? str.substring(pos + separator.length()) : "";
        }
    }

    /**
     * 统计子串在字符串中出现的次数
     *
     * @param str 原字符串
     * @param sub 要统计的子串
     * @return 子串出现的次数
     */
    public static int countOccurrencesOf(String str, String sub) {
        if(str != null && sub != null && str.length() != 0 && sub.length() != 0) {
            int count = 0;

            int idx;
            // 从前往后查找子串，每次找到后计数加1，并从找到的位置继续查找
            for(int pos = 0; (idx = str.indexOf(sub, pos)) != -1; pos = idx + sub.length()) {
                ++count;
            }

            return count;
        } else {
            return 0;
        }
    }

    /**
     * 替换字符串中的旧模式为新模式
     *
     * @param inString   原字符串
     * @param oldPattern 要替换的旧模式
     * @param newPattern 新模式
     * @return 替换后的字符串
     */
    public static String replace(String inString, String oldPattern, String newPattern) {
        if(hasLength(inString) && hasLength(oldPattern) && newPattern != null) {
            int pos = 0;
            int index = inString.indexOf(oldPattern);
            if (index < 0) {
                // 如果没有找到要替换的模式，直接返回原字符串
                return inString;
            }

            StringBuilder sb = new StringBuilder();
            for(int patLen = oldPattern.length(); index >= 0; index = inString.indexOf(oldPattern, pos)) {
                // 添加旧模式之前的部分
                sb.append(inString, pos, index);
                // 添加新模式
                sb.append(newPattern);
                // 更新位置到旧模式之后
                pos = index + patLen;
            }

            // 添加剩余的部分
            sb.append(inString.substring(pos));
            return sb.toString();
        } else {
            return inString;
        }
    }

    /**
     * 删除字符串中的指定模式
     *
     * @param inString 原字符串
     * @param pattern  要删除的模式
     * @return 删除指定模式后的字符串
     */
    public static String delete(String inString, String pattern) {
        return replace(inString, pattern, "");
    }

    /**
     * 删除字符串中所有出现在指定字符集中的字符
     *
     * @param inString     原字符串
     * @param charsToDelete 要删除的字符集
     * @return 删除指定字符后的字符串
     */
    public static String deleteAny(String inString, String charsToDelete) {
        if(hasLength(inString) && hasLength(charsToDelete)) {
            StringBuilder sb = new StringBuilder();

            // 遍历字符串，只保留不在删除字符集中的字符
            for(int i = 0; i < inString.length(); ++i) {
                char c = inString.charAt(i);
                if(charsToDelete.indexOf(c) == -1) {
                    sb.append(c);
                }
            }

            return sb.toString();
        } else {
            return inString;
        }
    }

    /**
     * 为字符串添加单引号
     *
     * @param str 要处理的字符串
     * @return 添加单引号后的字符串，如果输入为null则返回null
     */
    public static String quote(String str) {
        return str != null?"\'" + str + "\'":null;
    }

    /**
     * 如果对象是字符串类型，则为其添加单引号
     *
     * @param obj 要处理的对象
     * @return 如果是字符串则添加单引号，否则返回原对象
     */
    public static Object quoteIfString(Object obj) {
        return obj instanceof String?quote((String)obj):obj;
    }

    /**
     * 获取限定名称的简单名称（去除包名）
     *
     * @param qualifiedName 限定名称
     * @return 简单名称
     */
    public static String unqualify(String qualifiedName) {
        return unqualify(qualifiedName, '.');
    }

    /**
     * 获取限定名称的简单名称（去除分隔符之前的部分）
     *
     * @param qualifiedName 限定名称
     * @param separator     分隔符
     * @return 简单名称
     */
    public static String unqualify(String qualifiedName, char separator) {
        return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
    }

    /**
     * 将字符串的首字母大写
     *
     * @param str 要处理的字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        return changeFirstCharacterCase(str, true);
    }

    /**
     * 将字符串的首字母小写
     *
     * @param str 要处理的字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        return changeFirstCharacterCase(str, false);
    }

    /**
     * 改变字符串首字母的大小写
     *
     * @param str       要处理的字符串
     * @param capitalize true表示大写，false表示小写
     * @return 改变首字母大小写后的字符串
     */
    private static String changeFirstCharacterCase(String str, boolean capitalize) {
        if(str != null && str.length() != 0) {
            StringBuilder sb = new StringBuilder(str.length());
            if(capitalize) {
                // 首字母大写
                sb.append(Character.toUpperCase(str.charAt(0)));
            } else {
                // 首字母小写
                sb.append(Character.toLowerCase(str.charAt(0)));
            }

            // 添加剩余部分
            sb.append(str.substring(1));
            return sb.toString();
        } else {
            return str;
        }
    }

    /**
     * 将字符串集合转换为字符串数组
     *
     * @param collection 字符串集合
     * @return 字符串数组，如果集合为null则返回null
     */
    public static String[] toStringArray(Collection<String> collection) {
        return collection == null?null:(String[])collection.toArray(new String[0]);
    }

    /**
     * 根据分隔符将字符串分成两部分
     *
     * @param toSplit   要分割的字符串
     * @param delimiter 分隔符
     * @return 包含两部分的字符串数组，如果找不到分隔符则返回null
     */
    public static String[] split(String toSplit, String delimiter) {
        if(hasLength(toSplit) && hasLength(delimiter)) {
            int offset = toSplit.indexOf(delimiter);
            if(offset < 0) {
                return null;
            } else {
                String beforeDelimiter = toSplit.substring(0, offset);
                String afterDelimiter = toSplit.substring(offset + delimiter.length());
                return new String[]{beforeDelimiter, afterDelimiter};
            }
        } else {
            return null;
        }
    }

    /**
     * 将字符串数组按分隔符分割成Properties对象
     *
     * @param array     字符串数组
     * @param delimiter 分隔符
     * @return Properties对象
     */
    public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
        return splitArrayElementsIntoProperties(array, delimiter, (String)null);
    }

    /**
     * 将字符串数组按分隔符分割成Properties对象，并删除指定字符
     *
     * @param array         字符串数组
     * @param delimiter     分隔符
     * @param charsToDelete 要删除的字符
     * @return Properties对象
     */
    public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter, String charsToDelete) {
        if(ObjectUtils.isEmpty(array)) {
            return null;
        } else {
            Properties result = new Properties();
            String[] var4 = array;
            int var5 = array.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String element = var4[var6];
                // 删除指定字符
                if(charsToDelete != null) {
                    element = deleteAny(element, charsToDelete);
                }

                // 分割字符串并设置属性
                String[] splittedElement = split(element, delimiter);
                if(splittedElement != null) {
                    result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
                }
            }

            return result;
        }
    }

    /**
     * 将字符串按分隔符标记化为字符串数组
     *
     * @param str        要标记化的字符串
     * @param delimiters 分隔符
     * @return 字符串数组
     */
    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    /**
     * 将字符串按分隔符标记化为字符串数组
     *
     * @param str               要标记化的字符串
     * @param delimiters        分隔符
     * @param trimTokens        是否修剪标记的空白字符
     * @param ignoreEmptyTokens 是否忽略空标记
     * @return 字符串数组
     */
    public static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
        if(str == null) {
            return null;
        } else {
            StringTokenizer st = new StringTokenizer(str, delimiters);
            ArrayList<String> tokens = new ArrayList<String>();

            while(true) {
                String token;
                do {
                    if(!st.hasMoreTokens()) {
                        return toStringArray(tokens);
                    }

                    token = st.nextToken();
                    // 修剪标记的空白字符
                    if(trimTokens) {
                        token = token.trim();
                    }
                } while(ignoreEmptyTokens && token.length() <= 0);

                tokens.add(token);
            }
        }
    }

    /**
     * 将分隔的字符串列表转换为字符串数组
     *
     * @param str       要转换的字符串
     * @param delimiter 分隔符
     * @return 字符串数组
     */
    public static String[] delimitedListToStringArray(String str, String delimiter) {
        return delimitedListToStringArray(str, delimiter, (String)null);
    }

    /**
     * 将分隔的字符串列表转换为字符串数组，并删除指定字符
     *
     * @param str           要转换的字符串
     * @param delimiter     分隔符
     * @param charsToDelete 要删除的字符
     * @return 字符串数组
     */
    public static String[] delimitedListToStringArray(String str, String delimiter, String charsToDelete) {
        if(str == null) {
            return new String[0];
        } else if(delimiter == null) {
            return new String[]{str};
        } else {
            ArrayList<String> result = new ArrayList<String>();
            int pos;
            if("".equals(delimiter)) {
                // 如果分隔符为空，则将每个字符作为一个元素
                for(pos = 0; pos < str.length(); ++pos) {
                    result.add(deleteAny(str.substring(pos, pos + 1), charsToDelete));
                }
            } else {
                int delPos;
                // 按分隔符分割字符串
                for(pos = 0; (delPos = str.indexOf(delimiter, pos)) != -1; pos = delPos + delimiter.length()) {
                    result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
                }

                // 添加最后一部分
                if(str.length() > 0 && pos <= str.length()) {
                    result.add(deleteAny(str.substring(pos), charsToDelete));
                }
            }

            return toStringArray(result);
        }
    }

    /**
     * 将逗号分隔的字符串列表转换为字符串数组
     *
     * @param str 逗号分隔的字符串
     * @return 字符串数组
     */
    public static String[] commaDelimitedListToStringArray(String str) {
        return delimitedListToStringArray(str, ",");
    }

    /**
     * 将逗号分隔的字符串列表转换为有序字符串集合
     *
     * @param str 逗号分隔的字符串
     * @return 有序字符串集合
     */
    public static Set<String> commaDelimitedListToSet(String str) {
        String[] tokens = commaDelimitedListToStringArray(str);
        return new TreeSet<String>(Arrays.asList(tokens));
    }

    /**
     * 将数组中的元素连接成一个字符串
     *
     * <p>
     * 列表前后不添加分隔符。null分隔符等同于空字符串。
     * </p>
     *
     * @param array     要连接的数组
     * @param separator 使用的分隔符
     * @return 连接后的字符串
     */
    public static String join(Object[] array, String separator) {
        if (separator == null) {
            separator = "";
        }
        int arraySize = array.length;
        int bufSize = (arraySize == 0 ? 0 : (array[0].toString().length() + separator.length()) * arraySize);
        StringBuilder buf = new StringBuilder(bufSize);

        for (int i = 0; i < arraySize; i++) {
            if (i > 0) {
                buf.append(separator);
            }
            buf.append(array[i]);
        }
        return buf.toString();
    }

    /**
     * 检查字符序列是否为空白、空字符串或null
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs 要检查的字符序列，可以为null
     * @return 如果字符序列为null、空或全是空白字符则返回true
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        // 检查是否所有字符都是空白字符
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 返回使用指定分隔符重复到给定长度的填充字符串
     *
     * <pre>
     * StringUtils.repeat('e', 0)  = ""
     * StringUtils.repeat('e', 3)  = "eee"
     * StringUtils.repeat('e', -2) = ""
     * </pre>
     *
     * <p>注意：此方法不支持使用
     * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode补充字符</a>
     * 进行填充，因为它们需要一对{@code char}来表示。
     * 如果需要支持应用程序的完整国际化，请考虑使用{@link #repeat(String, int)}。
     * </p>
     *
     * @param ch     要重复的字符
     * @param repeat 重复次数，负数将被视为零
     * @return 包含重复字符的字符串
     * @see #repeat(String, int)
     */
    public static String repeat(final char ch, final int repeat) {
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    /**
     * 将字符串重复指定次数以形成新字符串
     *
     * <pre>
     * StringUtils.repeat(null, 2) = null
     * StringUtils.repeat("", 0)   = ""
     * StringUtils.repeat("", 2)   = ""
     * StringUtils.repeat("a", 3)  = "aaa"
     * StringUtils.repeat("ab", 2) = "abab"
     * StringUtils.repeat("a", -2) = ""
     * </pre>
     *
     * @param str    要重复的字符串，可以为null
     * @param repeat 重复次数，负数将被视为零
     * @return 由原字符串重复组成的新字符串，如果输入为null则返回null
     */
    public static String repeat(final String str, final int repeat) {
        // 针对2.0版本（JDK1.4）进行了性能优化

        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return Constants.EMPTY_STRING;
        }
        final int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        // 如果字符串长度为1且重复次数在限制范围内，使用字符版本
        if (inputLength == 1 && repeat <= PAD_LIMIT) {
            return repeat(str.charAt(0), repeat);
        }

        final int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1 :
                return repeat(str.charAt(0), repeat);
            case 2 :
                // 优化两个字符的字符串重复
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default :
                // 使用StringBuilder进行常规重复
                final StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    /**
     * 获取字符序列的长度，如果字符序列为null则返回0
     *
     * @param cs 字符序列或null
     * @return 字符序列的长度，如果字符序列为null则返回0
     * @since 2.4
     * @since 3.0 Changed signature from length(String) to length(CharSequence)
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * 从字符串末尾去除一组字符中的任意字符
     *
     * <p>null输入字符串返回null。空字符串("")输入返回空字符串。</p>
     *
     * <p>如果stripChars为null，则按照{@link Character#isWhitespace(char)}的定义去除空白字符。</p>
     *
     * <pre>
     * StringUtils.stripEnd(null, *)          = null
     * StringUtils.stripEnd("", *)            = ""
     * StringUtils.stripEnd("abc", "")        = "abc"
     * StringUtils.stripEnd("abc", null)      = "abc"
     * StringUtils.stripEnd("  abc", null)    = "  abc"
     * StringUtils.stripEnd("abc  ", null)    = "abc"
     * StringUtils.stripEnd(" abc ", null)    = " abc"
     * StringUtils.stripEnd("  abcyx", "xyz") = "  abc"
     * StringUtils.stripEnd("120.00", ".0")   = "12"
     * </pre>
     *
     * @param str        要去除字符的字符串，可以为null
     * @param stripChars 要去除的字符集合，null将被视为空白字符
     * @return 去除后的字符串，如果输入为null则返回null
     */
    public static String stripEnd(final String str, final String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            // 去除空白字符
            while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            // 去除指定字符
            while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != INDEX_NOT_FOUND) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    /**
     * 获取类的类加载器哈希码
     *
     * @param clazz 要检查的类
     * @return 类加载器的哈希码（十六进制字符串），如果类或类加载器为null则返回"null"
     */
    public static String classLoaderHash(Class<?> clazz) {
        if (clazz == null || clazz.getClassLoader() == null) {
            return "null";
        }
        return Integer.toHexString(clazz.getClassLoader().hashCode());
    }

    /**
     * 将字节大小格式化为人类可读的格式
     * 参考链接：https://stackoverflow.com/a/3758880
     *
     * @param bytes 字节数
     * @return 人类可读的格式（如：1.5 KiB, 2.3 MiB等）
     */
    public static String humanReadableByteCount(long bytes) {
        return bytes < 1024L ? bytes + " B"
                : bytes < 0xfffccccccccccccL >> 40 ? String.format("%.1f KiB", bytes / 0x1p10)
                : bytes < 0xfffccccccccccccL >> 30 ? String.format("%.1f MiB", bytes / 0x1p20)
                : bytes < 0xfffccccccccccccL >> 20 ? String.format("%.1f GiB", bytes / 0x1p30)
                : bytes < 0xfffccccccccccccL >> 10 ? String.format("%.1f TiB", bytes / 0x1p40)
                : bytes < 0xfffccccccccccccL ? String.format("%.1f PiB", (bytes >> 10) / 0x1p40)
                : String.format("%.1f EiB", (bytes >> 20) / 0x1p40);
    }

    /**
     * 将文本按行分割成字符串列表
     *
     * @param text 要分割的文本
     * @return 包含每一行的字符串列表
     */
    public static List<String> toLines(String text) {
        List<String> result = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(text));
        try {
            String line = reader.readLine();
            while (line != null) {
                result.add(line);
                line = reader.readLine();
            }
        } catch (IOException exc) {
            // 退出
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // 忽略
            }
        }
        return result;
    }

    /**
     * 生成指定长度的随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(AB.charAt(ThreadLocalRandom.current().nextInt(AB.length())));
        return sb.toString();
    }

    /**
     * 获取指定标记之前的字符串
     *
     * @param text   原文本
     * @param before 标记
     * @return 标记之前的字符串，如果文本不包含标记则返回null
     */
    public static String before(String text, String before) {
        int pos = text.indexOf(before);
        return pos == -1 ? null : text.substring(0, pos);
    }

    /**
     * 获取指定标记之后的字符串
     *
     * @param text  原文本
     * @param after 标记
     * @return 标记之后的字符串，如果文本不包含标记则返回null
     */
    public static String after(String text, String after) {
        int pos = text.indexOf(after);
        if (pos == -1) {
            return null;
        }
        return text.substring(pos + after.length());
    }

    /**
     * 分割方法信息
     * 格式示例：print|(ILjava/util/List;)V
     *
     * @param methodInfo 方法信息字符串
     * @return 包含方法名和方法描述符的数组
     */
    // print|(ILjava/util/List;)V
    public static String[] splitMethodInfo(String methodInfo) {
        int index = methodInfo.indexOf('|');
        return new String[] { methodInfo.substring(0, index), methodInfo.substring(index + 1) };
    }

    /**
     * 分割调用信息
     * 格式示例：demo/MathGame|primeFactors|(I)Ljava/util/List;|24
     *
     * @param invokeInfo 调用信息字符串
     * @return 包含类名、方法名、方法描述符和行号的数组
     */
    // demo/MathGame|primeFactors|(I)Ljava/util/List;|24
    public static String[] splitInvokeInfo(String invokeInfo) {
        int index1 = invokeInfo.indexOf('|');
        int index2 = invokeInfo.indexOf('|', index1 + 1);
        int index3 = invokeInfo.indexOf('|', index2 + 1);
        return new String[] { invokeInfo.substring(0, index1), invokeInfo.substring(index1 + 1, index2),
                invokeInfo.substring(index2 + 1, index3), invokeInfo.substring(index3 + 1) };
    }

    /**
     * 美化名称（将空格替换为下划线并转为小写）
     *
     * @param name 原名称
     * @return 美化后的名称
     */
    public static String beautifyName(String name) {
        return name.replace(' ', '_').toLowerCase();
    }

    /**
     * 将URL数组转换为字符串列表
     *
     * @param urls URL数组
     * @return 字符串列表，如果输入为null则返回空列表
     */
    public static List<String> toStringList(URL[] urls) {
        if (urls != null) {
            List<String> result = new ArrayList<String>(urls.length);
            for (URL url : urls) {
                result.add(url.toString());
            }
            return result;
        }
        return Collections.emptyList();
    }
}
