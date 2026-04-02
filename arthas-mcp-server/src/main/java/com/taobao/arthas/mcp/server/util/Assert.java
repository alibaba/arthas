package com.taobao.arthas.mcp.server.util;

import java.util.Collection;
import java.util.Map;

/**
 * 参数校验工具类，提供常用的前置条件断言方法。
 * <p>
 * 所有方法在断言失败时均抛出 {@link IllegalArgumentException}，
 * 用于在方法入口处对参数合法性进行快速校验，使调用方能第一时间发现非法参数。
 * <p>
 * 本类为工具类，不可实例化，所有方法均为静态方法。
 */
public final class Assert {

	/**
	 * 私有构造方法，禁止外部实例化。
	 */
	private Assert() {
	}

	/**
	 * 断言对象不为 {@code null}。
	 * <p>
	 * 常用于方法参数的非空校验，例如：
	 * <pre>{@code
	 * Assert.notNull(commandExecutor, "commandExecutor cannot be null");
	 * }</pre>
	 *
	 * @param object  待校验的对象
	 * @param message 校验失败时抛出的异常消息
	 * @throws IllegalArgumentException 若 {@code object} 为 {@code null}
	 */
	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言字符串有实际文本内容（非 {@code null}、非空字符串、非纯空白字符串）。
	 * <p>
	 * 相比 {@link #notNull}，额外过滤了全为空白字符的字符串（如 {@code "   "}），
	 * 适用于需要确保字符串有有效内容的场景（如命令行参数、名称等）。
	 *
	 * @param text    待校验的字符串
	 * @param message 校验失败时抛出的异常消息
	 * @throws IllegalArgumentException 若 {@code text} 为 {@code null}、空字符串或纯空白字符串
	 */
	public static void hasText(String text, String message) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言集合不为 {@code null} 且不为空（至少包含一个元素）。
	 * <p>
	 * 适用于需要确保集合参数包含有效数据的场景，例如：
	 * <pre>{@code
	 * Assert.notEmpty(toolList, "toolList must not be empty");
	 * }</pre>
	 *
	 * @param collection 待校验的集合
	 * @param message    校验失败时抛出的异常消息
	 * @throws IllegalArgumentException 若 {@code collection} 为 {@code null} 或不包含任何元素
	 */
	public static void notEmpty(Collection<?> collection, String message) {
		if (collection == null || collection.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言 Map 不为 {@code null} 且不为空（至少包含一个键值对）。
	 * <p>
	 * 适用于需要确保 Map 参数包含有效条目的场景，例如：
	 * <pre>{@code
	 * Assert.notEmpty(params, "params must not be empty");
	 * }</pre>
	 *
	 * @param map     待校验的 Map
	 * @param message 校验失败时抛出的异常消息
	 * @throws IllegalArgumentException 若 {@code map} 为 {@code null} 或不包含任何键值对
	 */
	public static void notEmpty(Map<?, ?> map, String message) {
		if (map == null || map.isEmpty()) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言布尔条件为 {@code true}。
	 * <p>
	 * 适用于对业务规则或状态的通用校验，例如：
	 * <pre>{@code
	 * Assert.isTrue(timeout > 0, "timeout must be positive");
	 * }</pre>
	 *
	 * @param condition 待校验的布尔条件
	 * @param message   校验失败时抛出的异常消息
	 * @throws IllegalArgumentException 若 {@code condition} 为 {@code false}
	 */
	public static void isTrue(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}

}
