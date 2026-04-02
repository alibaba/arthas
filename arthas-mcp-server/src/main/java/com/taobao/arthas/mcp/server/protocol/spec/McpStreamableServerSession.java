/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.server.McpNotificationHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.MCP_AUTH_SUBJECT_KEY;

/**
 * 可流式MCP服务端会话的主要实现类
 *
 * <p>该类实现了McpSession接口，管理与客户端之间的JSON-RPC通信。
 * 它支持可流式传输，通过SSE（Server-Sent Events）实现服务器向客户端的持续消息推送。
 *
 * <p>主要功能：
 * <ul>
 *   <li>管理会话状态和生命周期</li>
 *   <li>发送JSON-RPC请求和接收响应</li>
 *   <li>发送单向通知</li>
 *   <li>处理来自客户端的请求和通知</li>
 *   <li>支持事件重放（用于断点续传）</li>
 *   <li>管理Arthas命令执行上下文</li>
 *   <li>实现日志级别控制</li>
 * </ul>
 *
 * <p>该类使用了CompletableFuture来实现异步操作，确保在高并发场景下的性能。
 *
 * @author Yeaury
 */
public class McpStreamableServerSession implements McpSession {

	// 日志记录器，用于记录会话操作和调试信息
	private static final Logger logger = LoggerFactory.getLogger(McpStreamableServerSession.class);

	// 请求ID到流的映射，用于跟踪每个请求对应的流
	private final ConcurrentHashMap<Object, McpStreamableServerSessionStream> requestIdToStream = new ConcurrentHashMap<>();

	// 会话唯一标识符
	private final String id;

	// 请求超时时长
	private final Duration requestTimeout;

	// 请求计数器，用于生成唯一的请求ID
	private final AtomicLong requestCounter = new AtomicLong(0);

	// 方法名到请求处理器的映射，用于处理客户端的请求
	private final Map<String, McpRequestHandler<?>> requestHandlers;

	// 方法名到通知处理器的映射，用于处理客户端的通知
	private final Map<String, McpNotificationHandler> notificationHandlers;

	// 客户端能力声明，使用原子引用确保线程安全
	private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();

	// 客户端信息（名称、版本等），使用原子引用确保线程安全
	private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();

	// 当前监听流的引用，使用原子引用确保线程安全的流切换
	private final AtomicReference<McpSession> listeningStreamRef;

	// 缺失传输会话的占符对象，用于没有活动流时
	private final MissingMcpTransportSession missingMcpTransportSession;

	// 最小日志级别，只有等于或高于此级别的日志才会发送给客户端
	private volatile McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.INFO;

	// 命令执行器，用于执行Arthas命令
	private final CommandExecutor commandExecutor;

	// Arthas命令会话管理器，管理命令执行的会话
	private final ArthasCommandSessionManager commandSessionManager;

	// 事件存储，用于存储和重放会话事件
	private final EventStore eventStore;

	/**
	 * 构造一个新的可流式MCP服务端会话
	 *
	 * @param id               会话唯一标识符
	 * @param clientCapabilities 客户端能力声明
	 * @param clientInfo       客户端信息
	 * @param requestTimeout   请求超时时长
	 * @param requestHandlers  请求处理器映射表
	 * @param notificationHandlers 通知处理器映射表
	 * @param commandExecutor  命令执行器
	 * @param eventStore       事件存储
	 */
	public McpStreamableServerSession(String id, McpSchema.ClientCapabilities clientCapabilities,
									  McpSchema.Implementation clientInfo, Duration requestTimeout,
									  Map<String, McpRequestHandler<?>> requestHandlers,
									  Map<String, McpNotificationHandler> notificationHandlers,
									  CommandExecutor commandExecutor, EventStore eventStore) {
		this.id = id;
		// 创建缺失传输会话的占符对象
		this.missingMcpTransportSession = new MissingMcpTransportSession(id);
		// 初始状态下，监听流设置为缺失传输会话
		this.listeningStreamRef = new AtomicReference<>(this.missingMcpTransportSession);
		// 使用lazySet设置客户端能力，避免volatile写开销
		this.clientCapabilities.lazySet(clientCapabilities);
		this.clientInfo.lazySet(clientInfo);
		this.requestTimeout = requestTimeout;
		this.requestHandlers = requestHandlers;
		this.notificationHandlers = notificationHandlers;
		this.commandExecutor = commandExecutor;
		// 创建命令会话管理器
		this.commandSessionManager = new ArthasCommandSessionManager(commandExecutor);
		this.eventStore = eventStore;
	}

