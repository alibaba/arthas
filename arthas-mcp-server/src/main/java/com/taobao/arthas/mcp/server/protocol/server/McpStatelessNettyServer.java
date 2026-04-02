/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpStatelessServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * 基于Netty的MCP无状态服务器实现
 * 提供对工具、资源和提示的访问能力
 *
 * @author Yeaury
 */
public class McpStatelessNettyServer {

	// 日志记录器
	private static final Logger logger = LoggerFactory.getLogger(McpStatelessNettyServer.class);

	// MCP传输层提供者
	private final McpStatelessServerTransport mcpTransportProvider;

	// JSON对象映射器，用于序列化和反序列化
	private final ObjectMapper objectMapper;

	// 服务器能力声明
	private final McpSchema.ServerCapabilities serverCapabilities;

	// 服务器实现信息
	private final McpSchema.Implementation serverInfo;

	// 服务器使用说明
	private final String instructions;

	// 工具规范列表（线程安全的CopyOnWriteArrayList）
	private final CopyOnWriteArrayList<McpStatelessServerFeatures.ToolSpecification> tools = new CopyOnWriteArrayList<>();

	// 资源模板列表（线程安全的CopyOnWriteArrayList）
	private final CopyOnWriteArrayList<McpSchema.ResourceTemplate> resourceTemplates = new CopyOnWriteArrayList<>();

	// 资源规范映射表（线程安全的ConcurrentHashMap，key为资源URI）
	private final ConcurrentHashMap<String, McpStatelessServerFeatures.ResourceSpecification> resources = new ConcurrentHashMap<>();

	// 提示规范映射表（线程安全的ConcurrentHashMap，key为提示名称）
	private final ConcurrentHashMap<String, McpStatelessServerFeatures.PromptSpecification> prompts = new ConcurrentHashMap<>();

	// 支持的协议版本列表
	private List<String> protocolVersions;

