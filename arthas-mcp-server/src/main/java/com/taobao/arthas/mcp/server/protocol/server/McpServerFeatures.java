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
 * MCP服务器功能规范类，服务器可以选择支持的功能特性
 * 该实现仅提供异步API接口
 *
 * @author Yeaury
 */
public class McpServerFeatures {

	/**
	 * MCP服务器配置类
	 * 用于定义服务器的基本信息、能力、工具、资源、提示等配置
	 */
	public static class McpServerConfig {
		// 服务器实现信息（名称、版本等）
		private final McpSchema.Implementation serverInfo;
		// 服务器能力声明（日志、提示、资源、工具等）
		private final McpSchema.ServerCapabilities serverCapabilities;
		// 工具规范列表
		private final List<ToolSpecification> tools;
		// 资源规范映射表（key为资源URI）
		private final Map<String, ResourceSpecification> resources;
		// 资源模板列表
		private final List<McpSchema.ResourceTemplate> resourceTemplates;
		// 提示规范映射表（key为提示名称）
		private final Map<String, PromptSpecification> prompts;
		// 根目录变化监听器列表
		private final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers;
		// 服务器使用说明
		private final String instructions;

		/**
		 * 构造MCP服务器配置对象
		 *
		 * @param serverInfo 服务器实现信息
		 * @param serverCapabilities 服务器能力声明
		 * @param tools 工具列表
		 * @param resources 资源映射表
		 * @param resourceTemplates 资源模板列表
		 * @param prompts 提示映射表
		 * @param rootsChangeConsumers 根目录变化监听器列表
		 * @param instructions 服务器使用说明
		 */
		public McpServerConfig(
					McpSchema.Implementation serverInfo,
					McpSchema.ServerCapabilities serverCapabilities,
					List<ToolSpecification> tools,
					Map<String, ResourceSpecification> resources,
					List<McpSchema.ResourceTemplate> resourceTemplates,
					Map<String, PromptSpecification> prompts,
					List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers,
					String instructions) {

				Assert.notNull(serverInfo, "The server information cannot be empty");

				// 如果serverCapabilities为空，则根据提供的功能自动构建相应的能力配置
				if (serverCapabilities == null) {
					serverCapabilities = new McpSchema.ServerCapabilities(
							null, // experimental - 实验性功能
							new McpSchema.ServerCapabilities.LoggingCapabilities(),
							!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
							!Utils.isEmpty(resources) ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
							!Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);
				}

				// 初始化各项配置，如果为null则使用空集合
				this.tools = (tools != null) ? tools : Collections.emptyList();
				this.resources = (resources != null) ? resources : Collections.emptyMap();
				this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : Collections.emptyList();
				this.prompts = (prompts != null) ? prompts : Collections.emptyMap();
				this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : Collections.emptyList();
				this.serverInfo = serverInfo;
				this.serverCapabilities = serverCapabilities;
				this.instructions = instructions;
			}

			/**
			 * 获取服务器实现信息
			 * @return 服务器实现信息对象
			 */
			public McpSchema.Implementation getServerInfo() {
				return serverInfo;
			}

			/**
			 * 获取服务器能力声明
			 * @return 服务器能力声明对象
			 */
			public McpSchema.ServerCapabilities getServerCapabilities() {
				return serverCapabilities;
			}

			/**
			 * 获取工具列表
			 * @return 工具规范列表
			 */
			public List<ToolSpecification> getTools() {
				return tools;
			}

			/**
			 * 获取资源映射表
			 * @return 资源规范映射表
			 */
			public Map<String, ResourceSpecification> getResources() {
				return resources;
			}

			/**
			 * 获取资源模板列表
			 * @return 资源模板列表
			 */
			public List<McpSchema.ResourceTemplate> getResourceTemplates() {
				return resourceTemplates;
			}

			/**
			 * 获取提示映射表
			 * @return 提示规范映射表
			 */
			public Map<String, PromptSpecification> getPrompts() {
				return prompts;
			}

			/**
			 * 获取根目录变化监听器列表
			 * @return 根目录变化监听器列表
			 */
			public List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> getRootsChangeConsumers() {
				return rootsChangeConsumers;
			}

			/**
			 * 获取服务器使用说明
			 * @return 服务器使用说明文本
			 */
			public String getInstructions() {
				return instructions;
			}

			/**
			 * 创建一个新的Builder实例
			 * @return Builder对象
			 */
			public static Builder builder() {
				return new Builder();
			}

			/**
			 * Builder构建器类，用于链式构建McpServerConfig对象
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
				// 提示映射表
				private final Map<String, PromptSpecification> prompts = new HashMap<>();
				// 根目录变化监听器列表
				private final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers = new ArrayList<>();
				// 服务器使用说明
				private String instructions;

				/**
				 * 设置服务器实现信息
				 * @param serverInfo 服务器实现信息
				 * @return 当前Builder对象
				 */
				public Builder serverInfo(McpSchema.Implementation serverInfo) {
					this.serverInfo = serverInfo;
					return this;
				}

				/**
				 * 设置服务器能力声明
				 * @param serverCapabilities 服务器能力声明
				 * @return 当前Builder对象
				 */
				public Builder serverCapabilities(McpSchema.ServerCapabilities serverCapabilities) {
					this.serverCapabilities = serverCapabilities;
					return this;
				}

				/**
				 * 添加工具规范
				 * @param tool 工具规范对象
				 * @return 当前Builder对象
				 */
				public Builder addTool(ToolSpecification tool) {
					this.tools.add(tool);
					return this;
				}

