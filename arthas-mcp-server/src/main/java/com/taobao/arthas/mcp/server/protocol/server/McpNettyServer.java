/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.spec.*;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.Utils;
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
 * 基于Netty的MCP服务器实现类
 *
 * 该类是MCP（Model Context Protocol）服务器的核心实现，基于Netty框架提供高性能的异步IO能力。
 * 服务器提供对工具（Tools）、资源（Resources）和提示（Prompts）的访问和管理功能。
 *
 * 主要功能包括：
 * 1. 会话管理：创建和管理客户端会话，处理初始化握手
 * 2. 工具管理：注册、注销和调用工具，支持动态工具列表变更通知
 * 3. 资源管理：注册、注销和读取资源，支持动态资源列表变更通知
 * 4. 提示管理：注册、注销和获取提示，支持动态提示列表变更通知
 * 5. 日志管理：设置日志级别，发送日志消息通知
 * 6. 协议版本协商：支持多版本协议，自动协商最佳版本
 *
 * 该实现使用线程安全的集合类（如CopyOnWriteArrayList、ConcurrentHashMap）
 * 来保证并发场景下的数据一致性。
 *
 * @author Yeaury
 */
public class McpNettyServer {

	/**
	 * 日志记录器
	 * 用于记录服务器运行时的各种状态信息和错误信息
	 */
	private static final Logger logger = LoggerFactory.getLogger(McpNettyServer.class);

	/**
	 * MCP传输提供者
	 * 负责底层的网络通信、消息编解码和会话管理
	 */
	private final McpServerTransportProvider mcpTransportProvider;

	/**
	 * JSON对象映射器
	 * 用于JSON和Java对象之间的序列化和反序列化操作
	 */
	private final ObjectMapper objectMapper;

	/**
	 * 服务器能力声明
	 * 定义服务器支持的功能（工具、资源、提示、日志等）
	 */
	private final McpSchema.ServerCapabilities serverCapabilities;

	/**
	 * 服务器实现信息
	 * 包含服务器名称、版本等元数据
	 */
	private final McpSchema.Implementation serverInfo;

	/**
	 * 使用说明文档
	 * 向客户端提供的使用指南或帮助文档
	 */
	private final String instructions;

	/**
	 * 工具规格列表
	 * 使用线程安全的CopyOnWriteArrayList存储所有已注册的工具
	 * 支持动态添加和删除工具
	 */
	private final CopyOnWriteArrayList<McpServerFeatures.ToolSpecification> tools = new CopyOnWriteArrayList<>();

	/**
	 * 资源模板列表
	 * 使用线程安全的CopyOnWriteArrayList存储所有资源模板
	 * 资源模板用于定义资源的创建规则和参数
	 */
	private final CopyOnWriteArrayList<McpSchema.ResourceTemplate> resourceTemplates = new CopyOnWriteArrayList<>();

	/**
	 * 资源规格映射表
	 * 使用ConcurrentHashMap存储已注册的资源，键为资源URI
	 * 支持高并发的资源查找和更新操作
	 */
	private final ConcurrentHashMap<String, McpServerFeatures.ResourceSpecification> resources = new ConcurrentHashMap<>();

	/**
	 * 提示规格映射表
	 * 使用ConcurrentHashMap存储已注册的提示，键为提示名称
	 * 支持高并发的提示查找和更新操作
	 */
	private final ConcurrentHashMap<String, McpServerFeatures.PromptSpecification> prompts = new ConcurrentHashMap<>();

	/**
	 * 最小日志级别
	 * 只有等于或高于此级别的日志才会被处理和发送
	 * 默认为DEBUG级别
	 */
	private McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.DEBUG;

	/**
	 * 支持的协议版本列表
	 * 按从低到高的顺序排列，用于与客户端协商协议版本
	 */
	private List<String> protocolVersions;

