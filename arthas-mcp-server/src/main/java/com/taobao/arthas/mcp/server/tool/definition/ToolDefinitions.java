package com.taobao.arthas.mcp.server.tool.definition;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.util.JsonSchemaGenerator;
import com.taobao.arthas.mcp.server.util.Assert;

import java.lang.reflect.Method;

/**
 * MCP工具定义工具类
 *
 * <p>该类提供了一系列静态方法，用于从Java方法对象创建和构建MCP工具定义。
 * 它是注解驱动的工具定义机制的核心组件，能够自动从@Tool注解的方法中提取工具信息。
 *
 * <p>主要功能：
 * <ul>
 *   <li>从Method对象创建ToolDefinition</li>
 *   <li>提取工具名称（优先使用注解定义，否则使用方法名）</li>
 *   <li>提取工具描述（优先使用注解定义，否则使用方法名）</li>
 *   <li>自动生成输入参数的JSON Schema</li>
 *   <li>判断工具是否支持流式处理</li>
 * </ul>
 *
 * @author Yeaury
 */
public class ToolDefinitions {

	/**
	 * 从Method对象创建ToolDefinition的Builder
	 *
	 * <p>该方法分析给定方法的@Tool注解（如果存在），
	 * 自动提取工具名称、描述、输入Schema和流式处理标志，并返回预配置的Builder。
	 *
	 * @param method 要分析的方法对象，必须不为null
	 * @return 预配置的ToolDefinition.Builder实例
	 * @throws IllegalArgumentException 如果method为null
	 */
	public static ToolDefinition.Builder builder(Method method) {
		// 验证参数不为null
		Assert.notNull(method, "method cannot be null");
		// 使用Builder模式构建工具定义，自动提取各种属性
		return ToolDefinition.builder()
			.name(getToolName(method))           // 提取工具名称
			.description(getToolDescription(method))  // 提取工具描述
			.inputSchema(JsonSchemaGenerator.generateForMethodInput(method))  // 生成输入Schema
			.streamable(isStreamable(method));    // 检查是否支持流式处理
	}

	/**
	 * 从Method对象直接创建ToolDefinition对象
	 *
	 * <p>这是一个便捷方法，等同于调用builder(method).build()。
	 *
	 * @param method 要分析的方法对象，必须不为null
	 * @return 构建完成的ToolDefinition对象
	 */
	public static ToolDefinition from(Method method) {
		return builder(method).build();
	}

	/**
	 * 从方法中提取工具名称
	 *
	 * <p>名称提取逻辑：
	 * <ol>
	 *   <li>如果方法有@Tool注解且注解的name属性不为空，使用注解的name</li>
	 *   <li>否则使用方法的实际名称</li>
	 * </ol>
	 *
	 * @param method 要分析的方法对象，必须不为null
	 * @return 工具名称字符串
	 * @throws IllegalArgumentException 如果method为null
	 */
	public static String getToolName(Method method) {
		// 验证参数不为null
		Assert.notNull(method, "method cannot be null");
		// 获取方法上的@Tool注解
		Tool tool = method.getAnnotation(Tool.class);
		// 如果注解存在且name属性不为空，使用注解的name
		if (tool == null) {
			return method.getName();
		}
		return tool.name() != null ? tool.name() : method.getName();
	}

	/**
	 * 从方法中提取工具描述
	 *
	 * <p>描述提取逻辑：
	 * <ol>
	 *   <li>如果方法有@Tool注解且注解的description属性不为空，使用注解的description</li>
	 *   <li>否则使用方法的实际名称作为默认描述</li>
	 * </ol>
	 *
	 * @param method 要分析的方法对象，必须不为null
	 * @return 工具描述字符串
	 * @throws IllegalArgumentException 如果method为null
	 */
	public static String getToolDescription(Method method) {
		// 验证参数不为null
		Assert.notNull(method, "method cannot be null");
		// 获取方法上的@Tool注解
		Tool tool = method.getAnnotation(Tool.class);
		// 如果注解存在且description属性不为空，使用注解的description
		if (tool == null) {
			return method.getName();
		}
		return tool.description() != null ? tool.description() : method.getName();
	}

	/**
	 * 检查方法是否支持流式处理
	 *
	 * <p>流式处理检查逻辑：
	 * <ol>
	 *   <li>如果方法有@Tool注解，返回注解的streamable属性值</li>
	 *   <li>否则返回false（不支持流式处理）</li>
	 * </ol>
	 *
	 * @param method 要分析的方法对象，必须不为null
	 * @return 如果支持流式处理返回true，否则返回false
	 * @throws IllegalArgumentException 如果method为null
	 */
	public static boolean isStreamable(Method method) {
		// 验证参数不为null
		Assert.notNull(method, "method cannot be null");
		// 获取方法上的@Tool注解
		Tool tool = method.getAnnotation(Tool.class);
		// 如果注解不存在，默认不支持流式处理
		if (tool == null) {
			return false;
		}
		// 返回注解的streamable属性值
		return tool.streamable();
	}

}
