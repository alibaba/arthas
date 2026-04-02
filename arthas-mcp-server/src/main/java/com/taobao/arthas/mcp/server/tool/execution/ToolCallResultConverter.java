package com.taobao.arthas.mcp.server.tool.execution;

import java.lang.reflect.Type;

/**
 * 将 Tool 调用结果转换为字符串的函数式接口，转换后的字符串将返回给 AI 模型。
 * <p>
 * MCP Tool 的方法可以返回任意类型的 Java 对象（如 String、POJO、图片等），
 * 但 MCP 协议要求最终向 AI 模型返回的内容为字符串（通常是 JSON 格式）。
 * 本接口抽象了"将任意返回值序列化为字符串"这一转换步骤，
 * 允许使用者提供自定义的转换策略。
 * <p>
 * 框架提供了默认实现 {@link DefaultToolCallResultConverter}，
 * 支持处理 {@code void}、{@link String}、图片（{@code RenderedImage}）和普通对象等常见返回类型。
 * <p>
 * 作为函数式接口（{@link FunctionalInterface}），也可以通过 lambda 表达式或方法引用快速提供自定义实现。
 */
@FunctionalInterface
public interface ToolCallResultConverter {

	/**
	 * 将 Tool 方法的返回值转换为可发送给 AI 模型的字符串。
	 * <p>
	 * 实现时可结合 {@code returnType} 参数按类型分支处理，例如：
	 * <ul>
	 *   <li>若 {@code returnType} 为 {@code void}，可返回约定的完成提示字符串</li>
	 *   <li>若 {@code result} 已是合法 JSON 字符串，可直接返回</li>
	 *   <li>对于图片等二进制类型，可编码为 Base64 字符串后返回</li>
	 *   <li>对于 POJO 对象，通常序列化为 JSON 字符串返回</li>
	 * </ul>
	 *
	 * @param result     Tool 方法的返回值，当 {@code returnType} 为 {@code void} 时为 {@code null}
	 * @param returnType Tool 方法声明的返回类型，通过 {@link java.lang.reflect.Method#getGenericReturnType()} 获取
	 * @return 转换后的字符串，将作为 Tool 调用结果发送给 AI 模型
	 */
	String convert(Object result, Type returnType);

}