	/**
	 * 设置会话的最小日志级别
	 *
	 * <p>只有等于或高于此级别的日志消息才会发送给客户端。
	 * 例如，如果设置为WARNING，则DEBUG和INFO级别的日志不会被发送。
	 *
	 * @param minLoggingLevel 最小日志级别，必须不为null
	 * @throws IllegalArgumentException 如果minLoggingLevel为null
	 */
	public void setMinLoggingLevel(McpSchema.LoggingLevel minLoggingLevel) {
		Assert.notNull(minLoggingLevel, "minLoggingLevel must not be null");
		this.minLoggingLevel = minLoggingLevel;
	}

	/**
	 * 检查是否允许发送指定级别的日志通知
	 *
	 * @param loggingLevel 要检查的日志级别
	 * @return 如果允许发送该级别的日志返回true，否则返回false
	 */
	public boolean isNotificationForLevelAllowed(McpSchema.LoggingLevel loggingLevel) {
		// 比较日志级别数值，数值越大级别越高
		return loggingLevel.level() >= this.minLoggingLevel.level();
	}

	/**
	 * 获取会话ID
	 *
	 * @return 会话唯一标识符
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * 生成唯一的请求ID
	 *
	 * <p>请求ID格式为："{sessionId}-{counter}"，确保每个请求都有唯一标识。
	 *
	 * @return 唯一的请求ID字符串
	 */
	private String generateRequestId() {
		return this.id + "-" + this.requestCounter.getAndIncrement();
	}

