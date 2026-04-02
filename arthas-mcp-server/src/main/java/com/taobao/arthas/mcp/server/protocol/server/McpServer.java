/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.util.JsonParser;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpStatelessServerTransport;
import com.taobao.arthas.mcp.server.protocol.spec.McpStreamableServerTransportProvider;
import com.taobao.arthas.mcp.server.util.Assert;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * MCP 服务器接口和基于 Netty 的实现构建器
 *
 * <p>
 * 此接口提供了两种 MCP 服务器的构建方式：
 * <ul>
 * <li>流式服务器（Streamable）：基于持久连接的服务器，支持双向通信
 * <li>无状态服务器（Stateless）：基于 HTTP 等无状态协议的服务器
 * </ul>
 *
 * <p>
 * 使用构建器模式配置和创建 MCP 服务器实例
 *
 * @author Yeaury
 */
public interface McpServer {

	/**
	 * 默认服务器信息
	 * 包含服务器名称 "mcp-server" 和版本 "1.0.0"
	 */
	McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server", "1.0.0");

	/**
	 * 创建一个基于流式传输的 Netty 服务器构建器
	 * 流式服务器使用持久连接，支持服务器主动推送消息给客户端
	 *
	 * @param transportProvider 流式服务器传输提供者，负责建立和管理传输连接
	 * @return 流式服务器规范对象，用于配置服务器
	 */
	static StreamableServerNettySpecification netty(McpStreamableServerTransportProvider transportProvider) {
		return new StreamableServerNettySpecification(transportProvider);
	}

	/**
	 * 创建一个基于无状态传输的 Netty 服务器构建器
	 * 无状态服务器基于 HTTP 等协议，每个请求都是独立的
	 *
	 * @param transport 无状态服务器传输对象
	 * @return 无状态服务器规范对象，用于配置服务器
	 */
	static StatelessServerNettySpecification netty(McpStatelessServerTransport transport) {
		return new StatelessServerNettySpecification(transport);
	}

	/**
	 * 流式服务器 Netty 规范类
	 * 用于配置和构建基于流式传输的 MCP Netty 服务器
	 *
	 * <p>
	 * 使用构建器模式，支持链式调用配置服务器的各个方面：
	 * <ul>
	 * <li>服务器信息（名称、版本）
	 * <li>服务器能力（工具、资源、提示等）
	 * <li>请求超时时间
	 * <li>自定义对象映射器
	 * <li>命令执行器
	 * </ul>
	 */
	class StreamableServerNettySpecification {

		/**
		 * JSON 对象映射器，用于序列化和反序列化 MCP 消息
		 * 如果未设置，将使用默认的 JsonParser.getObjectMapper()
		 */
		ObjectMapper objectMapper;

		/**
		 * 服务器实现信息，包含服务器名称和版本
		 * 默认值为 DEFAULT_SERVER_INFO
		 */
		McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

		/**
		 * 服务器能力描述，定义服务器支持的功能
		 * 包括工具、资源、提示、日志等能力
		 */
		McpSchema.ServerCapabilities serverCapabilities;

		/**
		 * 服务器使用说明
		 * 用于向客户端描述服务器的用途和使用方法
		 */
		String instructions;

		/**
		 * 命令执行器
		 * 用于执行 Arthas 命令，必须在构建前设置
		 */
		CommandExecutor commandExecutor;

		/**
		 * 流式服务器传输提供者
		 * 负责建立和管理与客户端的传输连接
		 */
		private final McpStreamableServerTransportProvider transportProvider;

		/**
		 * 已注册的工具列表
		 * 每个 ToolSpecification 包含工具定义和处理函数
		 */
		final List<McpServerFeatures.ToolSpecification> tools = new ArrayList<>();

		/**
		 * 已注册的资源映射表
		 * 键为资源 URI，值为资源规范对象
		 */
		final Map<String, McpServerFeatures.ResourceSpecification> resources = new HashMap<>();

