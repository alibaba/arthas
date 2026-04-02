/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taobao.arthas.core.env;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * PropertySources 接口的默认实现类。
 * 允许对包含的属性源进行操作，并提供了一个构造函数用于复制现有的 {@code PropertySources} 实例。
 *
 * <p>
 * 在 {@link #addFirst} 和 {@link #addLast} 等方法中提到的 <em>优先级</em>，
 * 是指在使用 {@link PropertyResolver} 解析给定属性时，属性源的搜索顺序。
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertySourcesPropertyResolver
 */
public class MutablePropertySources implements PropertySources {

    // 错误消息模板：当属性源不存在时使用
    static final String NON_EXISTENT_PROPERTY_SOURCE_MESSAGE = "PropertySource named [%s] does not exist";
    // 错误消息模板：当属性源尝试相对于自身添加时使用
    static final String ILLEGAL_RELATIVE_ADDITION_MESSAGE = "PropertySource named [%s] cannot be added relative to itself";

    // 使用 LinkedList 存储属性源列表，支持高效的头尾操作
    private final LinkedList<PropertySource<?>> propertySourceList = new LinkedList<PropertySource<?>>();

    /**
     * 创建一个新的 {@link MutablePropertySources} 对象。
     */
    public MutablePropertySources() {
    }

    /**
     * 从给定的 propertySources 对象创建一个新的 {@code MutablePropertySources}，
     * 保留所包含的 {@code PropertySource} 对象的原始顺序。
     *
     * @param propertySources 要复制的属性源集合
     */
    public MutablePropertySources(PropertySources propertySources) {
        this();
        // 遍历给定的属性源，按顺序添加到列表末尾
        for (PropertySource<?> propertySource : propertySources) {
            this.addLast(propertySource);
        }
    }

    /**
     * 检查是否包含指定名称的属性源。
     *
     * @param name 要检查的属性源名称
     * @return 如果包含则返回 true，否则返回 false
     */
    public boolean contains(String name) {
        return this.propertySourceList.contains(PropertySource.named(name));
    }

    /**
     * 获取指定名称的属性源。
     *
     * @param name 要获取的属性源名称
     * @return 如果找到则返回对应的属性源，否则返回 null
     */
    public PropertySource<?> get(String name) {
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        return index == -1 ? null : this.propertySourceList.get(index);
    }

    /**
     * 返回属性源列表的迭代器。
     *
     * @return 属性源迭代器
     */
    public Iterator<PropertySource<?>> iterator() {
        return this.propertySourceList.iterator();
    }

    /**
     * 将给定的属性源对象添加到列表开头（具有最高优先级）。
     * 在解析属性时，列表开头的属性源会被首先搜索。
     *
     * @param propertySource 要添加的属性源
     */
    public void addFirst(PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with highest search precedence",
//					propertySource.getName()));
//		}
        // 如果该属性源已存在，先移除
        removeIfPresent(propertySource);
        // 添加到列表开头
        this.propertySourceList.addFirst(propertySource);
    }

    /**
     * 将给定的属性源对象添加到列表末尾（具有最低优先级）。
     * 在解析属性时，列表末尾的属性源会被最后搜索。
     *
     * @param propertySource 要添加的属性源
     */
    public void addLast(PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with lowest search precedence",
//					propertySource.getName()));
//		}
        // 如果该属性源已存在，先移除
        removeIfPresent(propertySource);
        // 添加到列表末尾
        this.propertySourceList.addLast(propertySource);
    }

    /**
     * 将给定的属性源对象添加到指定相对属性源之前（优先级略高于指定的属性源）。
     *
     * @param relativePropertySourceName 相对的属性源名称
     * @param propertySource 要添加的属性源
     */
    public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with search precedence immediately higher than [%s]",
//					propertySource.getName(), relativePropertySourceName));
//		}
        // 检查是否尝试相对于自身添加
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        // 如果该属性源已存在，先移除
        removeIfPresent(propertySource);
        // 获取相对属性源的索引位置
        int index = assertPresentAndGetIndex(relativePropertySourceName);
        // 在指定位置插入属性源
        addAtIndex(index, propertySource);
    }

    /**
     * 将给定的属性源对象添加到指定相对属性源之后（优先级略低于指定的属性源）。
     *
     * @param relativePropertySourceName 相对的属性源名称
     * @param propertySource 要添加的属性源
     */
    public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Adding [%s] PropertySource with search precedence immediately lower than [%s]",
//					propertySource.getName(), relativePropertySourceName));
//		}
        // 检查是否尝试相对于自身添加
        assertLegalRelativeAddition(relativePropertySourceName, propertySource);
        // 如果该属性源已存在，先移除
        removeIfPresent(propertySource);
        // 获取相对属性源的索引位置
        int index = assertPresentAndGetIndex(relativePropertySourceName);
        // 在相对属性源之后的位置插入
        addAtIndex(index + 1, propertySource);
    }

    /**
     * 返回给定属性源的优先级（在列表中的索引位置），如果未找到则返回 {@code -1}。
     * 索引越小，优先级越高。
     *
     * @param propertySource 要查询的属性源
     * @return 属性源的索引位置，如果未找到则返回 -1
     */
    public int precedenceOf(PropertySource<?> propertySource) {
        return this.propertySourceList.indexOf(propertySource);
    }

    /**
     * 移除并返回指定名称的属性源，如果未找到则返回 {@code null}。
     *
     * @param name 要查找并移除的属性源名称
     * @return 被移除的属性源，如果未找到则返回 null
     */
    public PropertySource<?> remove(String name) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Removing [%s] PropertySource", name));