	/**
	 * 构造函数 - 创建MCP Netty服务器实例
	 *
	 * 该构造函数初始化服务器的所有组件，包括传输提供者、对象映射器、
	 * 工具、资源、提示等，并配置请求和通知处理器。
	 *
	 * @param mcpTransportProvider MCP传输提供者，负责底层网络通信
	 * @param objectMapper JSON对象映射器，用于序列化和反序列化
	 * @param requestTimeout 请求超时时长
	 * @param features MCP服务器配置，包含服务器能力、工具、资源、提示等信息
	 * @param commandExecutor 命令执行器，用于执行具体的命令逻辑
	 */
	McpNettyServer(McpStreamableServerTransportProvider mcpTransportProvider,
				   ObjectMapper objectMapper, Duration requestTimeout,
				   McpServerFeatures.McpServerConfig features,
				   CommandExecutor commandExecutor) {
		// 保存传输提供者引用
		this.mcpTransportProvider = mcpTransportProvider;
		// 保存对象映射器引用
		this.objectMapper = objectMapper;
		// 从配置中获取服务器信息
		this.serverInfo = features.getServerInfo();
		// 从配置中获取服务器能力声明
		this.serverCapabilities = features.getServerCapabilities();
		// 从配置中获取使用说明
		this.instructions = features.getInstructions();
		// 将配置中的所有工具添加到工具列表
		this.tools.addAll(features.getTools());
		// 将配置中的所有资源添加到资源映射表
		this.resources.putAll(features.getResources());
		// 将配置中的所有资源模板添加到模板列表
		this.resourceTemplates.addAll(features.getResourceTemplates());
		// 将配置中的所有提示添加到提示映射表
		this.prompts.putAll(features.getPrompts());

		// 准备请求处理器，处理各种MCP协议请求
		Map<String, McpRequestHandler<?>> requestHandlers = prepareRequestHandlers();
		// 准备通知处理器，处理各种MCP协议通知
		Map<String, McpNotificationHandler> notificationHandlers = prepareNotificationHandlers(features);

		// 从传输提供者获取支持的协议版本列表
		this.protocolVersions = mcpTransportProvider.protocolVersions();

		// 设置会话工厂，创建会话时使用的工厂实例
		// 传入初始化请求处理器方法引用、请求处理器映射、通知处理器映射和命令执行器
		mcpTransportProvider.setSessionFactory(new DefaultMcpStreamableServerSessionFactory(requestTimeout,
				this::initializeRequestHandler, requestHandlers, notificationHandlers, commandExecutor));
	}

	/**
	 * 准备通知处理器映射表
	 *
	 * 该方法创建并配置所有MCP协议通知的处理器，包括：
	 * 1. initialized通知：客户端完成初始化后发送的通知
	 * 2. roots/list_changed通知：根列表变更时发送的通知
	 *
	 * @param features MCP服务器配置，可能包含根列表变更的消费者
	 * @return 通知方法名到处理器对象的映射表
	 */
	private Map<String, McpNotificationHandler> prepareNotificationHandlers(McpServerFeatures.McpServerConfig features) {
		Map<String, McpNotificationHandler> notificationHandlers = new HashMap<>();

		// 添加初始化完成通知的处理器
		// 客户端发送此通知表示已完成初始化，服务器收到后可以开始正常通信
		notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED,
				(exchange, commandContext, params) -> CompletableFuture.completedFuture(null));

		// 从配置中获取根列表变更的消费者列表
		// 根（Root）是指文件系统或其他资源系统的根路径
		List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers = features
				.getRootsChangeConsumers();

		// 如果没有配置根列表变更消费者，则使用默认的警告日志处理器
		if (Utils.isEmpty(rootsChangeConsumers)) {
			rootsChangeConsumers = Collections.singletonList(
					(exchange, roots) -> CompletableFuture.runAsync(() ->
							logger.warn("Roots list changed notification, but no consumers provided. Roots list changed: {}", roots))
			);
		}

