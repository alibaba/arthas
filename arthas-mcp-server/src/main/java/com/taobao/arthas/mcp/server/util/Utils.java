package com.taobao.arthas.mcp.server.util;

import java.util.Collection;
import java.util.Map;

/**
 * 通用工具方法集合，提供字符串、集合、类型判断等常用工具方法。
 * <p>
 * 本类为工具类，不可实例化，所有方法均为静态方法。
 */
public final class Utils {

	/**
	 * 判断字符串是否有实际文本内容（非 {@code null}、非空、非纯空白字符）。
	 * <p>
	 * 与 {@code str != null && !str.isEmpty()} 的区别在于，
	 * 本方法额外过滤了全为空白字符的字符串（如 {@code "   "}）。
	 *
	 * @param str 待检查的字符串，可以为 {@code null}
	 * @return 若字符串非 {@code null} 且去除首尾空白后不为空则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean hasText(String str) {
		return str != null && !str.trim().isEmpty();
	}

	/**
	 * 判断集合是否为空（{@code null} 或不包含任何元素）。
	 *
	 * @param collection 待检查的集合，可以为 {@code null}
	 * @return 若集合为 {@code null} 或不包含任何元素则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * 判断 Map 是否为空（{@code null} 或不包含任何键值对）。
	 *
	 * @param map 待检查的 Map，可以为 {@code null}
	 * @return 若 Map 为 {@code null} 或不包含任何键值对则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * 判断 {@code sourceType} 是否可以赋值给 {@code targetType}（即 source 是 target 的子类型或同类型）。
	 * <p>
	 * 在标准 {@link Class#isAssignableFrom} 的基础上，额外处理了基本类型与包装类之间的互相赋值关系：
	 * <ul>
	 *   <li>若 {@code targetType} 为基本类型（如 {@code int}），{@code sourceType} 为其包装类（如 {@code Integer}），
	 *       则认为可赋值（装箱/拆箱场景）。</li>
	 *   <li>若 {@code sourceType} 为基本类型，{@code targetType} 为其对应包装类，同样认为可赋值。</li>
	 * </ul>
	 * <p>
	 * 若任一参数为 {@code null}，直接返回 {@code false}。
	 *
	 * @param targetType 目标类型（赋值目标），可以为 {@code null}
	 * @param sourceType 源类型（待赋值的值的类型），可以为 {@code null}
	 * @return 若 {@code sourceType} 可赋值给 {@code targetType} 则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean isAssignable(Class<?> targetType, Class<?> sourceType) {
		if (targetType == null || sourceType == null) {
			return false;
		}

		// 完全相同的类型，直接可赋值
		if (targetType.equals(sourceType)) {
			return true;
		}

		// 标准 Java 类型继承/实现关系判断（sourceType 是 targetType 的子类或实现类）
		if (targetType.isAssignableFrom(sourceType)) {
			return true;
		}

		if (targetType.isPrimitive()) {
			// 目标为基本类型（如 int）：检查 sourceType 是否为其对应包装类（如 Integer）
			Class<?> resolvedPrimitive = getPrimitiveClassForWrapper(sourceType);
			return resolvedPrimitive != null && targetType.equals(resolvedPrimitive);
		}
		else if (sourceType.isPrimitive()) {
			// 源为基本类型（如 int）：检查 targetType 是否为其对应包装类（如 Integer）
			Class<?> resolvedWrapper = getWrapperClassForPrimitive(sourceType);
			return resolvedWrapper != null && targetType.equals(resolvedWrapper);
		}

		return false;
	}

	/**
	 * 获取包装类对应的基本类型。
	 * <p>
	 * 支持全部 9 种 Java 基本类型（含 {@code void}）与其包装类的映射：
	 * <ul>
	 *   <li>{@code Boolean}   → {@code boolean}</li>
	 *   <li>{@code Byte}      → {@code byte}</li>
	 *   <li>{@code Character} → {@code char}</li>
	 *   <li>{@code Double}    → {@code double}</li>
	 *   <li>{@code Float}     → {@code float}</li>
	 *   <li>{@code Integer}   → {@code int}</li>
	 *   <li>{@code Long}      → {@code long}</li>
	 *   <li>{@code Short}     → {@code short}</li>
	 *   <li>{@code Void}      → {@code void}</li>
	 * </ul>
	 * 若 {@code wrapperClass} 不是已知的包装类，则返回 {@code null}。
	 *
	 * @param wrapperClass 包装类型
	 * @return 对应的基本类型，若不是已知包装类则返回 {@code null}
	 */
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
		// 非已知包装类，返回 null
		return null;
	}

	/**
	 * 获取基本类型对应的包装类。
	 * <p>
	 * 支持全部 9 种 Java 基本类型（含 {@code void}）与其包装类的映射：
	 * <ul>
	 *   <li>{@code boolean} → {@code Boolean}</li>
	 *   <li>{@code byte}    → {@code Byte}</li>
	 *   <li>{@code char}    → {@code Character}</li>
	 *   <li>{@code double}  → {@code Double}</li>
	 *   <li>{@code float}   → {@code Float}</li>
	 *   <li>{@code int}     → {@code Integer}</li>
	 *   <li>{@code long}    → {@code Long}</li>
	 *   <li>{@code short}   → {@code Short}</li>
	 *   <li>{@code void}    → {@code Void}</li>
	 * </ul>
	 * 若 {@code primitiveClass} 不是已知的基本类型，则返回 {@code null}。
	 * 该方法也被 {@link JsonParser#resolvePrimitiveIfNecessary} 调用，
	 * 用于在序列化/反序列化前将基本类型统一替换为包装类。
	 *
	 * @param primitiveClass 基本类型
	 * @return 对应的包装类，若不是已知基本类型则返回 {@code null}
	 */
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
		// 非已知基本类型，返回 null
		return null;
	}

}
