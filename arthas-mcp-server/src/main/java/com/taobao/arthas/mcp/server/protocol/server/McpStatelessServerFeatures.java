/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.Utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * MCP无状态服务器功能规范
 *
 * 该类定义了MCP服务器的功能规范，服务器可以选择支持的功能特性。
 * 此实现仅提供异步API，用于处理MCP协议的各种请求。
 *
 * @author Yeaury
 */
public class McpStatelessServerFeatures {

	/**
	 * MCP服务器配置类
	 *
	 * 该类封装了MCP服务器的所有配置信息，包括：
	 * - 服务器基本信息
	 * - 服务器能力声明
	 * - 支持的工具列表
	 * - 支持的资源列表
	 * - 资源模板列表
	 * - 提示词列表
	 * - 使用说明
	 */
	public static class McpServerConfig {
		// 服务器实现信息，包含名称、版本等基本信息
		private final McpSchema.Implementation serverInfo;
		// 服务器能力声明，定义服务器支持哪些MCP协议功能
		private final McpSchema.ServerCapabilities serverCapabilities;
		// 支持的工具规格列表
		private final List<ToolSpecification> tools;
		// 支持的资源规格映射表，key为资源URI
		private final Map<String, ResourceSpecification> resources;
		// 资源模板列表，用于匹配动态资源
		private final List<McpSchema.ResourceTemplate> resourceTemplates;
		// 提示词规格映射表，key为提示词名称
		private final Map<String, PromptSpecification> prompts;
		// 服务器使用说明文本
		private final String instructions;

		/**
		 * 构造MCP服务器配置对象
		 *
		 * @param serverInfo 服务器实现信息，不能为空
		 * @param serverCapabilities 服务器能力声明，如果为空则根据提供的功能自动构建
		 * @param tools 支持的工具列表
		 * @param resources 支持的资源映射表
		 * @param resourceTemplates 资源模板列表
		 * @param prompts 提示词映射表
		 * @param instructions 服务器使用说明
		 */
		public McpServerConfig(
					McpSchema.Implementation serverInfo,
					McpSchema.ServerCapabilities serverCapabilities,
					List<ToolSpecification> tools,
					Map<String, ResourceSpecification> resources,
					List<McpSchema.ResourceTemplate> resourceTemplates,
					Map<String, PromptSpecification> prompts,
					String instructions) {

			// 校验服务器信息不能为空
			Assert.notNull(serverInfo, "The server information cannot be empty");

			// 如果serverCapabilities为空，则根据提供的功能自动构建相应的能力配置
			if (serverCapabilities == null) {
				serverCapabilities = new McpSchema.ServerCapabilities(
						null, // experimental - 实验性功能
						new McpSchema.ServerCapabilities.LoggingCapabilities(), // 日志能力
						!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null, // 提示词能力
						!Utils.isEmpty(resources) ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null, // 资源能力（订阅和列表）
						!Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null); // 工具能力
			}

			// 初始化所有配置字段，如果传入null则使用空集合
			this.tools = (tools != null) ? tools : Collections.emptyList();
			this.resources = (resources != null) ? resources : Collections.emptyMap();
			this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : Collections.emptyList();
			this.prompts = (prompts != null) ? prompts : Collections.emptyMap();
			this.serverInfo = serverInfo;
			this.serverCapabilities = serverCapabilities;
			this.instructions = instructions;
		}

		/**
		 * 获取服务器实现信息
		 *
		 * @return 服务器实现信息对象
		 */
		public McpSchema.Implementation getServerInfo() {
			return serverInfo;
		}

		/**
		 * 获取服务器能力声明
		 *
		 * @return 服务器能力声明对象
		 */
		public McpSchema.ServerCapabilities getServerCapabilities() {
			return serverCapabilities;
		}

		/**
		 * 获取支持的工具列表
		 *
		 * @return 工具规格列表
		 */
		public List<ToolSpecification> getTools() {
			return tools;
		}

		/**
		 * 获取支持的资源映射表
		 *
		 * @return 资源规格映射表，key为资源URI
		 */
		public Map<String, ResourceSpecification> getResources() {
			return resources;
		}

		/**
		 * 获取资源模板列表
		 *
		 * @return 资源模板列表
		 */
		public List<McpSchema.ResourceTemplate> getResourceTemplates() {
			return resourceTemplates;
		}

		/**
		 * 获取提示词映射表
		 *
		 * @return 提示词规格映射表，key为提示词名称
		 */
		public Map<String, PromptSpecification> getPrompts() {
			return prompts;
		}

		/**
		 * 获取服务器使用说明
		 *
		 * @return 使用说明文本
		 */
		public String getInstructions() {
			return instructions;
		}

		/**
		 * 创建Builder实例
		 *
		 * @return Builder对象
		 */
		public static Builder builder() {
			return new Builder();
		}

		/**
		 * MCP服务器配置构建器
		 *
		 * 提供流式API来构建McpServerConfig对象
		 */
		public static class Builder {
			// 服务器实现信息
			private McpSchema.Implementation serverInfo;
			// 服务器能力声明
			private McpSchema.ServerCapabilities serverCapabilities;
			// 工具列表
			private final List<ToolSpecification> tools = new ArrayList<>();
			// 资源映射表
			private final Map<String, ResourceSpecification> resources = new HashMap<>();
			// 资源模板列表
			private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();
			// 提示词映射表
			private final Map<String, PromptSpecification> prompts = new HashMap<>();
			// 使用说明文本
			private String instructions;

			/**
			 * 设置服务器实现信息
			 *
			 * @param serverInfo 服务器实现信息
			 * @return 当前Builder对象
			 */
			public Builder serverInfo(McpSchema.Implementation serverInfo) {
				this.serverInfo = serverInfo;
				return this;
			}

