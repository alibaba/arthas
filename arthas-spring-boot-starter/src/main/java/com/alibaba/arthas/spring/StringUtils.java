package com.alibaba.arthas.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author hengyunabc 2020-06-24
 *
 */
public class StringUtils {

	public static Map<String, String> removeDashKey(Map<String, String> map) {
		Map<String, String> result = new HashMap<String, String>();

		for (Entry<String, String> entry : map.entrySet()) {
			String key = entry.getKey();

			if (key.contains("-")) {

				StringBuilder sb = new StringBuilder(key.length());
				for (int i = 0; i < key.length(); i++) {
					if (key.charAt(i) == '-' && (i + 1 < key.length()) && Character.isAlphabetic(key.charAt(i + 1))) {
						++i;
						char upperChar = Character.toUpperCase(key.charAt(i));
						sb.append(upperChar);
					} else {
						sb.append(key.charAt(i));
					}
				}
				key = sb.toString();
			}

			result.put(key, entry.getValue());
		}

		return result;
	}

}
