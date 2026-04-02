/*
 * Copyright 2002-2011 the original author or authors.
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

package com.taobao.arthas.core.env.convert;

import java.util.HashSet;
import java.util.Set;

/**
 * 字符串到布尔值的转换器
 * <p>
 * 该转换器负责将各种常见格式的字符串转换为布尔值。
 * 支持多种表示"真"和"假"的字符串形式，不区分大小写。
 * </p>
 * <p>
 * 表示"真"的值包括：
 * <ul>
 * <li>"true" - 标准布尔值</li>
 * <li>"on" - 开启状态</li>
 * <li>"yes" - 肯定回答</li>
 * <li>"1" - 数字1</li>
 * </ul>
 * </p>
 * <p>
 * 表示"假"的值包括：
 * <ul>
 * <li>"false" - 标准布尔值</li>
 * <li>"off" - 关闭状态</li>
 * <li>"no" - 否定回答</li>
 * <li>"0" - 数字0</li>
 * </ul>
 * </p>
 * <p>
 * 特殊处理：空字符串（仅包含空白字符）会返回null。
 * </p>
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class StringToBooleanConverter implements Converter<String, Boolean> {

	/**
	 * 表示"真"的字符串集合
	 * <p>
	 * 使用HashSet存储，确保O(1)时间复杂度的查找性能。
	 * 初始容量设置为4，正好容纳4个预定义的值。
	 * </p>
	 * <p>
	 * 包含的值：true, on, yes, 1（全部小写）
	 * </p>
	 */
	private static final Set<String> trueValues = new HashSet<String>(4);

	/**
	 * 表示"假"的字符串集合
	 * <p>
	 * 使用HashSet存储，确保O(1)时间复杂度的查找性能。
	 * 初始容量设置为4，正好容纳4个预定义的值。
	 * </p>
	 * <p>
	 * 包含的值：false, off, no, 0（全部小写）
	 * </p>
	 */
	private static final Set<String> falseValues = new HashSet<String>(4);

	/**
	 * 静态初始化块
	 * <p>
	 * 在类加载时初始化trueValues和falseValues集合。
	 * 这些值都是小写形式，实际转换时会将输入转为小写进行比较。
	 * </p>
	 */
	static {
		// 初始化表示"真"的值
		trueValues.add("true");  // 标准布尔真值
		trueValues.add("on");    // 开启状态
		trueValues.add("yes");   // 肯定回答
		trueValues.add("1");     // 数字1表示真

		// 初始化表示"假"的值
		falseValues.add("false"); // 标准布尔假值
		falseValues.add("off");   // 关闭状态
		falseValues.add("no");    // 否定回答
		falseValues.add("0");     // 数字0表示假
	}

	/**
	 * 将字符串转换为布尔值
	 * <p>
	 * 转换规则：
	 * <ol>
	 * <li>首先去除字符串两端的空白字符</li>
	 * <li>如果结果为空字符串，返回null</li>
	 * <li>将字符串转换为小写</li>
	 * <li>检查是否在trueValues集合中，是则返回Boolean.TRUE</li>
	 * <li>检查是否在falseValues集合中，是则返回Boolean.FALSE</li>
	 * <li>如果都不在，抛出IllegalArgumentException异常</li>
	 * </ol>
	 * </p>
	 * <p>
	 * 示例：
	 * <pre>
	 * "true"  -> Boolean.TRUE
	 * "YES"   -> Boolean.TRUE (不区分大小写)
	 * "  On " -> Boolean.TRUE (自动去除空白)
	 * "1"     -> Boolean.TRUE
	 * ""      -> null
	 * "false" -> Boolean.FALSE
	 * "no"    -> Boolean.FALSE
	 * "maybe" -> 抛出IllegalArgumentException
	 * </pre>
	 * </p>
	 *
	 * @param source 要转换的源字符串，可以为null或空
	 * @param targetType 目标类型，应该是Boolean.class或boolean.class（未使用，保留用于接口一致性）
	 * @return 对应的布尔值对象，如果源字符串为空则返回null
	 * @throws IllegalArgumentException 当字符串不能识别为有效的布尔值时抛出
	 */
	public Boolean convert(String source, Class<Boolean> targetType) {
		// 步骤1: 去除字符串两端的空白字符
		// 例如: " true " -> "true"
		String value = source.trim();

		// 步骤2: 检查是否为空字符串，如果是则返回null
		// 这样处理可以优雅地处理空白字符串的情况
		if ("".equals(value)) {
			return null;
		}

		// 步骤3: 转换为小写，实现不区分大小写的匹配
		// 例如: "TRUE" -> "true", "Yes" -> "yes"
		value = value.toLowerCase();

		// 步骤4: 检查是否在"真"值集合中
		if (trueValues.contains(value)) {
			return Boolean.TRUE;  // 返回Boolean.TRUE常量（比new Boolean(true)更高效）
		}
		// 步骤5: 检查是否在"假"值集合中
		else if (falseValues.contains(value)) {
			return Boolean.FALSE; // 返回Boolean.FALSE常量
		}
		// 步骤6: 无法识别的值，抛出异常
		else {
			throw new IllegalArgumentException("Invalid boolean value '" + source + "'");
		}
	}

}