		/**
		 * 已注册的资源模板列表
		 * 资源模板用于匹配动态的 URI 模式
		 */
		final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();

		/**
		 * 已注册的提示映射表
		 * 键为提示名称，值为提示规范对象
		 */
		final Map<String, McpServerFeatures.PromptSpecification> prompts = new HashMap<>();

		/**
		 * 根目录变更处理器列表
		 * 当客户端的根目录发生变化时，会调用这些处理器
		 */
		final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeHandlers = new ArrayList<>();

		/**
		 * 请求超时时间
		 * 默认为 10 秒
		 */
		Duration requestTimeout = Duration.ofSeconds(10); // Default timeout

		/**
		 * 构造函数，创建流式服务器规范对象
		 *
		 * @param transportProvider 流式服务器传输提供者
		 */
		public StreamableServerNettySpecification(McpStreamableServerTransportProvider transportProvider) {
			this.transportProvider = transportProvider;
		}

		/**
		 * 设置服务器实现信息
		 *
		 * @param serverInfo 服务器实现信息，包含名称和版本
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification serverInfo(McpSchema.Implementation serverInfo) {
			Assert.notNull(serverInfo, "Server info must not be null");
			this.serverInfo = serverInfo;
			return this;
		}

		/**
		 * 设置请求超时时间
		 *
		 * @param requestTimeout 请求超时时间
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification requestTimeout(Duration requestTimeout) {
			Assert.notNull(requestTimeout, "Request timeout must not be null");
			this.requestTimeout = requestTimeout;
			return this;
		}

		/**
		 * 使用名称和版本设置服务器实现信息
		 *
		 * @param name 服务器名称，不能为空
		 * @param version 服务器版本，不能为空
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification serverInfo(String name, String version) {
			Assert.hasText(name, "Name must not be null or empty");
			Assert.hasText(version, "Version must not be null or empty");
			this.serverInfo = new McpSchema.Implementation(name, version);
			return this;
		}

		/**
		 * 设置服务器使用说明
		 *
		 * @param instructions 服务器使用说明文本
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		/**
		 * 设置服务器能力
		 *
		 * @param serverCapabilities 服务器能力描述对象
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
			this.serverCapabilities = serverCapabilities;
			return this;
		}

		/**
		 * 注册单个工具及其处理函数
		 *
		 * @param tool 工具定义，包含工具的名称、描述、参数模式等
		 * @param handler 工具调用处理函数，当客户端调用此工具时执行
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification tool(McpSchema.Tool tool,
										   McpServerFeatures.ToolCallFunction handler) {
			Assert.notNull(tool, "Tool must not be null");
			Assert.notNull(handler, "Handler must not be null");

			this.tools.add(new McpServerFeatures.ToolSpecification(tool, handler));

			return this;
		}

		/**
		 * 批量注册工具
		 *
		 * @param toolRegistrations 工具规范列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification tools(List<McpServerFeatures.ToolSpecification> toolRegistrations) {
			Assert.notNull(toolRegistrations, "Tool handlers list must not be null");
			this.tools.addAll(toolRegistrations);
			return this;
		}

		/**
		 * 使用可变参数批量注册工具
		 *
		 * @param toolRegistrations 工具规范数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification tools(McpServerFeatures.ToolSpecification... toolRegistrations) {
			this.tools.addAll(Arrays.asList(toolRegistrations));
			return this;
		}

		/**
		 * 使用映射表批量注册资源
		 *
		 * @param resourceSpecifications 资源规范映射表，键为资源 URI
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification resources(Map<String, McpServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
			this.resources.putAll(resourceSpecifications);
			return this;
		}

		/**
		 * 使用列表批量注册资源
		 *
		 * @param resourceSpecifications 资源规范列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification resources(List<McpServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		/**
		 * 使用可变参数批量注册资源
		 *
		 * @param resourceSpecifications 资源规范数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification resources(McpServerFeatures.ResourceSpecification... resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		/**
		 * 使用列表批量注册资源模板
		 *
		 * @param resourceTemplates 资源模板列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(resourceTemplates);
			return this;
		}

		/**
		 * 使用可变参数批量注册资源模板
		 *
		 * @param resourceTemplates 资源模板数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(Arrays.asList(resourceTemplates));
			return this;
		}

		/**
		 * 使用映射表批量注册提示
		 *
		 * @param prompts 提示规范映射表，键为提示名称
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification prompts(Map<String, McpServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			this.prompts.putAll(prompts);
			return this;
		}

		/**
		 * 使用列表批量注册提示
		 *
		 * @param prompts 提示规范列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification prompts(List<McpServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		/**
		 * 使用可变参数批量注册提示
		 *
		 * @param prompts 提示规范数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification prompts(McpServerFeatures.PromptSpecification... prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		/**
		 * 注册单个根目录变更处理器
		 * 当客户端的根目录发生变化时，会调用此处理器
		 *
		 * @param handler 根目录变更处理函数
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification rootsChangeHandler(
					BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> handler) {
			Assert.notNull(handler, "Consumer must not be null");
			this.rootsChangeHandlers.add(handler);
			return this;
		}


		/**
		 * 使用列表批量注册根目录变更处理器
		 *
		 * @param handlers 根目录变更处理函数列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification rootsChangeHandlers(
					List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			this.rootsChangeHandlers.addAll(handlers);
			return this;
		}

		/**
		 * 使用可变参数批量注册根目录变更处理器
		 *
		 * @param handlers 根目录变更处理函数数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification rootsChangeHandlers(
					@SuppressWarnings("unchecked") BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>... handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			return this.rootsChangeHandlers(Arrays.asList(handlers));
		}

		/**
		 * 设置自定义的 JSON 对象映射器
		 *
		 * @param objectMapper JSON 对象映射器
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		/**
		 * 设置命令执行器
		 * 命令执行器必须在构建服务器之前设置
		 *
		 * @param commandExecutor 命令执行器
		 * @return 当前规范对象，支持链式调用
		 */
		public StreamableServerNettySpecification commandExecutor(CommandExecutor commandExecutor) {
			Assert.notNull(commandExecutor, "CommandExecutor must not be null");
			this.commandExecutor = commandExecutor;
			return this;
		}