	/**
	 * 构造MCP无状态Netty服务器
	 *
	 * @param mcpTransport MCP传输层
	 * @param objectMapper JSON对象映射器
	 * @param requestTimeout 请求超时时间
	 * @param features 服务器功能配置
	 * @param commandExecutor 命令执行器
	 */
	public McpStatelessNettyServer(
			McpStatelessServerTransport mcpTransport,
			ObjectMapper objectMapper,
			Duration requestTimeout,
			McpStatelessServerFeatures.McpServerConfig features,
			CommandExecutor commandExecutor) {
		this.mcpTransportProvider = mcpTransport;
		this.objectMapper = objectMapper;
		this.serverInfo = features.getServerInfo();
		this.serverCapabilities = features.getServerCapabilities();
		this.instructions = features.getInstructions();
		// 初始化工具列表
		this.tools.addAll(features.getTools());
		// 初始化资源映射表
		this.resources.putAll(features.getResources());
		// 初始化资源模板列表
		this.resourceTemplates.addAll(features.getResourceTemplates());
		// 初始化提示映射表
		this.prompts.putAll(features.getPrompts());

		// 初始化支持的协议版本列表
		this.protocolVersions = new ArrayList<>(mcpTransport.protocolVersions());

		// 创建请求处理器映射表
		Map<String, McpStatelessRequestHandler<?>> requestHandlers = new HashMap<>();

		// 初始化标准MCP方法的请求处理器
		requestHandlers.put(McpSchema.METHOD_INITIALIZE, initializeRequestHandler());

		// Ping请求必须返回空数据，但不能返回NULL响应
		requestHandlers.put(McpSchema.METHOD_PING,
				(exchange, commandContext, params) -> CompletableFuture.completedFuture(Collections.emptyMap()));

		// 如果启用了工具能力，添加工具API处理器
		if (this.serverCapabilities.getTools() != null) {
			requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
			requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
		}

		// 如果提供了资源能力，添加资源API处理器
		if (this.serverCapabilities.getResources() != null) {
			requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST, resourcesListRequestHandler());
			requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
			requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, resourceTemplateListRequestHandler());
		}

		// 如果提供了提示能力，添加提示API处理器
		if (this.serverCapabilities.getPrompts() != null) {
			requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
			requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
		}

		// 创建通知处理器映射表
		Map<String, McpStatelessNotificationHandler> notificationHandlers = new HashMap<>();
		// 添加初始化完成通知处理器
		notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED, (ctx, params) -> CompletableFuture.completedFuture(null));

		// 创建默认的MCP服务器处理器并设置到传输层
		McpStatelessServerHandler handler = new DefaultMcpStatelessServerHandler(requestHandlers, notificationHandlers, commandExecutor);
		mcpTransport.setMcpHandler(handler);
	}

	// ---------------------------------------
	// 生命周期管理
	// ---------------------------------------

	/**
	 * 创建初始化请求处理器
	 * @return 初始化请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.InitializeResult> initializeRequestHandler() {
		return (exchange, commandContext, params) -> CompletableFuture.supplyAsync(() -> {
			// 将请求参数转换为初始化请求对象
			McpSchema.InitializeRequest initializeRequest = objectMapper.convertValue(params, McpSchema.InitializeRequest.class);

			logger.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
					initializeRequest.getProtocolVersion(), initializeRequest.getCapabilities(),
					initializeRequest.getClientInfo());

			// 服务器必须返回其支持的最高协议版本
			// 如果不支持客户端请求的版本
			String serverProtocolVersion = protocolVersions.get(protocolVersions.size() - 1);

			// 检查是否支持客户端请求的协议版本
			if (protocolVersions.contains(initializeRequest.getProtocolVersion())) {
				serverProtocolVersion = initializeRequest.getProtocolVersion();
			} else {
				logger.warn(
						"Client requested unsupported protocol version: {}, " + "so the server will suggest {} instead",
						initializeRequest.getProtocolVersion(), serverProtocolVersion);
			}

			// 返回初始化结果
			return new McpSchema.InitializeResult(serverProtocolVersion, serverCapabilities, serverInfo, instructions);
		});
	}

	/**
	 * 获取服务器能力声明
	 * @return 服务器能力声明对象
	 */
	public McpSchema.ServerCapabilities getServerCapabilities() {
		return this.serverCapabilities;
	}

	/**
	 * 获取服务器实现信息
	 * @return 服务器实现信息对象
	 */
	public McpSchema.Implementation getServerInfo() {
		return this.serverInfo;
	}

	/**
	 * 优雅地关闭服务器
	 * @return 异步关闭操作的Future
	 */
	public CompletableFuture<Void> closeGracefully() {
		return this.mcpTransportProvider.closeGracefully();
	}

	/**
	 * 立即关闭服务器
	 */
	public void close() {
		this.mcpTransportProvider.close();
	}

	/**
	 * 创建根目录列表变化通知处理器
	 * @param rootsChangeConsumers 根目录变化监听器列表
	 * @return 通知处理器
	 */
	private McpNotificationHandler rootsListChangedNotificationHandler(
			List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers) {
		return (exchange, commandContext, params) -> {
			// 异步获取根目录列表
			CompletableFuture<McpSchema.ListRootsResult> futureRoots = exchange.listRoots();

			// 当根目录列表返回后，通知所有监听器
			return futureRoots.thenCompose(listRootsResult -> {
				List<McpSchema.Root> roots = listRootsResult.getRoots();
				List<CompletableFuture<?>> futures = new ArrayList<>();
				// 遍历所有监听器，依次处理根目录变化
				for (BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> consumer : rootsChangeConsumers) {
					CompletableFuture<Void> future = consumer.apply(exchange, roots).exceptionally(error -> {
						logger.error("Error handling roots list change notification", error);
						return null;
					});
					futures.add(future);
				}
				// 等待所有监听器处理完成
				return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			});
		};
	}

	// ---------------------------------------
	// 工具管理
	// ---------------------------------------

	/**
	 * 添加工具规范
	 * @param toolSpecification 工具规范对象
	 * @return 异步添加操作的Future
	 */
	public CompletableFuture<Void> addTool(McpStatelessServerFeatures.ToolSpecification toolSpecification) {
		// 参数校验：工具规范不能为null
		if (toolSpecification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool specification must not be null"));
			return future;
		}
		// 参数校验：工具定义不能为null
		if (toolSpecification.getTool() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool must not be null"));
			return future;
		}
		// 参数校验：工具调用处理函数不能为null
		if (toolSpecification.getCall() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool call handler must not be null"));
			return future;
		}
		// 参数校验：服务器必须配置了工具能力
		if (this.serverCapabilities.getTools() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with tool capabilities"));
			return future;
		}

		return CompletableFuture
				.runAsync(() -> {
					// 检查是否已存在同名工具
					if (this.tools.stream().anyMatch(th ->
							th.getTool().getName().equals(toolSpecification.getTool().getName()))) {
						throw new CompletionException(
								new McpError("Tool with name '" + toolSpecification.getTool().getName() + "' already exists"));
					}
					// 添加工具到列表
					this.tools.add(toolSpecification);
					logger.debug("Added tool handler: {}", toolSpecification.getTool().getName());
				})
				.exceptionally(ex -> {
					// 处理异常
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					logger.error("Error while adding tool", cause);
					throw new CompletionException(cause);
				});
	}

	/**
	 * 移除工具
	 * @param toolName 工具名称
	 * @return 异步移除操作的Future
	 */
	public CompletableFuture<Void> removeTool(String toolName) {
		// 参数校验：工具名称不能为null
		if (toolName == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool name must not be null"));
			return future;
		}
		// 参数校验：服务器必须配置了工具能力
		if (this.serverCapabilities.getTools() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with tool capabilities"));
			return future;
		}

		return CompletableFuture
				.runAsync(() -> {
					// 尝试移除指定名称的工具
					boolean removed = this.tools.removeIf(
							spec -> spec.getTool().getName().equals(toolName));
					// 如果工具不存在，抛出异常
					if (!removed) {
						throw new CompletionException(
								new McpError("Tool with name '" + toolName + "' not found"));
					}
					logger.debug("Removed tool handler: {}", toolName);
				})
				.exceptionally(ex -> {
					// 处理异常
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					logger.error("Error while removing tool '{}'", toolName, cause);
					throw new CompletionException(cause);
				});
	}

	/**
	 * 创建工具列表请求处理器
	 * @return 工具列表请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 构建工具列表
			List<McpSchema.Tool> tools = new ArrayList<>();
			for (McpStatelessServerFeatures.ToolSpecification toolSpec : this.tools) {
				tools.add(toolSpec.getTool());
			}

			// 返回工具列表结果
			return CompletableFuture.completedFuture(new McpSchema.ListToolsResult(tools, null));
		};
	}

	/**
	 * 创建工具调用请求处理器
	 * @return 工具调用请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.CallToolResult> toolsCallRequestHandler() {
		return (context, commandContext, params) -> {
			// 将请求参数转换为工具调用请求对象
			McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.CallToolRequest>() {
					});
			// 根据工具名称查找对应的工具规范
			Optional<McpStatelessServerFeatures.ToolSpecification> toolSpecification = this.tools.stream()
				.filter(tr -> callToolRequest.getName().equals(tr.getTool().getName()))
				.findAny();

			// 如果工具不存在，返回错误
			if (!toolSpecification.isPresent()) {
				CompletableFuture<McpSchema.CallToolResult> future = new CompletableFuture<>();
				future.completeExceptionally(new McpError("no tool found: " + callToolRequest.getName()));
				return future;
			}

			// 调用工具的处理函数
			return toolSpecification.get().getCall().apply(context, commandContext, callToolRequest.getArguments());
		};
	}

	// ---------------------------------------
	// 资源管理
	// ---------------------------------------

	/**
	 * 添加资源规范
	 * @param resourceSpecification 资源规范对象
	 * @return 异步添加操作的Future
	 */
	public CompletableFuture<Void> addResource(McpStatelessServerFeatures.ResourceSpecification resourceSpecification) {
		// 参数校验：资源规范和资源定义不能为null
		if (resourceSpecification == null || resourceSpecification.getResource() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource must not be null"));
			return future;
		}
		// 参数校验：服务器必须配置了资源能力
		if (this.serverCapabilities.getResources() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with resource capabilities"));
			return future;
		}

		return CompletableFuture
				.runAsync(() -> {
					// 获取资源URI
					String uri = resourceSpecification.getResource().getUri();
					// 如果URI已存在，抛出异常
					if (this.resources.putIfAbsent(uri, resourceSpecification) != null) {
						throw new CompletionException(new McpError("Resource with URI '" + uri + "' already exists"));
					}
					logger.debug("Added resource handler: {}", uri);
				})
				.exceptionally(ex -> {
					// 处理异常
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					logger.error("Error while adding resource '{}'",
							resourceSpecification.getResource().getUri(), cause);
					throw new CompletionException(cause);
				});
	}

	/**
	 * 移除资源
	 * @param resourceUri 资源URI
	 * @return 异步移除操作的Future
	 */
	public CompletableFuture<Void> removeResource(String resourceUri) {
		// 参数校验：资源URI不能为null
		if (resourceUri == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource URI must not be null"));
			return future;
		}
		// 参数校验：服务器必须配置了资源能力
		if (this.serverCapabilities.getResources() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with resource capabilities"));
			return future;
		}

		return CompletableFuture
				.runAsync(() -> {
					// 尝试移除指定URI的资源
					McpStatelessServerFeatures.ResourceSpecification removed = this.resources.remove(resourceUri);
					// 如果资源不存在，抛出异常
					if (removed == null) {
						throw new CompletionException(new McpError("Resource with URI '" + resourceUri + "' not found"));
					}
					logger.debug("Removed resource handler: {}", resourceUri);
				})
				.exceptionally(ex -> {
					// 处理异常
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					logger.error("Error while removing resource '{}'", resourceUri, cause);
					throw new CompletionException(cause);
				});
	}

	/**
	 * 创建资源列表请求处理器
	 * @return 资源列表请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.ListResourcesResult> resourcesListRequestHandler() {
		return (exchange, commandContext,  params) -> {
			// 构建资源列表
			List<McpSchema.Resource> resourceList = new ArrayList<>();
			for (McpStatelessServerFeatures.ResourceSpecification spec : this.resources.values()) {
				resourceList.add(spec.getResource());
			}
			// 返回资源列表结果
			return CompletableFuture.completedFuture(new McpSchema.ListResourcesResult(resourceList, null));
		};
	}

	/**
	 * 创建资源模板列表请求处理器
	 * @return 资源模板列表请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
		return (context, commandContext, params) -> CompletableFuture
			.completedFuture(new McpSchema.ListResourceTemplatesResult(this.resourceTemplates, null));
	}

	/**
	 * 创建资源读取请求处理器
	 * @return 资源读取请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.ReadResourceResult> resourcesReadRequestHandler() {
		return (context, commandContext, params) -> {
			// 将请求参数转换为资源读取请求对象
			McpSchema.ReadResourceRequest resourceRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.ReadResourceRequest>() {
					});
			// 获取资源URI
			String resourceUri = resourceRequest.getUri();
			// 查找对应的资源规范
			McpStatelessServerFeatures.ResourceSpecification specification = this.resources.get(resourceUri);
			// 如果找到资源，调用读取处理函数
			if (specification != null) {
				return specification.getReadHandler().apply(context, resourceRequest);
			}
			// 资源不存在，返回错误
			CompletableFuture<McpSchema.ReadResourceResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource not found: " + resourceUri));
			return future;
		};
	}

	// ---------------------------------------
	// 提示管理
	// ---------------------------------------

	/**
	 * 添加提示规范
	 * @param promptSpecification 提示规范对象
	 * @return 异步添加操作的Future
	 */
	public CompletableFuture<Void> addPrompt(McpStatelessServerFeatures.PromptSpecification promptSpecification) {
		// 参数校验：提示规范不能为null
		if (promptSpecification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt specification must not be null"));
			return future;
		}
		// 参数校验：服务器必须配置了提示能力
		if (this.serverCapabilities.getPrompts() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with prompt capabilities"));
			return future;
		}

		return CompletableFuture
				.runAsync(() -> {
					// 获取提示名称
					String name = promptSpecification.getPrompt().getName();
					// 如果提示名称已存在，抛出异常
					McpStatelessServerFeatures.PromptSpecification existing =
							this.prompts.putIfAbsent(name, promptSpecification);
					if (existing != null) {
						throw new CompletionException(new McpError("Prompt with name '" + name + "' already exists"));
					}
					logger.debug("Added prompt handler: {}", name);
				})
				.exceptionally(ex -> {
					// 处理异常
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					String name = promptSpecification.getPrompt().getName();
					logger.error("Error while adding prompt '{}'", name, cause);
					throw new CompletionException(cause);
				});
	}

	/**
	 * 移除提示
	 * @param promptName 提示名称
	 * @return 异步移除操作的Future
	 */
	public CompletableFuture<Void> removePrompt(String promptName) {
		// 参数校验：提示名称不能为null或空
		if (promptName == null || promptName.isEmpty()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt name must not be null or empty"));
			return future;
		}
		// 参数校验：服务器必须配置了提示能力
		if (this.serverCapabilities.getPrompts() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with prompt capabilities"));
			return future;
		}

		return CompletableFuture
				.runAsync(() -> {
					// 尝试移除指定名称的提示
					McpStatelessServerFeatures.PromptSpecification removed =
							this.prompts.remove(promptName);
					// 如果提示不存在，抛出异常
					if (removed == null) {
						throw new CompletionException(new McpError("Prompt with name '" + promptName + "' not found"));
					}
					logger.debug("Removed prompt handler: {}", promptName);
				})
				.exceptionally(ex -> {
					// 处理异常
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					logger.error("Error while removing prompt '{}'", promptName, cause);
					throw new CompletionException(cause);
				});
	}


	/**
	 * 创建提示列表请求处理器
	 * @return 提示列表请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.ListPromptsResult> promptsListRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 构建提示列表
			List<McpSchema.Prompt> promptList = new ArrayList<>();
			for (McpStatelessServerFeatures.PromptSpecification promptSpec : this.prompts.values()) {
				promptList.add(promptSpec.getPrompt());
			}
			// 返回提示列表结果
			return CompletableFuture.completedFuture(new McpSchema.ListPromptsResult(promptList, null));
		};
	}

	/**
	 * 创建提示获取请求处理器
	 * @return 提示获取请求处理器
	 */
	private McpStatelessRequestHandler<McpSchema.GetPromptResult> promptsGetRequestHandler() {
		return (context, commandContext, params) -> {
			// 将请求参数转换为提示获取请求对象
			McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.GetPromptRequest>() {
					});

			// 查找对应的提示规范
			McpStatelessServerFeatures.PromptSpecification specification = this.prompts.get(promptRequest.getName());
			// 如果找到提示，调用获取处理函数
			if (specification != null) {
				return specification.getPromptHandler().apply(context, promptRequest);
			}
			// 提示不存在，返回错误
			CompletableFuture<McpSchema.GetPromptResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt not found: " + promptRequest.getName()));
			return future;
		};
	}


	// ---------------------------------------
	// 采样
	// ---------------------------------------

	/**
	 * 设置支持的协议版本列表
	 * @param protocolVersions 协议版本列表
	 */
	public void setProtocolVersions(List<String> protocolVersions) {
		this.protocolVersions = protocolVersions;
	}

}
