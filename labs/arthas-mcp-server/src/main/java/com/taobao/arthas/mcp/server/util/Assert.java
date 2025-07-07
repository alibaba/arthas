package com.taobao.arthas.mcp.server.util;

import java.util.Collection;
import java.util.Map;

/**
 * Assertion utility class for parameter validation.
 */
public final class Assert {

	private Assert() {
	}

	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void hasText(String text, String message) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void notEmpty(Collection<?> collection, String message) {
		if (collection == null || collection.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void notEmpty(Map<?, ?> map, String message) {
		if (map == null || map.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void isTrue(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}

}
