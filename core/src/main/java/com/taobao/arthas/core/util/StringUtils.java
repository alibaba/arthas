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

public abstract class StringUtils {
    private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
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
     * <p>The maximum size to which the padding constant(s) can expand.</p>
     */
    private static final int PAD_LIMIT = 8192;

    /**
     * Represents a failed index search.
     * @since 2.1
     */
    public static final int INDEX_NOT_FOUND = -1;

    public StringUtils() {
    }

    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    public static boolean hasLength(CharSequence str) {
        return str != null && str.length() > 0;
    }

    public static boolean hasLength(String str) {
        return hasLength((CharSequence)str);
    }

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

    public static boolean hasText(String str) {
        return hasText((CharSequence)str);
    }

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

    public static boolean containsWhitespace(String str) {
        return containsWhitespace((CharSequence)str);
    }

    public static String trimWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
                sb.deleteCharAt(0);
            }

            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    public static String trimAllWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);
            int index = 0;

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

    public static String trimLeadingWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(0))) {
                sb.deleteCharAt(0);
            }

            return sb.toString();
        }
    }

    public static String trimTrailingWhitespace(String str) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            while(sb.length() > 0 && Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    public static String trimLeadingCharacter(String str, char leadingCharacter) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            while(sb.length() > 0 && sb.charAt(0) == leadingCharacter) {
                sb.deleteCharAt(0);
            }

            return sb.toString();
        }
    }

    public static String trimTrailingCharacter(String str, char trailingCharacter) {
        if(!hasLength(str)) {
            return str;
        } else {
            StringBuilder sb = new StringBuilder(str);

            while(sb.length() > 0 && sb.charAt(sb.length() - 1) == trailingCharacter) {
                sb.deleteCharAt(sb.length() - 1);
            }

            return sb.toString();
        }
    }

    public static boolean startsWithIgnoreCase(String str, String prefix) {
        if(str != null && prefix != null) {
            if(str.startsWith(prefix)) {
                return true;
            } else if(str.length() < prefix.length()) {
                return false;
            } else {
                String lcStr = str.substring(0, prefix.length()).toLowerCase();
                String lcPrefix = prefix.toLowerCase();
                return lcStr.equals(lcPrefix);
            }
        } else {
            return false;
        }
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        if(str != null && suffix != null) {
            if(str.endsWith(suffix)) {
                return true;
            } else if(str.length() < suffix.length()) {
                return false;
            } else {
                String lcStr = str.substring(str.length() - suffix.length()).toLowerCase();
                String lcSuffix = suffix.toLowerCase();
                return lcStr.equals(lcSuffix);
            }
        } else {
            return false;
        }
    }

    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        for(int j = 0; j < substring.length(); ++j) {
            int i = index + j;
            if(i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }

        return true;
    }

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

    public static String substringBeforeLast(String str, String separator) {
        if (!isEmpty(str) && !isEmpty(separator)) {
            int pos = str.lastIndexOf(separator);
            return pos == -1 ? str : str.substring(0, pos);
        } else {
            return str;
        }
    }

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

    public static int countOccurrencesOf(String str, String sub) {
        if(str != null && sub != null && str.length() != 0 && sub.length() != 0) {
            int count = 0;

            int idx;
            for(int pos = 0; (idx = str.indexOf(sub, pos)) != -1; pos = idx + sub.length()) {
                ++count;
            }

            return count;
        } else {
            return 0;
        }
    }

    public static String replace(String inString, String oldPattern, String newPattern) {
        if(hasLength(inString) && hasLength(oldPattern) && newPattern != null) {
            int pos = 0;
            int index = inString.indexOf(oldPattern);
            if (index < 0) {
                //no need to replace
                return inString;
            }

            StringBuilder sb = new StringBuilder();
            for(int patLen = oldPattern.length(); index >= 0; index = inString.indexOf(oldPattern, pos)) {
                sb.append(inString, pos, index);
                sb.append(newPattern);
                pos = index + patLen;
            }

            sb.append(inString.substring(pos));
            return sb.toString();
        } else {
            return inString;
        }
    }

    public static String delete(String inString, String pattern) {
        return replace(inString, pattern, "");
    }

    public static String deleteAny(String inString, String charsToDelete) {
        if(hasLength(inString) && hasLength(charsToDelete)) {
            StringBuilder sb = new StringBuilder();

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

    public static String quote(String str) {
        return str != null?"\'" + str + "\'":null;
    }

    public static Object quoteIfString(Object obj) {
        return obj instanceof String?quote((String)obj):obj;
    }

    public static String unqualify(String qualifiedName) {
        return unqualify(qualifiedName, '.');
    }

    public static String unqualify(String qualifiedName, char separator) {
        return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
    }

    public static String capitalize(String str) {
        return changeFirstCharacterCase(str, true);
    }

    public static String uncapitalize(String str) {
        return changeFirstCharacterCase(str, false);
    }

    private static String changeFirstCharacterCase(String str, boolean capitalize) {
        if(str != null && str.length() != 0) {
            StringBuilder sb = new StringBuilder(str.length());
            if(capitalize) {
                sb.append(Character.toUpperCase(str.charAt(0)));
            } else {
                sb.append(Character.toLowerCase(str.charAt(0)));
            }

            sb.append(str.substring(1));
            return sb.toString();
        } else {
            return str;
        }
    }

    public static String[] toStringArray(Collection<String> collection) {
        return collection == null?null:(String[])collection.toArray(new String[0]);
    }

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

    public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
        return splitArrayElementsIntoProperties(array, delimiter, (String)null);
    }

    public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter, String charsToDelete) {
        if(ObjectUtils.isEmpty(array)) {
            return null;
        } else {
            Properties result = new Properties();
            String[] var4 = array;
            int var5 = array.length;

            for(int var6 = 0; var6 < var5; ++var6) {
                String element = var4[var6];
                if(charsToDelete != null) {
                    element = deleteAny(element, charsToDelete);
                }

                String[] splittedElement = split(element, delimiter);
                if(splittedElement != null) {
                    result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
                }
            }

            return result;
        }
    }

    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

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
                    if(trimTokens) {
                        token = token.trim();
                    }
                } while(ignoreEmptyTokens && token.length() <= 0);

                tokens.add(token);
            }
        }
    }

    public static String[] delimitedListToStringArray(String str, String delimiter) {
        return delimitedListToStringArray(str, delimiter, (String)null);
    }

    public static String[] delimitedListToStringArray(String str, String delimiter, String charsToDelete) {
        if(str == null) {
            return new String[0];
        } else if(delimiter == null) {
            return new String[]{str};
        } else {
            ArrayList<String> result = new ArrayList<String>();
            int pos;
            if("".equals(delimiter)) {
                for(pos = 0; pos < str.length(); ++pos) {
                    result.add(deleteAny(str.substring(pos, pos + 1), charsToDelete));
                }
            } else {
                int delPos;
                for(pos = 0; (delPos = str.indexOf(delimiter, pos)) != -1; pos = delPos + delimiter.length()) {
                    result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
                }

                if(str.length() > 0 && pos <= str.length()) {
                    result.add(deleteAny(str.substring(pos), charsToDelete));
                }
            }

            return toStringArray(result);
        }
    }

    public static String[] commaDelimitedListToStringArray(String str) {
        return delimitedListToStringArray(str, ",");
    }

    public static Set<String> commaDelimitedListToSet(String str) {
        String[] tokens = commaDelimitedListToStringArray(str);
        return new TreeSet<String>(Arrays.asList(tokens));
    }

    /**
     * <p>
     * Joins the elements of the provided array into a single String containing the provided list of
     * elements.
     * </p>
     *
     * <p>
     * No delimiter is added before or after the list. A <code>null</code> separator is the same as a
     * blank String.
     * </p>
     *
     * @param array the array of values to join together
     * @param separator the separator character to use
     * @return the joined String
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
     * <p>Checks if a CharSequence is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs  the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
     */
    public static boolean isBlank(final CharSequence cs) {
        int strLen;
        if (cs == null || (strLen = cs.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Returns padding using the specified delimiter repeated
     * to a given length.</p>
     *
     * <pre>
     * StringUtils.repeat('e', 0)  = ""
     * StringUtils.repeat('e', 3)  = "eee"
     * StringUtils.repeat('e', -2) = ""
     * </pre>
     *
     * <p>Note: this method doesn't not support padding with
     * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
     * as they require a pair of {@code char}s to be represented.
     * If you are needing to support full I18N of your applications
     * consider using {@link #repeat(String, int)} instead.
     * </p>
     *
     * @param ch  character to repeat
     * @param repeat  number of times to repeat char, negative treated as zero
     * @return String with repeated character
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
     * <p>Repeat a String {@code repeat} times to form a
     * new String.</p>
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
     * @param str  the String to repeat, may be null
     * @param repeat  number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated,
     *  {@code null} if null String input
     */
    public static String repeat(final String str, final int repeat) {
        // Performance tuned for 2.0 (JDK1.4)

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
        if (inputLength == 1 && repeat <= PAD_LIMIT) {
            return repeat(str.charAt(0), repeat);
        }

        final int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1 :
                return repeat(str.charAt(0), repeat);
            case 2 :
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default :
                final StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     *
     * @param cs
     *            a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is
     *         {@code null}.
     * @since 2.4
     * @since 3.0 Changed signature from length(String) to length(CharSequence)
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * <p>Strips any of a set of characters from the end of a String.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>If the stripChars String is {@code null}, whitespace is
     * stripped as defined by {@link Character#isWhitespace(char)}.</p>
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
     * @param str  the String to remove characters from, may be null
     * @param stripChars  the set of characters to remove, null treated as whitespace
     * @return the stripped String, {@code null} if null String input
     */
    public static String stripEnd(final String str, final String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.isEmpty()) {
            return str;
        } else {
            while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != INDEX_NOT_FOUND) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    public static String classLoaderHash(Class<?> clazz) {
        if (clazz == null || clazz.getClassLoader() == null) {
            return "null";
        }
        return Integer.toHexString(clazz.getClassLoader().hashCode());
    }

    /**
     * format byte size to human readable format. https://stackoverflow.com/a/3758880
     * @param bytes byets
     * @return  human readable format
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
            // quit
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        }
        return result;
    }

    public static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(AB.charAt(ThreadLocalRandom.current().nextInt(AB.length())));
        return sb.toString();
    }

    /**
     * Returns the string before the given token
     *
     * @param text   the text
     * @param before the token
     * @return the text before the token, or <tt>null</tt> if text does not contain
     *         the token
     */
    public static String before(String text, String before) {
        int pos = text.indexOf(before);
        return pos == -1 ? null : text.substring(0, pos);
    }

    /**
     * Returns the string after the given token
     *
     * @param text  the text
     * @param after the token
     * @return the text after the token, or <tt>null</tt> if text does not contain
     *         the token
     */
    public static String after(String text, String after) {
        int pos = text.indexOf(after);
        if (pos == -1) {
            return null;
        }
        return text.substring(pos + after.length());
    }

    // print|(ILjava/util/List;)V
    public static String[] splitMethodInfo(String methodInfo) {
        int index = methodInfo.indexOf('|');
        return new String[] { methodInfo.substring(0, index), methodInfo.substring(index + 1) };
    }

    // demo/MathGame|primeFactors|(I)Ljava/util/List;|24
    public static String[] splitInvokeInfo(String invokeInfo) {
        int index1 = invokeInfo.indexOf('|');
        int index2 = invokeInfo.indexOf('|', index1 + 1);
        int index3 = invokeInfo.indexOf('|', index2 + 1);
        return new String[] { invokeInfo.substring(0, index1), invokeInfo.substring(index1 + 1, index2),
                invokeInfo.substring(index2 + 1, index3), invokeInfo.substring(index3 + 1) };
    }

    public static String beautifyName(String name) {
        return name.replace(' ', '_').toLowerCase();
    }

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