		/**
		 * 构建并返回 MCP Netty 服务器实例
		 * 此方法会验证必需的配置，并创建服务器实例
		 *
		 * @return 已配置的 MCP Netty 服务器实例
		 * @throws IllegalArgumentException 如果 CommandExecutor 未设置
		 */
		public McpNettyServer build() {
			// 使用自定义的 ObjectMapper 或默认的 ObjectMapper
			ObjectMapper mapper = this.objectMapper != null ? this.objectMapper : JsonParser.getObjectMapper();
			// 验证命令执行器必须设置
			Assert.notNull(this.commandExecutor, "CommandExecutor must be set before building");
			// 创建并返回服务器实例
			return new McpNettyServer(
						this.transportProvider, mapper, this.requestTimeout,
						new McpServerFeatures.McpServerConfig(this.serverInfo, this.serverCapabilities, this.tools,
								this.resources, this.resourceTemplates, this.prompts, this.rootsChangeHandlers, this.instructions
						), this.commandExecutor
				);
		}
	}

	/**
	 * 无状态服务器 Netty 规范类
	 * 用于配置和构建基于无状态传输的 MCP Netty 服务器
	 *
	 * <p>
	 * 使用构建器模式，支持链式调用配置服务器的各个方面
	 * 与流式服务器类似，但使用无状态传输协议（如 HTTP）
	 */
	class StatelessServerNettySpecification {

		/**
		 * 无状态服务器传输对象
		 * 负责处理无状态协议的通信
		 */
		private final McpStatelessServerTransport transport;