			/**
			 * 设置服务器能力声明
			 *
			 * @param serverCapabilities 服务器能力声明
			 * @return 当前Builder对象
			 */
			public Builder serverCapabilities(McpSchema.ServerCapabilities serverCapabilities) {
				this.serverCapabilities = serverCapabilities;
				return this;
			}

			/**
			 * 添加工具规格
			 *
			 * @param tool 工具规格对象
			 * @return 当前Builder对象
			 */
			public Builder addTool(ToolSpecification tool) {
				this.tools.add(tool);
				return this;
			}

			/**
			 * 添加资源规格
			 *
			 * @param key 资源URI作为key
			 * @param resource 资源规格对象
			 * @return 当前Builder对象
			 */
			public Builder addResource(String key, ResourceSpecification resource) {
				this.resources.put(key, resource);
				return this;
			}

			/**
			 * 添加资源模板
			 *
			 * @param template 资源模板对象
			 * @return 当前Builder对象
			 */
			public Builder addResourceTemplate(McpSchema.ResourceTemplate template) {
				this.resourceTemplates.add(template);
				return this;
			}

			/**
			 * 添加提示词规格
			 *
			 * @param key 提示词名称作为key
			 * @param prompt 提示词规格对象
			 * @return 当前Builder对象
			 */
			public Builder addPrompt(String key, PromptSpecification prompt) {
				this.prompts.put(key, prompt);
				return this;
			}

			/**
			 * 设置使用说明文本
			 *
			 * @param instructions 使用说明文本
			 * @return 当前Builder对象
			 */
			public Builder instructions(String instructions) {
				this.instructions = instructions;
				return this;
			}

			/**
			 * 构建McpServerConfig对象
			 *
			 * @return McpServerConfig对象
			 */
			public McpServerConfig build() {
				return new McpServerConfig(serverInfo, serverCapabilities, tools, resources, resourceTemplates, prompts,
						instructions);
			}
		}
	}

	/**
	 * 工具规格类
	 *
	 * 封装了工具的定义和调用函数
	 */
	public static class ToolSpecification {
		// 工具定义对象
		private final McpSchema.Tool tool;
		// 工具调用函数
		private final ToolCallFunction call;

		/**
		 * 构造工具规格对象
		 *
		 * @param tool 工具定义对象
		 * @param call 工具调用函数
		 */
		public ToolSpecification(
					McpSchema.Tool tool,
					ToolCallFunction call) {
			this.tool = tool;
			this.call = call;
		}

		/**
		 * 获取工具定义对象
		 *
		 * @return 工具定义对象
		 */
		public McpSchema.Tool getTool() {
			return tool;
		}

		/**
		 * 获取工具调用函数
		 *
		 * @return 工具调用函数
		 */
		public ToolCallFunction getCall() {
			return call;
		}
	}

	/**
	 * 工具调用函数接口
	 *
	 * 定义了工具调用的函数式接口，包含三个参数：
	 * - 传输上下文
	 * - Arthas命令上下文
	 * - 参数映射表
	 */
	@FunctionalInterface
	public interface ToolCallFunction {
		/**
		 * 执行工具调用
		 *
		 * @param context MCP传输上下文，包含传输层元数据
		 * @param commandContext Arthas命令上下文
		 * @param arguments 工具调用参数映射表
		 * @return 异步结果，包含工具调用的返回值
		 */
		CompletableFuture<McpSchema.CallToolResult> apply(
					McpTransportContext context,
					ArthasCommandContext commandContext,
					Map<String, Object> arguments
			);
	}

	/**
	 * 资源规格类
	 *
	 * 封装了资源的定义和读取处理函数
	 */
	public static class ResourceSpecification {
		// 资源定义对象
		private final McpSchema.Resource resource;
		// 资源读取处理函数
		private final BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> readHandler;

		/**
		 * 构造资源规格对象
		 *
		 * @param resource 资源定义对象
		 * @param readHandler 资源读取处理函数
		 */
		public ResourceSpecification(
					McpSchema.Resource resource,
					BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> readHandler) {
			this.resource = resource;
			this.readHandler = readHandler;
		}

		/**
		 * 获取资源定义对象
		 *
		 * @return 资源定义对象
		 */
		public McpSchema.Resource getResource() {
			return resource;
		}

		/**
		 * 获取资源读取处理函数
		 *
		 * @return 资源读取处理函数
		 */
		public BiFunction<McpTransportContext, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> getReadHandler() {
			return readHandler;
		}
	}

	/**
	 * 提示词规格类
	 *
	 * 封装了提示词的定义和获取处理函数
	 */
	public static class PromptSpecification {
		// 提示词定义对象
		private final McpSchema.Prompt prompt;
		// 提示词获取处理函数
		private final BiFunction<McpTransportContext, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> promptHandler;

		/**
		 * 构造提示词规格对象
		 *
		 * @param prompt 提示词定义对象
		 * @param promptHandler 提示词获取处理函数
		 */
		public PromptSpecification(
					McpSchema.Prompt prompt,
					BiFunction<McpTransportContext, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> promptHandler) {
			this.prompt = prompt;
			this.promptHandler = promptHandler;
		}

		/**
		 * 获取提示词定义对象
		 *
		 * @return 提示词定义对象
		 */
		public McpSchema.Prompt getPrompt() {
			return prompt;
		}

		/**
		 * 获取提示词获取处理函数
		 *
		 * @return 提示词获取处理函数
		 */
		public BiFunction<McpTransportContext, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> getPromptHandler() {
			return promptHandler;
		}
	}
}
