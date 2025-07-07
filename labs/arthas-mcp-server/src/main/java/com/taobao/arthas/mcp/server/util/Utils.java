/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.util;

import java.util.Collection;
import java.util.Map;

public final class Utils {

	public static boolean hasText(String str) {
		return str != null && !str.trim().isEmpty();
	}

	public static boolean isEmpty(Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	public static boolean isEmpty(Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	public static boolean isCollectionEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}

	public static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
		if (targetType == null || sourceType == null) {
			return false;
		}

		if (targetType.equals(sourceType)) {
			return true;
		}

		if (targetType.isAssignableFrom(sourceType)) {
			return true;
		}

		if (targetType.isPrimitive()) {
			Class<?> resolvedPrimitive = getPrimitiveClassForWrapper(sourceType);
			return resolvedPrimitive != null && targetType.equals(resolvedPrimitive);
		}
		else if (sourceType.isPrimitive()) {
			Class<?> resolvedWrapper = getWrapperClassForPrimitive(sourceType);
			return resolvedWrapper != null && targetType.equals(resolvedWrapper);
		}

		return false;
	}

	public static Class<?> getPrimitiveClassForWrapper(Class<?> wrapperClass) {
		if (Boolean.class.equals(wrapperClass)) return boolean.class;
		if (Byte.class.equals(wrapperClass)) return byte.class;
		if (Character.class.equals(wrapperClass)) return char.class;
		if (Double.class.equals(wrapperClass)) return double.class;
		if (Float.class.equals(wrapperClass)) return float.class;
		if (Integer.class.equals(wrapperClass)) return int.class;
		if (Long.class.equals(wrapperClass)) return long.class;
		if (Short.class.equals(wrapperClass)) return short.class;
		if (Void.class.equals(wrapperClass)) return void.class;
		return null;
	}

	public static Class<?> getWrapperClassForPrimitive(Class<?> primitiveClass) {
		if (boolean.class.equals(primitiveClass)) return Boolean.class;
		if (byte.class.equals(primitiveClass)) return Byte.class;
		if (char.class.equals(primitiveClass)) return Character.class;
		if (double.class.equals(primitiveClass)) return Double.class;
		if (float.class.equals(primitiveClass)) return Float.class;
		if (int.class.equals(primitiveClass)) return Integer.class;
		if (long.class.equals(primitiveClass)) return Long.class;
		if (short.class.equals(primitiveClass)) return Short.class;
		if (void.class.equals(primitiveClass)) return Void.class;
		return null;
	}

}