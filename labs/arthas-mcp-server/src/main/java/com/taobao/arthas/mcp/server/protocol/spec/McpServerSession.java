package com.taobao.arthas.mcp.server.protocol.spec;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McpServerSession implements McpSession {

	private static final Logger logger = LoggerFactory.getLogger(McpServerSession.class);

	private final ConcurrentHashMap<Object, CompletableFuture<McpSchema.JSONRPCResponse>> pendingResponses = new ConcurrentHashMap<>();

	private final String id;

	private final Duration requestTimeout;

	private final AtomicLong requestCounter = new AtomicLong(0);

	private final InitRequestHandler initRequestHandler;

	private final InitNotificationHandler initNotificationHandler;

	private final Map<String, RequestHandler<?>> requestHandlers;

	private final Map<String, NotificationHandler> notificationHandlers;

	private final McpServerTransport transport;

	private final CompletableFuture<McpNettyServerExchange> exchangeFuture = new CompletableFuture<>();

	private final AtomicReference<McpSchema.ClientCapabilities> clientCapabilities = new AtomicReference<>();

	private final AtomicReference<McpSchema.Implementation> clientInfo = new AtomicReference<>();

	private final Channel channel;

	private static final int STATE_UNINITIALIZED = 0;

	private static final int STATE_INITIALIZING = 1;

	private static final int STATE_INITIALIZED = 2;

	private final AtomicInteger state = new AtomicInteger(STATE_UNINITIALIZED);

	/**
	 * Create a new server session.
	 * @param id Session ID
	 * @param requestTimeout request timeout
	 * @param transport layer used
	 * @param initHandler handlers that handle initialization requests
	 * @param initNotificationHandler handles the handler that initializes the notification
	 * @param requestHandlers request processor mappings
	 * @param notificationHandlers notification handler mapping
	 * @param channel Netty's Channel object
	 */
	public McpServerSession(String id, Duration requestTimeout, McpServerTransport transport,
			InitRequestHandler initHandler, InitNotificationHandler initNotificationHandler,
			Map<String, RequestHandler<?>> requestHandlers, Map<String, NotificationHandler> notificationHandlers,
			Channel channel) {
		this.id = id;
		this.requestTimeout = requestTimeout;
		this.transport = transport;
		this.initRequestHandler = initHandler;
		this.initNotificationHandler = initNotificationHandler;
		this.requestHandlers = requestHandlers;
		this.notificationHandlers = notificationHandlers;
		this.channel = channel;
	}

	public String getId() {
		return this.id;
	}

	/**
	 * 在客户端和服务器之间成功初始化序列后调用，包含客户端能力和信息。
	 * @param clientCapabilities 连接的客户端提供的能力
	 * @param clientInfo 关于连接的客户端的信息
	 */
	public void init(McpSchema.ClientCapabilities clientCapabilities, McpSchema.Implementation clientInfo) {
		this.clientCapabilities.lazySet(clientCapabilities);
		this.clientInfo.lazySet(clientInfo);
	}

	private String generateRequestId() {
		return this.id + "-" + this.requestCounter.getAndIncrement();
	}

	@Override
	public <T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
		String requestId = this.generateRequestId();

		CompletableFuture<T> result = new CompletableFuture<>();
		CompletableFuture<McpSchema.JSONRPCResponse> responseFuture = new CompletableFuture<>();

		this.pendingResponses.put(requestId, responseFuture);

		McpSchema.JSONRPCRequest jsonrpcRequest = new McpSchema.JSONRPCRequest(McpSchema.JSONRPC_VERSION, method,
				requestId, requestParams);

		// 使用Netty的Channel进行异步发送
		channel.eventLoop().execute(() -> {
			transport.sendMessage(jsonrpcRequest).exceptionally(error -> {
				pendingResponses.remove(requestId);
				responseFuture.completeExceptionally(error);
				return null;
			});
		});

		// 设置超时处理
		channel.eventLoop().schedule(() -> {
			if (!responseFuture.isDone()) {
				pendingResponses.remove(requestId);
				responseFuture.completeExceptionally(
						new RuntimeException("Request timed out after " + requestTimeout.toMillis() + "ms"));
			}
		}, requestTimeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

		// 处理响应结果
		responseFuture.thenAccept(jsonRpcResponse -> {
			if (jsonRpcResponse.getError() != null) {
				result.completeExceptionally(new McpError(jsonRpcResponse.getError()));
			}
			else {
				if (typeRef.getType().equals(Void.class)) {
					result.complete(null);
				}
				else {
					result.complete(this.transport.unmarshalFrom(jsonRpcResponse.getResult(), typeRef));
				}
			}
		}).exceptionally(error -> {
			result.completeExceptionally(error);
			return null;
		});

		return result;
	}

	@Override
	public CompletableFuture<Void> sendNotification(String method, Object params) {
		McpSchema.JSONRPCNotification jsonrpcNotification = new McpSchema.JSONRPCNotification(McpSchema.JSONRPC_VERSION,
				method, params);

		return CompletableFuture.supplyAsync(() -> {
			channel.eventLoop().execute(() -> {
				transport.sendMessage(jsonrpcNotification);
			});
			return null;
		}, runnable -> channel.eventLoop().execute(runnable));
	}

	public CompletableFuture<Void> handle(McpSchema.JSONRPCMessage message) {
		CompletableFuture<Void> result = new CompletableFuture<>();

		try {
			if (message instanceof McpSchema.JSONRPCResponse) {
				handleResponse((McpSchema.JSONRPCResponse) message, result);
			}
			else if (message instanceof McpSchema.JSONRPCRequest) {
				handleRequest((McpSchema.JSONRPCRequest) message, result);
			}
			else if (message instanceof McpSchema.JSONRPCNotification) {
				handleNotification((McpSchema.JSONRPCNotification) message, result);
			}
			else {
				logger.warn("Received unknown message type: {}", message);
				result.complete(null);
			}
		}
		catch (Exception e) {
			logger.error("Error processing message", e);
			result.completeExceptionally(e);
		}

		return result;
	}

	private void handleResponse(McpSchema.JSONRPCResponse response, CompletableFuture<Void> result) {
		logger.debug("Received response: {}", response);
		CompletableFuture<McpSchema.JSONRPCResponse> sink = pendingResponses.remove(response.getId());
		if (sink == null) {
			logger.warn("Received unexpected response with unknown ID: {}", response.getId());
		}
		else {
			sink.complete(response);
		}
		result.complete(null);
	}

	private void handleRequest(McpSchema.JSONRPCRequest request, CompletableFuture<Void> result) {
		logger.debug("Received request: {}", request);
		handleIncomingRequest(request)
			.thenCompose(this.transport::sendMessage)
			.thenAccept(v -> result.complete(null))
			.exceptionally(error -> {
				McpSchema.JSONRPCResponse errorResponse = new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
						new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
								error.getMessage(), null));
				
				channel.eventLoop().execute(() -> {
					this.transport.sendMessage(errorResponse)
						.thenRun(() -> result.complete(null))
						.exceptionally(e -> {
							result.completeExceptionally(e);
							return null;
						});
				});
				return null;
			});
	}

	private void handleNotification(McpSchema.JSONRPCNotification notification, CompletableFuture<Void> result) {
		logger.debug("Received notification: {}", notification);
		handleIncomingNotification(notification)
			.thenAccept(v -> result.complete(null))
			.exceptionally(error -> {
				logger.error("Error processing notification: {}", error.getMessage());
				result.complete(null);
				return null;
			});
	}

	private CompletableFuture<McpSchema.JSONRPCResponse> handleIncomingRequest(McpSchema.JSONRPCRequest request) {
		String method = request.getMethod();
		Object params = request.getParams();

		if (McpSchema.METHOD_INITIALIZE.equals(method)) {
			if (this.state.compareAndSet(STATE_UNINITIALIZED, STATE_INITIALIZING)) {
				try {
					McpSchema.InitializeRequest initRequest = this.transport.unmarshalFrom(params,
							new TypeReference<McpSchema.InitializeRequest>() {
							});
					return this.initRequestHandler.handle(initRequest).thenApply(result -> {
						this.state.set(STATE_INITIALIZED);
						this.init(initRequest.getCapabilities(), initRequest.getClientInfo());

						if (!this.exchangeFuture.isDone()) {
							McpNettyServerExchange exchange = new McpNettyServerExchange(this,
									initRequest.getCapabilities(), initRequest.getClientInfo());
							this.exchangeFuture.complete(exchange);
						}

						return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), result, null);
					}).exceptionally(error -> {
						this.state.set(STATE_UNINITIALIZED);
						return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
								new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
										error.getMessage(), null));
					});
				}
				catch (Exception e) {
					this.state.set(STATE_UNINITIALIZED);
					CompletableFuture<McpSchema.JSONRPCResponse> future = new CompletableFuture<>();
					future.complete(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
							new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS,
									"invalid initialization parameter: " + e.getMessage(), null)));
					return future;
				}
			}
			else {
				CompletableFuture<McpSchema.JSONRPCResponse> future = new CompletableFuture<>();
				future.complete(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
						new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_REQUEST, "the session has been initialized",
								null)));
				return future;
			}
		}

		// check if the session is initialized
		if (this.state.get() != STATE_INITIALIZED) {
			CompletableFuture<McpSchema.JSONRPCResponse> future = new CompletableFuture<>();
			future.complete(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
					new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_REQUEST, "the session has not been initialized", null)));
			return future;
		}

		// handle regular requests
		RequestHandler<?> handler = this.requestHandlers.get(method);
		if (handler == null) {
			MethodNotFoundError error = getMethodNotFoundError(method);
			CompletableFuture<McpSchema.JSONRPCResponse> future = new CompletableFuture<>();
			future.complete(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
					new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.METHOD_NOT_FOUND, error.getMessage(),
							error.getData())));
			return future;
		}

		// get the swap object and process the request
		return this.exchangeFuture.thenCompose(exchange -> {
			try {
				@SuppressWarnings("unchecked")
				RequestHandler<Object> typedHandler = (RequestHandler<Object>) handler;
				return typedHandler.handle(exchange, params)
					.thenApply(result -> new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), result,
							null))
					.exceptionally(error -> {
						Throwable cause = error.getCause();
						if (cause instanceof McpError) {
							McpError mcpError = (McpError) cause;
							return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
									mcpError.getJsonRpcError());
						}
						else {
							return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
									new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
											error.getMessage(), null));
						}
					});
			}
			catch (Exception e) {
				CompletableFuture<McpSchema.JSONRPCResponse> future = new CompletableFuture<>();
				future.complete(new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, request.getId(), null,
						new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, e.getMessage(),
								null)));
				return future;
			}
		});
	}

	/**
	 * Handle incoming JSON-RPC notifications, routing them to the appropriate handler.
	 * @param notification incoming JSON-RPC notification
	 * @return indicates the CompletableFuture in which the notification processing is complete
	 */
	private CompletableFuture<Void> handleIncomingNotification(McpSchema.JSONRPCNotification notification) {
		String method = notification.getMethod();
		Object params = notification.getParams();

		if (McpSchema.METHOD_NOTIFICATION_INITIALIZED.equals(method)) {
			return this.initNotificationHandler.handle();
		}

		if (this.state.get() != STATE_INITIALIZED) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalStateException("the session has not been initialized"));
			return future;
		}

		NotificationHandler handler = this.notificationHandlers.get(method);
		if (handler == null) {
			logger.warn("No handler found for method: {}", method);
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.complete(null);
			return future;
		}

		return this.exchangeFuture.thenCompose(exchange -> handler.handle(exchange, params));
	}

	public static class MethodNotFoundError {
		private final String method;
		private final String message;
		private final Object data;

		public MethodNotFoundError(String method, String message, Object data) {
			this.method = method;
			this.message = message;
			this.data = data;
		}

		public String getMethod() {
			return method;
		}

		public String getMessage() {
			return message;
		}

		public Object getData() {
			return data;
		}
	}

	static MethodNotFoundError getMethodNotFoundError(String method) {
		Map<String, String> data = new HashMap<>();
		data.put("method", method);
		return new MethodNotFoundError(method, "method not found: " + method, data);
	}

	@Override
	public CompletableFuture<Void> closeGracefully() {
		return this.transport.closeGracefully();
	}

	@Override
	public void close() {
		this.transport.close();
	}

	public interface InitRequestHandler {

		CompletableFuture<McpSchema.InitializeResult> handle(McpSchema.InitializeRequest initializeRequest);

	}

	public interface InitNotificationHandler {

		CompletableFuture<Void> handle();

	}

	public interface NotificationHandler {

		CompletableFuture<Void> handle(McpNettyServerExchange exchange, Object params);

	}

	public interface RequestHandler<T> {

		CompletableFuture<T> handle(McpNettyServerExchange exchange, Object params);
	}

	@FunctionalInterface
	public interface Factory {

		McpServerSession create(McpServerTransport sessionTransport);
	}

}