	/**
	 * 向客户端发送JSON-RPC请求
	 *
	 * <p>该方法通过当前监听的流向客户端发送请求，并异步等待响应。
	 *
	 * @param <T>           响应结果的类型
	 * @param method        JSON-RPC方法名
	 * @param requestParams 请求参数对象
	 * @param typeRef       响应类型的TypeReference
	 * @return CompletableFuture，完成时包含类型化的响应结果
	 */
	@Override
	public <T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
		// 获取当前监听的流
		McpSession listeningStream = this.listeningStreamRef.get();
		// 委托给监听流发送请求
		return listeningStream.sendRequest(method, requestParams, typeRef);
	}

	/**
	 * 向客户端发送通知
	 *
	 * <p>该方法通过当前监听的流向客户端发送单向通知。
	 *
	 * @param method 通知方法名
	 * @param params 通知参数对象
	 * @return CompletableFuture，当通知发送完成后完成
	 */
	@Override
	public CompletableFuture<Void> sendNotification(String method, Object params) {
		// 获取当前监听的流
		McpSession listeningStream = this.listeningStreamRef.get();
		// 委托给监听流发送通知
		return listeningStream.sendNotification(method, params);
	}

	/**
	 * 删除会话并清理相关资源
	 *
	 * <p>该方法会：
	 * <ul>
	 *   <li>优雅关闭会话</li>
	 *   <li>移除事件存储中的所有会话事件</li>
	 *   <li>关闭Arthas命令会话</li>
	 * </ul>
	 *
	 * @return CompletableFuture，当所有清理操作完成后完成
	 */
	public CompletableFuture<Void> delete() {
		// 首先优雅关闭会话
		return this.closeGracefully().thenRun(() -> {
			try {
				// 从事件存储中移除该会话的所有事件
				eventStore.removeSessionEvents(this.id);
				// 关闭Arthas命令会话
				commandSessionManager.closeCommandSession(this.id);
			} catch (Exception e) {
				// 清理失败时记录警告，但不中断操作
				logger.warn("Failed to clear session during deletion: {}", e.getMessage());
			}
		});
	}

	/**
	 * 创建并设置新的监听流
	 *
	 * <p>该方法创建一个新的会话流，并将其设置为当前监听流。
	 * 监听流用于向客户端发送消息。
	 *
	 * @param transport 传输层对象
	 * @return 新创建的会话流
	 */
	public McpStreamableServerSessionStream listeningStream(McpStreamableServerTransport transport) {
		// 创建新的会话流
		McpStreamableServerSessionStream listeningStream = new McpStreamableServerSessionStream(transport);
		// 设置为当前监听流
		this.listeningStreamRef.set(listeningStream);
		return listeningStream;
	}

	/**
	 * 重播会话事件，从指定的最后事件ID之后开始
	 *
	 * <p>该方法用于实现断点续传功能。当客户端重连时，可以从上次断开的位置继续接收事件。
	 *
	 * @param lastEventId 最后一个事件ID，如果为null则从头开始重播
	 * @return 事件消息流
	 */
	public Stream<McpSchema.JSONRPCMessage> replay(Object lastEventId) {
		// 将事件ID转换为字符串
		String lastEventIdStr = lastEventId != null ? lastEventId.toString() : null;

		// 从事件存储中获取事件流，并提取消息
		return eventStore.getEventsForSession(this.id, lastEventIdStr)
				.map(EventStore.StoredEvent::getMessage);
	}

	/**
	 * 处理响应流请求
	 *
	 * <p>该方法处理来自客户端的请求，并返回响应。响应通过流式传输发送给客户端。
	 *
	 * @param jsonrpcRequest  JSON-RPC请求对象
	 * @param transport       传输层对象
	 * @param transportContext 传输上下文，包含认证信息等
	 * @return CompletableFuture，当请求处理完成并关闭流后完成
	 */
	public CompletableFuture<Void> responseStream(McpSchema.JSONRPCRequest jsonrpcRequest,
			McpStreamableServerTransport transport, McpTransportContext transportContext) {

		// 创建一个新的会话流用于发送响应
		McpStreamableServerSessionStream stream = new McpStreamableServerSessionStream(transport);
		// 获取请求处理器
		McpRequestHandler<?> requestHandler = this.requestHandlers.get(jsonrpcRequest.getMethod());

		// 如果没有找到对应的处理器，返回方法未找到错误
		if (requestHandler == null) {
			// 创建方法未找到错误
			MethodNotFoundError error = getMethodNotFoundError(jsonrpcRequest.getMethod());
			McpSchema.JSONRPCResponse errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION,
					jsonrpcRequest.getId(), null,
					new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND,
							error.getMessage(), error.getData()));

			// 存储错误响应到事件存储
			try {
				eventStore.storeEvent(this.id, errorResponse);
			} catch (Exception e) {
				logger.warn("Failed to store error response event: {}", e.getMessage());
			}

			// 发送错误响应并关闭流
			return transport.sendMessage(errorResponse, null)
					.thenCompose(v -> transport.closeGracefully());
		}
		// 从传输上下文中获取认证主题，创建命令执行上下文
		ArthasCommandContext commandContext = createCommandContext(transportContext.get(MCP_AUTH_SUBJECT_KEY));

		// 使用处理器处理请求
		return requestHandler
				.handle(new McpNettyServerExchange(this.id, stream, clientCapabilities.get(),
						clientInfo.get(), transportContext), commandContext, jsonrpcRequest.getParams())
				.thenApply(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION,
						jsonrpcRequest.getId(), result, null))
				.thenCompose(response -> transport.sendMessage(response, null))
				.thenCompose(v -> transport.closeGracefully());
	}

	/**
	 * 接受并处理来自客户端的通知
	 *
	 * @param notification      JSON-RPC通知对象
	 * @param transportContext  传输上下文
	 * @return CompletableFuture，当通知处理完成后完成
	 */
	public CompletableFuture<Void> accept(McpSchema.JSONRPCNotification notification,
			McpTransportContext transportContext) {

		// 获取通知处理器
		McpNotificationHandler notificationHandler = this.notificationHandlers.get(notification.getMethod());
		// 如果没有找到对应的处理器，记录错误并返回
		if (notificationHandler == null) {
			logger.error("No handler registered for notification method: {}", notification.getMethod());
			return CompletableFuture.completedFuture(null);
		}

		// 从传输上下文中获取认证主题，创建命令执行上下文
		ArthasCommandContext commandContext = createCommandContext(transportContext.get(MCP_AUTH_SUBJECT_KEY));
		// 获取当前监听流
		McpSession listeningStream = this.listeningStreamRef.get();
		// 使用处理器处理通知
		return notificationHandler.handle(new McpNettyServerExchange(this.id, listeningStream,
				this.clientCapabilities.get(), this.clientInfo.get(), transportContext), commandContext, notification.getParams());
	}

	/**
	 * 接受并处理来自客户端的响应
	 *
	 * @param response JSON-RPC响应对象
	 * @return CompletableFuture，当响应处理完成后完成
	 */
	public CompletableFuture<Void> accept(McpSchema.JSONRPCResponse response) {
		// 从映射中获取请求对应的流
		McpStreamableServerSessionStream stream = this.requestIdToStream.get(response.getId());
		// 如果没有找到对应的流，返回错误
		if (stream == null) {
			CompletableFuture<Void> f = CompletableFuture.completedFuture(null);
			f.completeExceptionally(new McpError("Unexpected response for unknown id " + response.getId()));
			return f;
		}

		// 从流中移除并完成对应的等待Future
		CompletableFuture<McpSchema.JSONRPCResponse> future = stream.pendingResponses.remove(response.getId());
		if (future == null) {
			// 如果没有找到等待的Future，返回错误
			CompletableFuture<Void> f = CompletableFuture.completedFuture(null);
			f.completeExceptionally(new McpError("Unexpected response for unknown id " + response.getId()));
			return f;
		} else {
			// 完成等待的Future
			future.complete(response);
		}

		return CompletableFuture.completedFuture(null);
	}

	/**
	 * 方法未找到错误类
	 *
	 * <p>该类封装了方法未找到错误的相关信息，包括方法名、错误消息和额外数据。
	 */
	public class MethodNotFoundError {
		// 未找到的方法名
		private final String method;
		// 错误消息
		private final String message;
		// 额外错误数据
		private final Object data;

		/**
		 * 构造一个新的方法未找到错误对象
		 *
		 * @param method  未找到的方法名
		 * @param message 错误消息
		 * @param data    额外错误数据
		 */
		public MethodNotFoundError(String method, String message, Object data) {
			this.method = method;
			this.message = message;
			this.data = data;
		}

		/**
		 * 获取未找到的方法名
		 *
		 * @return 方法名
		 */
		public String getMethod() {
			return method;
		}

		/**
		 * 获取错误消息
		 *
		 * @return 错误消息
		 */
		public String getMessage() {
			return message;
		}

		/**
		 * 获取额外错误数据
		 *
		 * @return 错误数据
		 */
		public Object getData() {
			return data;
		}
	}


	/**
	 * 获取方法未找到错误对象
	 *
	 * @param method 未找到的方法名
	 * @return 方法未找到错误对象
	 */
	private MethodNotFoundError getMethodNotFoundError(String method) {
		return new MethodNotFoundError(method, "Method not found: " + method, null);
	}

	/**
	 * 优雅地关闭会话
	 *
	 * <p>该方法会：
	 * <ul>
	 *   <li>将监听流替换为缺失传输会话</li>
	 *   <li>关闭Arthas命令会话</li>
	 *   <li>优雅关闭原来的监听流</li>
	 * </ul>
	 *
	 * @return CompletableFuture，当所有关闭操作完成后完成
	 */
	@Override
	public CompletableFuture<Void> closeGracefully() {
		// 获取当前监听流，并替换为缺失传输会话
		McpSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);

		// 清理 Arthas 命令会话
		try {
			commandSessionManager.closeCommandSession(this.id);
			logger.debug("Successfully closed command session during graceful shutdown: {}", this.id);
		} catch (Exception e) {
			logger.warn("Failed to close command session during graceful shutdown: {}", e.getMessage());
		}

		// 优雅关闭原来的监听流
		return listeningStream.closeGracefully();
		// TODO: 同时关闭所有打开的流
	}

	/**
	 * 立即关闭会话
	 *
	 * <p>该方法会：
	 * <ul>
	 *   <li>将监听流替换为缺失传输会话</li>
	 *   <li>关闭Arthas命令会话</li>
	 *   <li>立即关闭原来的监听流</li>
	 * </ul>
	 */
	@Override
	public void close() {
		// 获取当前监听流，并替换为缺失传输会话
		McpSession listeningStream = this.listeningStreamRef.getAndSet(missingMcpTransportSession);

		// 清理 Arthas 命令会话
		try {
			commandSessionManager.closeCommandSession(this.id);
			logger.debug("Successfully closed command session during close: {}", this.id);
		} catch (Exception e) {
			logger.warn("Failed to close command session during close: {}", e.getMessage());
		}

		// 立即关闭原来的监听流
		if (listeningStream != null) {
			listeningStream.close();
		}
		// TODO: 同时关闭所有打开的流
	}

	/**
	 * 会话工厂接口
	 *
	 * <p>该接口定义了创建新会话的方法，用于处理客户端的初始化请求。
	 */
	public interface Factory {
		/**
		 * 启动一个新的会话
		 *
		 * @param initializeRequest 初始化请求对象
		 * @return 包含新会话和初始化结果的对象
		 */
		McpStreamableServerSessionInit startSession(McpSchema.InitializeRequest initializeRequest);
	}

	/**
	 * 可流式服务端会话初始化结果
	 *
	 * <p>该类封装了会话初始化的结果，包括新创建的会话和异步初始化结果。
	 */
	public static class McpStreamableServerSessionInit {
		// 新创建的会话
		private final McpStreamableServerSession session;
		// 异步初始化结果
		private final CompletableFuture<McpSchema.InitializeResult> initResult;

		/**
		 * 构造一个新的初始化结果对象
		 *
		 * @param session    新创建的会话
		 * @param initResult 异步初始化结果
		 */
		public McpStreamableServerSessionInit(
				McpStreamableServerSession session,
				CompletableFuture<McpSchema.InitializeResult> initResult) {
			this.session = session;
			this.initResult = initResult;
		}

		/**
		 * 获取会话对象
		 *
		 * @return 会话对象
		 */
		public McpStreamableServerSession session() {
			return session;
		}

		/**
		 * 获取初始化结果
		 *
		 * @return 异步初始化结果
		 */
		public CompletableFuture<McpSchema.InitializeResult> initResult() {
			return initResult;
		}
	}


	/**
	 * 可流式服务端会话流
	 *
	 * <p>该内部类表示一个具体的流，用于与客户端进行通信。
	 * 每个流都有自己的待响应映射和传输层。
	 *
	 * <p>主要功能：
	 * <ul>
	 *   <li>管理待处理的响应</li>
	 *   <li>生成唯一的消息ID</li>
	 *   <li>发送请求和通知</li>
	 *   <li>处理流的关闭</li>
	 * </ul>
	 */
	public final class McpStreamableServerSessionStream implements McpSession {

		// 待处理的响应映射，请求ID到对应的Future
		private final ConcurrentHashMap<Object, CompletableFuture<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();

		// 底层传输层对象
		private final McpStreamableServerTransport transport;

		// 传输层唯一标识符，用于生成消息ID
		private final String transportId;

		// UUID生成器，用于生成消息ID
		private final Supplier<String> uuidGenerator;

		/**
		 * 构造一个新的会话流
		 *
		 * @param transport 传输层对象
		 */
		public McpStreamableServerSessionStream(McpStreamableServerTransport transport) {
			this.transport = transport;
			// 生成传输层唯一标识符
			this.transportId = UUID.randomUUID().toString();
			// 该ID设计允许通过第一个组件精确标识SSE流，实现常量时间的历史记录提取
			this.uuidGenerator = () -> this.transportId + "_" + UUID.randomUUID();
		}

		/**
		 * 向客户端发送请求
		 *
		 * @param <T>           响应结果的类型
		 * @param method        JSON-RPC方法名
		 * @param requestParams 请求参数对象
		 * @param typeRef       响应类型的TypeReference
		 * @return CompletableFuture，完成时包含类型化的响应结果
		 */
		@Override
		public <T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
			// 生成唯一的请求ID
			String requestId = McpStreamableServerSession.this.generateRequestId();

			// 将请求ID映射到当前流
			McpStreamableServerSession.this.requestIdToStream.put(requestId, this);

			// 创建响应Future
			CompletableFuture<McpSchema.JSONRPCResponse> responseFuture = new CompletableFuture<>();
			this.pendingResponses.put(requestId, responseFuture);

			// 创建JSON-RPC请求对象
			McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION,
					method, requestId, requestParams);
			String messageId = null;

			// 存储发送的请求到事件存储
			try {
				messageId = McpStreamableServerSession.this.eventStore.storeEvent(
					McpStreamableServerSession.this.id, jsonrpcRequest);
			} catch (Exception e) {
				logger.warn("Failed to store outbound request event: {}", e.getMessage());
			}

			// 发送消息
			this.transport.sendMessage(jsonrpcRequest, messageId).exceptionally(ex -> {
				// 发送失败时完成Future
				responseFuture.completeExceptionally(ex);
				return null;
			});

			// 处理响应
			return responseFuture.handle((jsonRpcResponse, throwable) -> {
				// 清理：移除映射
				this.pendingResponses.remove(requestId);
				McpStreamableServerSession.this.requestIdToStream.remove(requestId);

				// 如果有异常，抛出运行时异常
				if (throwable != null) {
					if (throwable instanceof RuntimeException) {
						throw (RuntimeException) throwable;
					}
					throw new RuntimeException(throwable);
				}

				// 如果响应包含错误，抛出MCP错误
				if (jsonRpcResponse.getError() != null) {
					throw new RuntimeException(new McpError(jsonRpcResponse.getError()));
				} else {
					// 如果返回类型是Void，返回null
					if (typeRef.getType().equals(Void.class)) {
						return null;
					} else {
						// 否则反序列化结果
						return this.transport.unmarshalFrom(jsonRpcResponse.getResult(), typeRef);
					}
				}
			});
		}

		/**
		 * 向客户端发送通知
		 *
		 * @param method 通知方法名
		 * @param params 通知参数对象
		 * @return CompletableFuture，当通知发送完成后完成
		 */
		@Override
		public CompletableFuture<Void> sendNotification(String method, Object params) {
			// 创建JSON-RPC通知对象
			McpSchema.JSONRPCNotification jsonrpcNotification = new McpSchema.JSONRPCNotification(
					McpSchema.JSONRPC_VERSION, method, params);
			String messageId = null;
			// 存储发送的通知到事件存储
			try {
				messageId = McpStreamableServerSession.this.eventStore.storeEvent(
						McpStreamableServerSession.this.id, jsonrpcNotification);
			} catch (Exception e) {
				logger.warn("Failed to store outbound notification event: {}", e.getMessage());
			}

			// 发送通知
			return this.transport.sendMessage(jsonrpcNotification, messageId);
		}

		/**
		 * 优雅地关闭流
		 *
		 * @return CompletableFuture，当所有关闭操作完成后完成
		 */
		@Override
		public CompletableFuture<Void> closeGracefully() {
			// 完成所有待处理的响应，标记为异常
			this.pendingResponses.values().forEach(future ->
					future.completeExceptionally(new RuntimeException("Stream closed")));
			this.pendingResponses.clear();

			// 如果这是通用流，重置它
			McpStreamableServerSession.this.listeningStreamRef.compareAndSet(this,
					McpStreamableServerSession.this.missingMcpTransportSession);

			// 移除所有指向此流的请求映射
			McpStreamableServerSession.this.requestIdToStream.values().removeIf(this::equals);

			// 优雅关闭传输层
			return this.transport.closeGracefully();
		}

		/**
		 * 立即关闭流
		 */
		@Override
		public void close() {
			// 完成所有待处理的响应，标记为异常
			this.pendingResponses.values().forEach(future ->
					future.completeExceptionally(new RuntimeException("Stream closed")));
			this.pendingResponses.clear();

			// 如果这是通用流，重置它
			McpStreamableServerSession.this.listeningStreamRef.compareAndSet(this,
					McpStreamableServerSession.this.missingMcpTransportSession);
			// 移除所有指向此流的请求映射
			McpStreamableServerSession.this.requestIdToStream.values().removeIf(this::equals);

			// 立即关闭传输层
			this.transport.close();
		}


	}

	/**
	 * 创建命令执行上下文
	 *
	 * <p>该方法根据认证主题获取或创建命令会话绑定，
	 * 然后创建命令执行上下文。
	 *
	 * @param authSubject 认证主题对象
	 * @return 命令执行上下文
	 */
	private ArthasCommandContext createCommandContext(Object authSubject) {
		// 获取命令会话绑定
		ArthasCommandSessionManager.CommandSessionBinding binding = commandSessionManager.getCommandSession(this.id, authSubject);
		// 创建并返回命令执行上下文
		return new ArthasCommandContext(commandExecutor, binding);
	}
}