		// 添加根列表变更通知的处理器
		notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED,
				rootsListChangedNotificationHandler(rootsChangeConsumers));
		return notificationHandlers;
	}

	/**
	 * 准备请求处理器映射表
	 *
	 * 该方法创建并配置所有MCP协议请求的处理器，根据服务器的能力声明
	 * 动态注册相应的处理器。包括：
	 * - ping：健康检查请求
	 * - tools/list和tools/call：工具相关请求
	 * - resources/list、resources/read和resources/templates/list：资源相关请求
	 * - prompts/list和prompts/get：提示相关请求
	 * - logging/set_level：日志级别设置请求
	 *
	 * @return 请求方法名到处理器对象的映射表
	 */
	private Map<String, McpRequestHandler<?>> prepareRequestHandlers() {
		Map<String, McpRequestHandler<?>> requestHandlers = new HashMap<>();

		// 为标准MCP方法初始化请求处理器

		// Ping请求必须返回空数据（但不是NULL响应）
		// 用于检测服务器是否正常运行
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

		// 如果存在提示提供者，添加提示API处理器
		if (this.serverCapabilities.getPrompts() != null) {
			requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
			requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
		}

		// 如果启用了日志能力，添加日志API处理器
		if (this.serverCapabilities.getLogging() != null) {
			requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, setLoggerRequestHandler());
		}

		return requestHandlers;
	}


	// ---------------------------------------
	// 生命周期管理
	// ---------------------------------------

	/**
	 * 处理客户端初始化请求
	 *
	 * 该方法是MCP协议握手过程的核心，负责：
	 * 1. 记录客户端的协议版本、能力和信息
	 * 2. 协商双方都支持的协议版本
	 * 3. 如果客户端请求的版本不支持，服务器会返回其支持的最新版本
	 * 4. 返回服务器的能力声明、实现信息和使用说明
	 *
	 * @param initializeRequest 客户端的初始化请求对象
	 * @return CompletableFuture对象，异步返回初始化结果
	 */
	private CompletableFuture<McpSchema.InitializeResult> initializeRequestHandler(
			McpSchema.InitializeRequest initializeRequest) {
		return CompletableFuture.supplyAsync(() -> {
			// 记录客户端初始化请求的详细信息
			logger.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
					initializeRequest.getProtocolVersion(), initializeRequest.getCapabilities(),
					initializeRequest.getClientInfo());

			// 服务器必须返回其支持的最高协议版本
			// 如果服务器不支持客户端请求的版本
			// 默认使用协议版本列表中的最后一个（最高版本）
			String serverProtocolVersion = protocolVersions.get(protocolVersions.size() - 1);

			// 检查服务器是否支持客户端请求的协议版本
			if (protocolVersions.contains(initializeRequest.getProtocolVersion())) {
				// 如果支持，使用客户端请求的版本
				serverProtocolVersion = initializeRequest.getProtocolVersion();
			}
			else {
				// 如果不支持，记录警告并使用服务器的最新版本
				logger.warn(
						"Client requested unsupported protocol version: {}, " + "so the server will suggest {} instead",
						initializeRequest.getProtocolVersion(), serverProtocolVersion);
			}

			// 返回初始化结果，包含协商后的协议版本、服务器能力、服务器信息和使用说明
			return new McpSchema.InitializeResult(serverProtocolVersion, serverCapabilities, serverInfo, instructions);
		});
	}

	/**
	 * 获取服务器能力声明
	 *
	 * @return 服务器能力声明对象，包含支持的工具、资源、提示等能力
	 */
	public McpSchema.ServerCapabilities getServerCapabilities() {
		return this.serverCapabilities;
	}

	/**
	 * 获取服务器实现信息
	 *
	 * @return 服务器实现信息对象，包含名称、版本等元数据
	 */
	public McpSchema.Implementation getServerInfo() {
		return this.serverInfo;
	}

	/**
	 * 优雅关闭服务器
	 *
	 * 该方法会等待所有正在处理的请求完成后再关闭服务器。
	 *
	 * @return CompletableFuture对象，异步完成关闭操作
	 */
	public CompletableFuture<Void> closeGracefully() {
		return this.mcpTransportProvider.closeGracefully();
	}

	/**
	 * 立即关闭服务器
	 *
	 * 该方法会立即关闭服务器，不等待正在处理的请求完成。
	 */
	public void close() {
		this.mcpTransportProvider.close();
	}

	/**
	 * 创建根列表变更通知处理器
	 *
	 * 该处理器在收到根列表变更通知时：
	 * 1. 首先调用exchange.listRoots()获取最新的根列表
	 * 2. 然后将根列表传递给所有注册的消费者进行处理
	 * 3. 等待所有消费者处理完成
	 * 4. 如果某个消费者处理失败，记录错误但不影响其他消费者
	 *
	 * @param rootsChangeConsumers 根列表变更的消费者列表
	 * @return 根列表变更通知处理器
	 */
	private McpNotificationHandler rootsListChangedNotificationHandler(
			List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers) {
		return (exchange, commandContext, params) -> {
			// 异步获取最新的根列表
			CompletableFuture<McpSchema.ListRootsResult> futureRoots = exchange.listRoots();

			// 获取到根列表后，依次调用所有消费者进行处理
			return futureRoots.thenCompose(listRootsResult -> {
				List<McpSchema.Root> roots = listRootsResult.getRoots();
				List<CompletableFuture<?>> futures = new ArrayList<>();
				// 遍历所有消费者，异步处理根列表变更
				for (BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> consumer : rootsChangeConsumers) {
					CompletableFuture<Void> future = consumer.apply(exchange, roots).exceptionally(error -> {
						// 如果某个消费者处理失败，记录错误但不抛出异常
						logger.error("Error handling roots list change notification", error);
						return null;
					});
					futures.add(future);
				}
				// 等待所有消费者处理完成
				return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			});
		};
	}

	// ---------------------------------------
	// 工具管理
	// ---------------------------------------

	/**
	 * 添加工具到服务器
	 *
	 * 该方法会验证工具规格的完整性，检查工具名称是否重复，
	 * 并在成功添加后通知客户端工具列表已变更（如果启用了listChanged能力）。
	 *
	 * @param toolSpecification 工具规格对象，包含工具定义和调用处理器
	 * @return CompletableFuture对象，异步完成添加操作
	 */
	public CompletableFuture<Void> addTool(McpServerFeatures.ToolSpecification toolSpecification) {
		// 验证工具规格不能为空
		if (toolSpecification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool specification must not be null"));
			return future;
		}
		// 验证工具定义不能为空
		if (toolSpecification.getTool() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool must not be null"));
			return future;
		}
		// 验证工具调用处理器不能为空
		if (toolSpecification.getCall() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool call handler must not be null"));
			return future;
		}
		// 验证服务器必须配置了工具能力
		if (this.serverCapabilities.getTools() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with tool capabilities"));
			return future;
		}

		// 异步添加工具
		return CompletableFuture.supplyAsync(() -> {
			// 检查是否存在重复的工具名称
			if (this.tools.stream().anyMatch(th -> th.getTool().getName().equals(toolSpecification.getTool().getName()))) {
				throw new CompletionException(
						new McpError("Tool with name '" + toolSpecification.getTool().getName() + "' already exists"));
			}
			// 添加工具到列表
			this.tools.add(toolSpecification);
			logger.debug("Added tool handler: {}", toolSpecification.getTool().getName());
			return null;
		}).thenCompose(ignored -> {
			// 如果启用了工具列表变更通知，则通知所有客户端
			if (this.serverCapabilities.getTools().getListChanged()) {
				return notifyToolsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			// 处理添加工具过程中的异常
			Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
			logger.error("Error while adding tool", cause);
			throw new CompletionException(cause);
		});
	}

	/**
	 * 从服务器移除工具
	 *
	 * 该方法会根据工具名称移除对应的工具，
	 * 并在成功移除后通知客户端工具列表已变更（如果启用了listChanged能力）。
	 *
	 * @param toolName 要移除的工具名称
	 * @return CompletableFuture对象，异步完成移除操作
	 */
	public CompletableFuture<Void> removeTool(String toolName) {
		// 验证工具名称不能为空
		if (toolName == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool name must not be null"));
			return future;
		}
		// 验证服务器必须配置了工具能力
		if (this.serverCapabilities.getTools() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with tool capabilities"));
			return future;
		}

		// 异步移除工具
		return CompletableFuture.supplyAsync(() -> {
			// 尝试移除指定名称的工具
			boolean removed = this.tools.removeIf(spec -> spec.getTool().getName().equals(toolName));
			if (!removed) {
				// 如果工具不存在，抛出异常
				throw new CompletionException(new McpError("Tool with name '" + toolName + "' not found"));
			}
			logger.debug("Removed tool handler: {}", toolName);
			return null;
		}).thenCompose(ignored -> {
			// 如果启用了工具列表变更通知，则通知所有客户端
			if (this.serverCapabilities.getTools().getListChanged()) {
				return notifyToolsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			// 处理移除工具过程中的异常
			Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
			logger.error("Error while removing tool '{}'", toolName, cause);
			throw new CompletionException(cause);
		});
	}

	/**
	 * 通知所有客户端工具列表已变更
	 *
	 * 该方法会向所有连接的客户端发送tools/list_changed通知，
	 * 告知工具列表已更新，客户端应该重新获取工具列表。
	 *
	 * @return CompletableFuture对象，异步完成通知操作
	 */
	public CompletableFuture<Void> notifyToolsListChanged() {
		logger.debug("Notifying clients about tool list changes");
		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, null);
	}

	/**
	 * 创建工具列表请求处理器
	 *
	 * 该处理器返回服务器当前注册的所有工具的列表。
	 *
	 * @return 工具列表请求处理器
	 */
	private McpRequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 创建工具列表
			List<McpSchema.Tool> tools = new ArrayList<>();
			// 遍历所有工具规格，提取工具定义
			for (McpServerFeatures.ToolSpecification toolSpec : this.tools) {
				tools.add(toolSpec.getTool());
			}

			// 返回工具列表结果
			return CompletableFuture.completedFuture(new McpSchema.ListToolsResult(tools, null));
		};
	}

	/**
	 * 创建工具调用请求处理器
	 *
	 * 该处理器处理客户端的工具调用请求：
	 * 1. 从请求参数中解析出工具名称和参数
	 * 2. 查找对应的工具规格
	 * 3. 调用工具的执行处理器
	 * 4. 返回执行结果
	 *
	 * @return 工具调用请求处理器
	 */
	private McpRequestHandler<McpSchema.CallToolResult> toolsCallRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 将请求参数转换为CallToolRequest对象
			McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.CallToolRequest>() {
					});
			// 根据工具名称查找对应的工具规格
			Optional<McpServerFeatures.ToolSpecification> toolSpecification = this.tools.stream()
				.filter(tr -> callToolRequest.getName().equals(tr.getTool().getName()))
				.findAny();

			// 如果找不到工具，返回错误
			if (!toolSpecification.isPresent()) {
				CompletableFuture<McpSchema.CallToolResult> future = new CompletableFuture<>();
				future.completeExceptionally(new McpError("no tool found: " + callToolRequest.getName()));
				return future;
			}

			// 调用工具的执行处理器并返回结果
			return toolSpecification.get().getCall().apply(exchange, commandContext, callToolRequest);
		};
	}

	// ---------------------------------------
	// 资源管理
	// ---------------------------------------

	/**
	 * 添加资源到服务器
	 *
	 * 该方法会验证资源规格的完整性，检查资源URI是否重复，
	 * 并在成功添加后通知客户端资源列表已变更（如果启用了listChanged能力）。
	 *
	 * @param resourceSpecification 资源规格对象，包含资源定义和读取处理器
	 * @return CompletableFuture对象，异步完成添加操作
	 */
	public CompletableFuture<Void> addResource(McpServerFeatures.ResourceSpecification resourceSpecification) {
		// 验证资源规格和资源定义不能为空
		if (resourceSpecification == null || resourceSpecification.getResource() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource must not be null"));
			return future;
		}
		// 验证服务器必须配置了资源能力
		if (this.serverCapabilities.getResources() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with resource capabilities"));
			return future;
		}

		// 异步添加资源
		return CompletableFuture.supplyAsync(() -> {
			// 使用putIfAbsent检查资源URI是否已存在
			if (this.resources.putIfAbsent(resourceSpecification.getResource().getUri(), resourceSpecification) != null) {
				throw new CompletionException(new McpError(
						"Resource with URI '" + resourceSpecification.getResource().getUri() + "' already exists"));
			}
			logger.debug("Added resource handler: {}", resourceSpecification.getResource().getUri());
			return null;
		}).thenCompose(ignored -> {
			// 如果启用了资源列表变更通知，则通知所有客户端
			if (this.serverCapabilities.getResources().getListChanged()) {
				return notifyResourcesListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			// 处理添加资源过程中的异常
			Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
			logger.error("Error while adding resource '{}'", resourceSpecification.getResource().getUri(), cause);
			throw new CompletionException(cause);
		});
	}

	/**
	 * 从服务器移除资源
	 *
	 * 该方法会根据资源URI移除对应的资源，
	 * 并在成功移除后通知客户端资源列表已变更（如果启用了listChanged能力）。
	 *
	 * @param resourceUri 要移除的资源URI
	 * @return CompletableFuture对象，异步完成移除操作
	 */
	public CompletableFuture<Void> removeResource(String resourceUri) {
		// 验证资源URI不能为空
		if (resourceUri == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource URI must not be null"));
			return future;
		}
		// 验证服务器必须配置了资源能力
		if (this.serverCapabilities.getResources() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with resource capabilities"));
			return future;
		}

		// 异步移除资源
		return CompletableFuture.supplyAsync(() -> {
			// 尝试移除指定URI的资源
			McpServerFeatures.ResourceSpecification removed = this.resources.remove(resourceUri);
			if (removed == null) {
				// 如果资源不存在，抛出异常
				throw new CompletionException(new McpError("Resource with URI '" + resourceUri + "' not found"));
			}

			logger.debug("Removed resource handler: {}", resourceUri);
			return null;
		}).thenCompose(ignored -> {
			// 如果启用了资源列表变更通知，则通知所有客户端
			if (this.serverCapabilities.getResources().getListChanged()) {
				return notifyResourcesListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			// 处理移除资源过程中的异常
			Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
			logger.error("Error while removing resource '{}'", resourceUri, cause);
			throw new CompletionException(cause);
		});
	}

	/**
	 * 通知所有客户端资源列表已变更
	 *
	 * 该方法会向所有连接的客户端发送resources/list_changed通知，
	 * 告知资源列表已更新，客户端应该重新获取资源列表。
	 *
	 * @return CompletableFuture对象，异步完成通知操作
	 */
	public CompletableFuture<Void> notifyResourcesListChanged() {
		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED, null);
	}

	/**
	 * 创建资源列表请求处理器
	 *
	 * 该处理器返回服务器当前注册的所有资源的列表。
	 *
	 * @return 资源列表请求处理器
	 */
	private McpRequestHandler<McpSchema.ListResourcesResult> resourcesListRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 创建资源列表
			List<McpSchema.Resource> resourceList = new ArrayList<>();
			// 遍历所有资源规格，提取资源定义
			for (McpServerFeatures.ResourceSpecification spec : this.resources.values()) {
				resourceList.add(spec.getResource());
			}
			// 返回资源列表结果
			return CompletableFuture.completedFuture(new McpSchema.ListResourcesResult(resourceList, null));
		};
	}

	/**
	 * 创建资源模板列表请求处理器
	 *
	 * 该处理器返回服务器当前注册的所有资源模板的列表。
	 * 资源模板定义了如何创建新资源的规则。
	 *
	 * @return 资源模板列表请求处理器
	 */
	private McpRequestHandler<McpSchema.ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
		return (exchange, commandContext, params) -> CompletableFuture
			.completedFuture(new McpSchema.ListResourceTemplatesResult(this.resourceTemplates, null));
	}

	/**
	 * 创建资源读取请求处理器
	 *
	 * 该处理器处理客户端的资源读取请求：
	 * 1. 从请求参数中解析出资源URI
	 * 2. 查找对应的资源规格
	 * 3. 调用资源的读取处理器
	 * 4. 返回资源内容
	 *
	 * @return 资源读取请求处理器
	 */
	private McpRequestHandler<McpSchema.ReadResourceResult> resourcesReadRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 将请求参数转换为ReadResourceRequest对象
			McpSchema.ReadResourceRequest resourceRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.ReadResourceRequest>() {
					});
			// 获取资源URI
			String resourceUri = resourceRequest.getUri();
			// 查找对应的资源规格
			McpServerFeatures.ResourceSpecification specification = this.resources.get(resourceUri);
			if (specification != null) {
				// 调用资源的读取处理器并返回结果
				return specification.getReadHandler().apply(exchange, resourceRequest);
			}
			// 如果资源不存在，返回错误
			CompletableFuture<McpSchema.ReadResourceResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource not found: " + resourceUri));
			return future;
		};
	}

	// ---------------------------------------
	// 提示管理
	// ---------------------------------------

	/**
	 * 添加提示到服务器
	 *
	 * 该方法会验证提示规格的完整性，检查提示名称是否重复，
	 * 并在成功添加后通知客户端提示列表已变更（如果启用了listChanged能力）。
	 *
	 * @param promptSpecification 提示规格对象，包含提示定义和获取处理器
	 * @return CompletableFuture对象，异步完成添加操作
	 */
	public CompletableFuture<Void> addPrompt(McpServerFeatures.PromptSpecification promptSpecification) {
		// 验证提示规格不能为空
		if (promptSpecification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt specification must not be null"));
			return future;
		}
		// 验证服务器必须配置了提示能力
		if (this.serverCapabilities.getPrompts() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with prompt capabilities"));
			return future;
		}

		// 异步添加提示
		return CompletableFuture.supplyAsync(() -> {
			// 使用putIfAbsent检查提示名称是否已存在
			McpServerFeatures.PromptSpecification existing = this.prompts
				.putIfAbsent(promptSpecification.getPrompt().getName(), promptSpecification);
			if (existing != null) {
				throw new CompletionException(
						new McpError("Prompt with name '" + promptSpecification.getPrompt().getName() + "' already exists"));
			}

			logger.debug("Added prompt handler: {}", promptSpecification.getPrompt().getName());
			return null;
		}).thenCompose(ignored -> {
			// 如果启用了提示列表变更通知，则通知所有客户端
			if (this.serverCapabilities.getPrompts().getListChanged()) {
				return notifyPromptsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			// 处理添加提示过程中的异常
			Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
			logger.error("Error while adding prompt '{}'", promptSpecification.getPrompt().getName(), cause);
			throw new CompletionException(cause);
		});
	}

	/**
	 * 从服务器移除提示
	 *
	 * 该方法会根据提示名称移除对应的提示，
	 * 并在成功移除后通知客户端提示列表已变更（如果启用了listChanged能力）。
	 *
	 * @param promptName 要移除的提示名称
	 * @return CompletableFuture对象，异步完成移除操作
	 */
	public CompletableFuture<Void> removePrompt(String promptName) {
		// 验证提示名称不能为空
		if (promptName == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt name must not be null"));
			return future;
		}
		// 验证服务器必须配置了提示能力
		if (this.serverCapabilities.getPrompts() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with prompt capabilities"));
			return future;
		}

		// 异步移除提示
		return CompletableFuture.supplyAsync(() -> {
			// 尝试移除指定名称的提示
			McpServerFeatures.PromptSpecification removed = this.prompts.remove(promptName);
			if (removed == null) {
				// 如果提示不存在，抛出异常
				throw new CompletionException(new McpError("Prompt with name '" + promptName + "' not found"));
			}
			logger.debug("Removed prompt handler: {}", promptName);
			return null;
		}).thenCompose(ignored -> {
			// 如果启用了提示列表变更通知，则通知所有客户端
			if (this.serverCapabilities.getPrompts().getListChanged()) {
				return notifyPromptsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			// 处理移除提示过程中的异常
			Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
			logger.error("Error while removing prompt '{}'", promptName, cause);
			throw new CompletionException(cause);
		});
	}

	/**
	 * 通知所有客户端提示列表已变更
	 *
	 * 该方法会向所有连接的客户端发送prompts/list_changed通知，
	 * 告知提示列表已更新，客户端应该重新获取提示列表。
	 *
	 * @return CompletableFuture对象，异步完成通知操作
	 */
	public CompletableFuture<Void> notifyPromptsListChanged() {
		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED, null);
	}

	/**
	 * 创建提示列表请求处理器
	 *
	 * 该处理器返回服务器当前注册的所有提示的列表。
	 *
	 * @return 提示列表请求处理器
	 */
	private McpRequestHandler<McpSchema.ListPromptsResult> promptsListRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 创建提示列表
			List<McpSchema.Prompt> promptList = new ArrayList<>();
			// 遍历所有提示规格，提取提示定义
			for (McpServerFeatures.PromptSpecification promptSpec : this.prompts.values()) {
				promptList.add(promptSpec.getPrompt());
			}
			// 返回提示列表结果
			return CompletableFuture.completedFuture(new McpSchema.ListPromptsResult(promptList, null));
		};
	}

	/**
	 * 创建提示获取请求处理器
	 *
	 * 该处理器处理客户端的提示获取请求：
	 * 1. 从请求参数中解析出提示名称和参数
	 * 2. 查找对应的提示规格
	 * 3. 调用提示的获取处理器
	 * 4. 返回提示内容
	 *
	 * @return 提示获取请求处理器
	 */
	private McpRequestHandler<McpSchema.GetPromptResult> promptsGetRequestHandler() {
		return (exchange, commandContext, params) -> {
			// 将请求参数转换为GetPromptRequest对象
			McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.GetPromptRequest>() {
					});

			// 查找对应的提示规格
			McpServerFeatures.PromptSpecification specification = this.prompts.get(promptRequest.getName());
			if (specification != null) {
				// 调用提示的获取处理器并返回结果
				return specification.getPromptHandler().apply(exchange, promptRequest);
			}
			// 如果提示不存在，返回错误
			CompletableFuture<McpSchema.GetPromptResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt not found: " + promptRequest.getName()));
			return future;
		};
	}

	// ---------------------------------------
	// 日志管理
	// ---------------------------------------

	/**
	 * 发送日志消息通知
	 *
	 * 该方法向所有客户端发送日志消息通知。
	 * 只有等于或高于当前最小日志级别的消息才会被发送。
	 *
	 * @param loggingMessageNotification 日志消息通知对象
	 * @return CompletableFuture对象，异步完成通知操作
	 */
	public CompletableFuture<Void> loggingNotification(
			McpSchema.LoggingMessageNotification loggingMessageNotification) {
		// 验证日志消息通知不能为空
		Assert.notNull(loggingMessageNotification, "Logging message must not be null");

		// 检查日志级别是否满足最小级别要求
		if (loggingMessageNotification.getLevel().level() < minLoggingLevel.level()) {
			return CompletableFuture.completedFuture(null);
		}

		// 向所有客户端发送日志消息通知
		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_MESSAGE,
				loggingMessageNotification);
	}

	/**
	 * 创建日志级别设置请求处理器
	 *
	 * 该处理器处理客户端的日志级别设置请求：
	 * 1. 从请求参数中解析出目标日志级别
	 * 2. 更新服务器的最小日志级别
	 * 3. 返回成功响应
	 *
	 * @return 日志级别设置请求处理器
	 */
	private McpRequestHandler<Map<String, Object>> setLoggerRequestHandler() {
		return (exchange, commandContext, params) -> {
			try {
				// 将请求参数转换为SetLevelRequest对象
				McpSchema.SetLevelRequest request = this.objectMapper.convertValue(params,
						McpSchema.SetLevelRequest.class);
				// 更新最小日志级别
				this.minLoggingLevel = request.getLevel();
				// 返回成功响应（空Map）
				return CompletableFuture.completedFuture(Collections.emptyMap());
			}
			catch (Exception e) {
				// 如果处理过程中出现异常，返回错误响应
				CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
				future.completeExceptionally(new McpError("An error occurred while processing a request to set the log level: " + e.getMessage()));
				return future;
			}
		};
	}

	// ---------------------------------------
	// 采样
	// ---------------------------------------

	/**
	 * 设置支持的协议版本列表
	 *
	 * 该方法用于动态更新服务器支持的协议版本列表。
	 * 版本列表应按从低到高的顺序排列。
	 *
	 * @param protocolVersions 协议版本列表
	 */
	public void setProtocolVersions(List<String> protocolVersions) {
		this.protocolVersions = protocolVersions;
	}

}