				/**
				 * 添加资源规范
				 * @param key 资源的唯一标识键
				 * @param resource 资源规范对象
				 * @return 当前Builder对象
				 */
				public Builder addResource(String key, ResourceSpecification resource) {
					this.resources.put(key, resource);
					return this;
				}

				/**
				 * 添加资源模板
				 * @param template 资源模板对象
				 * @return 当前Builder对象
				 */
				public Builder addResourceTemplate(McpSchema.ResourceTemplate template) {
					this.resourceTemplates.add(template);
					return this;
				}

				/**
				 * 添加提示规范
				 * @param key 提示的唯一标识键
				 * @param prompt 提示规范对象
				 * @return 当前Builder对象
				 */
				public Builder addPrompt(String key, PromptSpecification prompt) {
					this.prompts.put(key, prompt);
					return this;
				}

				/**
				 * 添加根目录变化监听器
				 * @param consumer 根目录变化时的回调函数
				 * @return 当前Builder对象
				 */
				public Builder addRootsChangeConsumer(
						BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> consumer) {
					this.rootsChangeConsumers.add(consumer);
					return this;
				}

				/**
				 * 设置服务器使用说明
				 * @param instructions 使用说明文本
				 * @return 当前Builder对象
				 */
				public Builder instructions(String instructions) {
					this.instructions = instructions;
					return this;
				}

				/**
				 * 构建McpServerConfig对象
				 * @return 构建好的配置对象
				 */
				public McpServerConfig build() {
					return new McpServerConfig(serverInfo, serverCapabilities, tools, resources, resourceTemplates, prompts,
							rootsChangeConsumers, instructions);
				}
			}
		}

		/**
		 * 工具规范类
		 * 定义了一个工具的元数据和调用处理函数
		 */
		public static class ToolSpecification {
			// MCP工具定义（包含名称、描述、输入参数schema等）
			private final McpSchema.Tool tool;
			// 工具调用函数接口
			private final ToolCallFunction call;

			/**
			 * 构造工具规范对象
			 * @param tool 工具定义对象
			 * @param call 工具调用处理函数
			 */
			public ToolSpecification(
					McpSchema.Tool tool,
					ToolCallFunction call) {
				this.tool = tool;
				this.call = call;
			}

			/**
			 * 获取工具定义
			 * @return 工具定义对象
			 */
			public McpSchema.Tool getTool() {
				return tool;
			}

			/**
			 * 获取工具调用处理函数
			 * @return 工具调用函数
			 */
			public ToolCallFunction getCall() {
				return call;
			}
		}

		/**
		 * 工具调用函数接口
		 * 定义了工具调用的函数签名，接收三个参数并异步返回调用结果
		 */
		@FunctionalInterface
		public interface ToolCallFunction {
			/**
			 * 执行工具调用
			 * @param exchange MCP服务器交换对象，用于与客户端交互
			 * @param commandContext Arthas命令上下文，包含命令执行环境信息
			 * @param arguments 工具调用请求参数对象
			 * @return 异步完成future，包含工具调用结果
			 */
			CompletableFuture<McpSchema.CallToolResult> apply(
					McpNettyServerExchange exchange,
					ArthasCommandContext commandContext,
					McpSchema.CallToolRequest arguments
			);
		}

		/**
		 * 资源规范类
		 * 定义了一个资源的元数据和读取处理函数
		 */
		public static class ResourceSpecification {
			// MCP资源定义（包含URI、名称、描述、MIME类型等）
			private final McpSchema.Resource resource;
			// 资源读取处理函数
			private final BiFunction<McpNettyServerExchange, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> readHandler;

			/**
			 * 构造资源规范对象
			 * @param resource 资源定义对象
			 * @param readHandler 资源读取处理函数
			 */
			public ResourceSpecification(
					McpSchema.Resource resource,
					BiFunction<McpNettyServerExchange, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> readHandler) {
				this.resource = resource;
				this.readHandler = readHandler;
			}

			/**
			 * 获取资源定义
			 * @return 资源定义对象
			 */
			public McpSchema.Resource getResource() {
				return resource;
			}

			/**
			 * 获取资源读取处理函数
			 * @return 资源读取处理函数
			 */
			public BiFunction<McpNettyServerExchange, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> getReadHandler() {
				return readHandler;
			}
		}

		/**
		 * 提示规范类
		 * 定义了一个提示的元数据和获取处理函数
		 */
		public static class PromptSpecification {
			// MCP提示定义（包含名称、描述、参数等）
			private final McpSchema.Prompt prompt;
			// 提示获取处理函数
			private final BiFunction<McpNettyServerExchange, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> promptHandler;

			/**
			 * 构造提示规范对象
			 * @param prompt 提示定义对象
			 * @param promptHandler 提示获取处理函数
			 */
			public PromptSpecification(
					McpSchema.Prompt prompt,
					BiFunction<McpNettyServerExchange, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> promptHandler) {
				this.prompt = prompt;
				this.promptHandler = promptHandler;
			}

			/**
			 * 获取提示定义
			 * @return 提示定义对象
			 */
			public McpSchema.Prompt getPrompt() {
				return prompt;
			}

			/**
			 * 获取提示获取处理函数
			 * @return 提示获取处理函数
			 */
			public BiFunction<McpNettyServerExchange, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> getPromptHandler() {
				return promptHandler;
			}
		}
	}