//		}
        // 查找属性源的索引位置
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
        return index == -1 ? null : this.propertySourceList.remove(index);
    }

    /**
     * 用给定的属性源对象替换指定名称的属性源。
     *
     * @param name 要查找并替换的属性源名称
     * @param propertySource 替换的属性源
     * @throws IllegalArgumentException 如果不存在指定名称的属性源
     * @see #contains
     */
    public void replace(String name, PropertySource<?> propertySource) {
//		if (logger.isDebugEnabled()) {
//			logger.debug(String.format("Replacing [%s] PropertySource with [%s]",
//					name, propertySource.getName()));
//		}
        // 确保属性源存在并获取其索引
        int index = assertPresentAndGetIndex(name);
        // 替换指定位置的属性源
        this.propertySourceList.set(index, propertySource);
    }

    /**
     * 返回包含的 {@link PropertySource} 对象数量。
     *
     * @return 属性源的数量
     */
    public int size() {
        return this.propertySourceList.size();
    }

    /**
     * 返回此对象的字符串表示形式，显示所有属性源的名称。
     *
     * @return 包含所有属性源名称的逗号分隔字符串
     */
    @Override
    public String toString() {
        String[] names = new String[this.size()];
        // 收集所有属性源的名称
        for (int i = 0; i < size(); i++) {
            names[i] = this.propertySourceList.get(i).getName();
        }
        return String.format("[%s]", arrayToCommaDelimitedString(names));
    }

    /**
     * 确保给定的属性源不是相对于自身添加的。
     * 如果尝试相对于自身添加属性源，将抛出异常。
     *
     * @param relativePropertySourceName 相对的属性源名称
     * @param propertySource 要添加的属性源
     * @throws IllegalArgumentException 如果尝试相对于自身添加
     */
    protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
//		String newPropertySourceName = propertySource.getName();
//		Assert.isTrue(!relativePropertySourceName.equals(newPropertySourceName),
//				String.format(ILLEGAL_RELATIVE_ADDITION_MESSAGE, newPropertySourceName));
    }

    /**
     * 如果给定的属性源存在，则将其移除。
     * 这个方法用于在添加属性源之前避免重复。
     *
     * @param propertySource 要移除的属性源（如果存在）
     */
    protected void removeIfPresent(PropertySource<?> propertySource) {
		this.propertySourceList.remove(propertySource);
    }

    /**
     * 在列表的特定索引位置添加给定的属性源。
     *
     * @param index 要插入的索引位置
     * @param propertySource 要添加的属性源
     */
    private void addAtIndex(int index, PropertySource<?> propertySource) {
        // 先移除已存在的同名属性源
        removeIfPresent(propertySource);
        // 在指定位置插入属性源
        this.propertySourceList.add(index, propertySource);
    }

    /**
     * 断言指定名称的属性源存在并返回其索引。
     *
     * @param name 要查找的 {@linkplain PropertySource#getName() 属性源名称}
     * @return 属性源在列表中的索引位置
     * @throws IllegalArgumentException 如果指定名称的属性源不存在
     */
    private int assertPresentAndGetIndex(String name) {
        int index = this.propertySourceList.indexOf(PropertySource.named(name));
//		Assert.isTrue(index >= 0, String.format(NON_EXISTENT_PROPERTY_SOURCE_MESSAGE, name));
        return index;
    }

    /**
     * 便捷方法：将字符串数组转换为带分隔符（例如 CSV）的字符串。
     * 例如，对 {@code toString()} 实现很有用。
     *
     * @param arr 要显示的数组
     * @param delim 要使用的分隔符（通常是 ","）
     * @return 带分隔符的字符串
     */
    private static String arrayToDelimitedString(Object[] arr, String delim) {
        if (arr == null || arr.length == 0) {
            return "";
        }
        if (arr.length == 1) {
            return nullSafeToString(arr[0]);
        }
        StringBuilder sb = new StringBuilder();
        // 遍历数组，用分隔符连接每个元素
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                sb.append(delim);
            }
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    /**
     * 返回指定对象的字符串表示形式。
     * <p>
     * 如果是数组，则构建内容的字符串表示。如果 {@code obj} 为 {@code null}，则返回 {@code "null"}。
     *
     * @param obj 要构建字符串表示的对象
     * @return {@code obj} 的字符串表示
     */
    private static String nullSafeToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        if (obj instanceof Object[]) {
            return nullSafeToString((Object[]) obj);
        }
        if (obj instanceof boolean[]) {
            return nullSafeToString((boolean[]) obj);
        }
        if (obj instanceof byte[]) {
            return nullSafeToString((byte[]) obj);
        }
        if (obj instanceof char[]) {
            return nullSafeToString((char[]) obj);
        }
        if (obj instanceof double[]) {
            return nullSafeToString((double[]) obj);
        }
        if (obj instanceof float[]) {
            return nullSafeToString((float[]) obj);
        }
        if (obj instanceof int[]) {
            return nullSafeToString((int[]) obj);
        }
        if (obj instanceof long[]) {
            return nullSafeToString((long[]) obj);
        }
        if (obj instanceof short[]) {
            return nullSafeToString((short[]) obj);
        }
        String str = obj.toString();
        return (str != null ? str : "");
    }

    /**
     * 便捷方法：将字符串数组转换为逗号分隔的 CSV 字符串。
     * 例如，对 {@code toString()} 实现很有用。
     *
     * @param arr 要显示的数组
     * @return 逗号分隔的字符串
     */
    private static String arrayToCommaDelimitedString(Object[] arr) {
        return arrayToDelimitedString(arr, ",");
    }
}
