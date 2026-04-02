package com.alibaba.arthas.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 字符串工具类
 *
 * 提供字符串处理相关的工具方法，主要用于处理配置属性中的命名转换。
 * 该类的主要功能是将带连字符的键名转换为驼峰命名格式。
 *
 * @author hengyunabc 2020-06-24
 *
 */
public class StringUtils {

	/**
	 * 移除键名中的连字符并转换为驼峰命名
	 *
	 * 该方法遍历输入的Map，将所有包含连字符（-）的键名转换为驼峰命名格式。
	 * 转换规则：连字符后的字母转换为大写，并移除连字符。
	 * 例如： "http-port" 转换为 "httpPort"
	 *
	 * @param map 原始键值对映射
	 * @return 转换后的键值对映射，键名中的连字符已被移除并转为驼峰格式
	 */
	public static Map<String, String> removeDashKey(Map<String, String> map) {
		// 创建新的HashMap，预分配足够大小的容量
		Map<String, String> result = new HashMap<String, String>(map.size());

		// 遍历原始Map中的所有条目
		for (Entry<String, String> entry : map.entrySet()) {
			// 获取键名
			String key = entry.getKey();

			// 检查键名中是否包含连字符
			if (key.contains("-")) {

				// 创建StringBuilder用于构建转换后的键名
				StringBuilder sb = new StringBuilder(key.length());
				// 遍历键名中的每个字符
				for (int i = 0; i < key.length(); i++) {
					// 如果当前字符是连字符，且下一个字符存在且是字母
					if (key.charAt(i) == '-' && (i + 1 < key.length()) && Character.isAlphabetic(key.charAt(i + 1))) {
						// 跳过连字符，移动到下一个字符
						++i;
						// 将连字符后的字母转为大写（驼峰命名）
						char upperChar = Character.toUpperCase(key.charAt(i));
						sb.append(upperChar);
					} else {
						// 非连字符后的字母，直接添加
						sb.append(key.charAt(i));
					}
				}
				// 使用转换后的键名
				key = sb.toString();
			}

			// 将转换后的键值对放入结果Map
			result.put(key, entry.getValue());
		}

		// 返回转换后的Map
		return result;
	}

}