		/**
		 * JSON 对象映射器，用于序列化和反序列化 MCP 消息
		 * 如果未设置，将使用默认的 JsonParser.getObjectMapper()
		 */
		ObjectMapper objectMapper;

		/**
		 * 服务器实现信息，包含服务器名称和版本
		 * 默认值为 DEFAULT_SERVER_INFO
		 */
		McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

		/**
		 * 服务器能力描述，定义服务器支持的功能
		 */
		McpSchema.ServerCapabilities serverCapabilities;

		/**
		 * 服务器使用说明
		 */
		String instructions;

		/**
		 * 命令执行器
		 * 用于执行 Arthas 命令，必须在构建前设置
		 */
		CommandExecutor commandExecutor;

		/**
		 * 已注册的工具列表
		 * 使用无状态服务器特定的工具规范
		 */
		final List<McpStatelessServerFeatures.ToolSpecification> tools = new ArrayList<>();

		/**
		 * 已注册的资源映射表
		 * 使用无状态服务器特定的资源规范
		 */
		final Map<String, McpStatelessServerFeatures.ResourceSpecification> resources = new HashMap<>();

		/**
		 * 已注册的资源模板列表
		 */
		final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();

		/**
		 * 已注册的提示映射表
		 * 使用无状态服务器特定的提示规范
		 */
		final Map<String, McpStatelessServerFeatures.PromptSpecification> prompts = new HashMap<>();

		/**
		 * 根目录变更处理器列表
		 */
		final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeHandlers = new ArrayList<>();

		/**
		 * 请求超时时间
		 * 默认为 10 秒
		 */
		Duration requestTimeout = Duration.ofSeconds(10); // Default timeout

		/**
		 * 构造函数，创建无状态服务器规范对象
		 *
		 * @param transport 无状态服务器传输对象
		 */
		StatelessServerNettySpecification(McpStatelessServerTransport transport) {
			this.transport = transport;
		}

