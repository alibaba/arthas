package com.taobao.arthas.mcp.server.tool.definition;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

/**
 * MCP工具定义类
 *
 * <p>该类封装了MCP工具的完整定义信息，用于描述服务器提供的工具。
 * 每个工具都有一个名称、描述、输入模式定义，以及是否支持流式处理的标志。
 *
 * <p>主要功能：
 * <ul>
 *   <li>定义工具的基本元信息（名称、描述）</li>
 *   <li>定义工具的输入参数结构（JSON Schema）</li>
 *   <li>声明工具是否支持流式处理</li>
 *   <li>通过Builder模式方便地构建工具定义</li>
 * </ul>
 *
 * @author Yeaury
 */
public class ToolDefinition {
	// 工具名称，用于唯一标识工具
	private String name;

	// 工具描述，说明工具的功能和用途
	private String description;

	// 工具输入参数的JSON Schema定义，描述参数的类型、结构等
	private McpSchema.JsonSchema inputSchema;

	// 是否支持流式处理，true表示工具可以返回流式结果
	private boolean streamable;

	/**
	 * 构造一个新的工具定义对象
	 *
	 * @param name        工具名称，必须唯一标识该工具
	 * @param description 工具描述，说明工具的功能
	 * @param inputSchema 输入参数的JSON Schema定义
	 * @param streamable  是否支持流式处理
	 */
	public ToolDefinition(String name, String description,
						  McpSchema.JsonSchema inputSchema, boolean streamable) {
		this.name = name;
		this.description = description;
		this.inputSchema = inputSchema;
		this.streamable = streamable;
	}

	/**
	 * 获取工具名称
	 *
	 * @return 工具名称字符串
	 */
	public String getName() {
		return name;
	}

	/**
	 * 获取工具描述
	 *
	 * @return 工具描述字符串
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 获取工具输入参数的JSON Schema定义
	 *
	 * @return JSON Schema对象，描述输入参数的结构
	 */
	public McpSchema.JsonSchema getInputSchema() {
		return inputSchema;
	}

	/**
	 * 判断工具是否支持流式处理
	 *
	 * @return 如果支持流式处理返回true，否则返回false
	 */
	public boolean isStreamable() {
		return streamable;
	}

	/**
	 * 创建一个新的Builder实例
	 *
	 * <p>使用Builder模式可以方便地构建ToolDefinition对象，
	 * 支持链式调用，使代码更加清晰易读。
	 *
	 * @return Builder实例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * ToolDefinition的构建器类
	 *
	 * <p>该类使用Builder模式，提供了一种灵活的方式来构建ToolDefinition对象。
	 * 支持链式调用，每个设置方法都返回Builder实例本身。
	 *
	 * <p>使用示例：
	 * <pre>
	 * ToolDefinition definition = ToolDefinition.builder()
	 *     .name("my_tool")
	 *     .description("My custom tool")
	 *     .inputSchema(schema)
	 *     .streamable(true)
	 *     .build();
	 * </pre>
	 */
	public static final class Builder {
		// 工具名称
		private String name;

		// 工具描述
		private String description;

		// 输入参数的JSON Schema定义
		private McpSchema.JsonSchema inputSchema;

		// 是否支持流式处理
		private boolean streamable;

		/**
		 * 私有构造函数，防止外部直接实例化
		 * 只能通过 ToolDefinition.builder() 方法获取Builder实例
		 */
		private Builder() {
		}

		/**
		 * 设置工具名称
		 *
		 * @param name 工具名称
		 * @return Builder实例，支持链式调用
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * 设置工具描述
		 *
		 * @param description 工具描述
		 * @return Builder实例，支持链式调用
		 */
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/**
		 * 设置输入参数的JSON Schema定义
		 *
		 * @param inputSchema JSON Schema对象
		 * @return Builder实例，支持链式调用
		 */
		public Builder inputSchema(McpSchema.JsonSchema inputSchema) {
			this.inputSchema = inputSchema;
			return this;
		}

		/**
		 * 设置是否支持流式处理
		 *
		 * @param streamable 是否支持流式处理
		 * @return Builder实例，支持链式调用
		 */
		public Builder streamable(boolean streamable) {
			this.streamable = streamable;
			return this;
		}

		/**
		 * 构建ToolDefinition对象
		 *
		 * <p>根据之前设置的属性，创建并返回一个新的ToolDefinition实例。
		 * 必须先设置所有必需的属性才能调用此方法。
		 *
		 * @return 新构建的ToolDefinition对象
		 */
		public ToolDefinition build() {
			return new ToolDefinition(this.name, this.description, this.inputSchema, this.streamable);
		}

	}

}