		/**
		 * 设置服务器实现信息
		 *
		 * @param serverInfo 服务器实现信息，包含名称和版本
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification serverInfo(McpSchema.Implementation serverInfo) {
			Assert.notNull(serverInfo, "Server info must not be null");
			this.serverInfo = serverInfo;
			return this;
		}

		/**
		 * 设置请求超时时间
		 *
		 * @param requestTimeout 请求超时时间
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification requestTimeout(Duration requestTimeout) {
			Assert.notNull(requestTimeout, "Request timeout must not be null");
			this.requestTimeout = requestTimeout;
			return this;
		}

		/**
		 * 使用名称和版本设置服务器实现信息
		 *
		 * @param name 服务器名称，不能为空
		 * @param version 服务器版本，不能为空
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification serverInfo(String name, String version) {
			Assert.hasText(name, "Name must not be null or empty");
			Assert.hasText(version, "Version must not be null or empty");
			this.serverInfo = new McpSchema.Implementation(name, version);
			return this;
		}

		/**
		 * 设置服务器使用说明
		 *
		 * @param instructions 服务器使用说明文本
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		/**
		 * 设置服务器能力
		 *
		 * @param serverCapabilities 服务器能力描述对象
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
			this.serverCapabilities = serverCapabilities;
			return this;
		}

		/**
		 * 使用列表批量注册工具
		 *
		 * @param toolRegistrations 工具规范列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification tools(List<McpStatelessServerFeatures.ToolSpecification> toolRegistrations) {
			Assert.notNull(toolRegistrations, "Tool handlers list must not be null");
			this.tools.addAll(toolRegistrations);
			return this;
		}

		/**
		 * 使用可变参数批量注册工具
		 *
		 * @param toolRegistrations 工具规范数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification tools(McpStatelessServerFeatures.ToolSpecification... toolRegistrations) {
			for (McpStatelessServerFeatures.ToolSpecification tool : toolRegistrations) {
				this.tools.add(tool);
			}
			return this;
		}

		/**
		 * 使用映射表批量注册资源
		 *
		 * @param resourceSpecifications 资源规范映射表，键为资源 URI
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification resources(Map<String, McpStatelessServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
			this.resources.putAll(resourceSpecifications);
			return this;
		}

		/**
		 * 使用列表批量注册资源
		 *
		 * @param resourceSpecifications 资源规范列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification resources(List<McpStatelessServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpStatelessServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		/**
		 * 使用可变参数批量注册资源
		 *
		 * @param resourceSpecifications 资源规范数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification resources(McpStatelessServerFeatures.ResourceSpecification... resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpStatelessServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		/**
		 * 使用列表批量注册资源模板
		 *
		 * @param resourceTemplates 资源模板列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(resourceTemplates);
			return this;
		}

		/**
		 * 使用可变参数批量注册资源模板
		 *
		 * @param resourceTemplates 资源模板数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(Arrays.asList(resourceTemplates));
			return this;
		}

		/**
		 * 使用映射表批量注册提示
		 *
		 * @param prompts 提示规范映射表，键为提示名称
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification prompts(Map<String, McpStatelessServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			this.prompts.putAll(prompts);
			return this;
		}

		/**
		 * 使用列表批量注册提示
		 *
		 * @param prompts 提示规范列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification prompts(List<McpStatelessServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpStatelessServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		/**
		 * 使用可变参数批量注册提示
		 *
		 * @param prompts 提示规范数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification prompts(McpStatelessServerFeatures.PromptSpecification... prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpStatelessServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		/**
		 * 注册单个根目录变更处理器
		 *
		 * @param handler 根目录变更处理函数
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification rootsChangeHandler(
					BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> handler) {
			Assert.notNull(handler, "Consumer must not be null");
			this.rootsChangeHandlers.add(handler);
			return this;
		}


		/**
		 * 使用列表批量注册根目录变更处理器
		 *
		 * @param handlers 根目录变更处理函数列表
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification rootsChangeHandlers(
					List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			this.rootsChangeHandlers.addAll(handlers);
			return this;
		}

		/**
		 * 使用可变参数批量注册根目录变更处理器
		 *
		 * @param handlers 根目录变更处理函数数组
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification rootsChangeHandlers(
					@SuppressWarnings("unchecked") BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>... handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			return this.rootsChangeHandlers(Arrays.asList(handlers));
		}

		/**
		 * 设置自定义的 JSON 对象映射器
		 *
		 * @param objectMapper JSON 对象映射器
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		/**
		 * 设置命令执行器
		 * 命令执行器必须在构建服务器之前设置
		 *
		 * @param commandExecutor 命令执行器
		 * @return 当前规范对象，支持链式调用
		 */
		public StatelessServerNettySpecification commandExecutor(CommandExecutor commandExecutor) {
			Assert.notNull(commandExecutor, "CommandExecutor must not be null");
			this.commandExecutor = commandExecutor;
			return this;
		}

		/**
		 * 构建并返回无状态 MCP Netty 服务器实例
		 * 此方法会验证配置，并创建服务器实例
		 *
		 * @return 已配置的无状态 MCP Netty 服务器实例
		 */
		public McpStatelessNettyServer build() {
			// 使用自定义的 ObjectMapper 或默认的 ObjectMapper
			ObjectMapper mapper = this.objectMapper != null ? this.objectMapper : JsonParser.getObjectMapper();
			// 创建并返回无状态服务器实例
			return new McpStatelessNettyServer(
						this.transport,
						mapper,
						this.requestTimeout,
						new McpStatelessServerFeatures.McpServerConfig(
								this.serverInfo,
								this.serverCapabilities,
								this.tools,
								this.resources,
								this.resourceTemplates,
								this.prompts,
								this.instructions
						),
						this.commandExecutor
				);
		}

	}
}
