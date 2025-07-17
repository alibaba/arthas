package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.LoggingLevel;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.LoggingMessageNotification;
import com.taobao.arthas.mcp.server.protocol.spec.McpServerSession;
import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;



/**
 * Represents the interaction between MCP server and client. Provides methods for communication, logging, and context management.
 *
 * @author Yeaury
 * <p>
 * McpNettyServerExchange provides various methods for communicating with the client, including:
 * <ul>
 * <li>Sending requests and notifications
 * <li>Getting client capabilities and information
 * <li>Handling logging notifications
 * <li>Creating client messages
 * <li>Managing root directories
 * </ul>
 * <p>
 * Each exchange object is associated with a specific client session, providing context and capabilities for that session.
 */
public class McpNettyServerExchange {

	private static final Logger logger = LoggerFactory.getLogger(McpNettyServerExchange.class);

	private final McpServerSession session;

	private final McpSchema.ClientCapabilities clientCapabilities;

	private final McpSchema.Implementation clientInfo;

	private volatile LoggingLevel minLoggingLevel = LoggingLevel.INFO;

	private static final TypeReference<McpSchema.CreateMessageResult> CREATE_MESSAGE_RESULT_TYPE_REF =
			new TypeReference<McpSchema.CreateMessageResult>() {
	};

	private static final TypeReference<McpSchema.ListRootsResult> LIST_ROOTS_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ListRootsResult>() {
	};

	/**
	 * Create a new server exchange object.
	 * @param session Session associated with the client
	 * @param clientCapabilities Client capabilities
	 * @param clientInfo Client information
	 */
	public McpNettyServerExchange(McpServerSession session, McpSchema.ClientCapabilities clientCapabilities,
			McpSchema.Implementation clientInfo) {
		Assert.notNull(session, "Session cannot be null");
		this.session = session;
		this.clientCapabilities = clientCapabilities;
		this.clientInfo = clientInfo;
		logger.debug("Created new server exchange, session ID: {}, client: {}", session.getId(), clientInfo);
	}

	/**
	 * Create a new server exchange object. This constructor is used when the session exists but client capabilities and information are unknown.
	 * For example, for exchange objects created in request handlers.
	 * @param session Server session
	 * @param objectMapper JSON object mapper
	 */
	public McpNettyServerExchange(McpServerSession session, ObjectMapper objectMapper) {
		Assert.notNull(session, "Session cannot be null");
		this.session = session;
		this.clientCapabilities = null;
		this.clientInfo = null;
		logger.debug("Created new server exchange, session ID: {}, client info unknown", session.getId());
	}

	/**
	 * Get client capabilities.
	 * @return Client capabilities
	 */
	public McpSchema.ClientCapabilities getClientCapabilities() {
		return this.clientCapabilities;
	}

	/**
	 * Get client information.
	 * @return Client information
	 */
	public McpSchema.Implementation getClientInfo() {
		return this.clientInfo;
	}

	/**
	 * Send a notification with parameters to the client.
	 * @param method Notification method name to send to the client
	 * @param params Parameters to send with the notification
	 * @return A CompletableFuture that completes when the notification is sent
	 */
	public CompletableFuture<Void> sendNotification(String method, Object params) {
		Assert.hasText(method, "Method name cannot be empty");

		logger.debug("Sending notification to client - method: {}, session: {}", method, this.session.getId());
		return this.session.sendNotification(method, params).whenComplete((result, error) -> {
			if (error != null) {
				logger.error("Notification failed - method: {}, session: {}, error: {}", method, this.session.getId(), error.getMessage());
			}
			else {
				logger.debug("Notification sent successfully - method: {}, session: {}", method, this.session.getId());
			}
		});
	}

	/**
	 * Create a new message using client sampling capability. MCP provides a standardized way for servers to request
	 * LLM sampling ("completion" or "generation") through the client. This flow allows clients to maintain control
	 * over model access, selection, and permissions while enabling servers to leverage AI capabilities—without server
	 * API keys. Servers can request text or image-based interactions and can optionally include context from the MCP
	 * server in their prompts.
	 * @param createMessageRequest Request to create a new message
	 * @return A CompletableFuture that completes when the message is created
	 */
	public CompletableFuture<McpSchema.CreateMessageResult> createMessage(
			McpSchema.CreateMessageRequest createMessageRequest) {
		if (this.clientCapabilities == null) {
			logger.error("Client not initialized, cannot create message");
			CompletableFuture<McpSchema.CreateMessageResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Client must be initialized first. Please call initialize method!"));
			return future;
		}
		if (this.clientCapabilities.getSampling() == null) {
			logger.error("Client not configured with sampling capability, cannot create message");
			CompletableFuture<McpSchema.CreateMessageResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Client must be configured with sampling capability"));
			return future;
		}

		logger.debug("Creating client message, session ID: {}", this.session.getId());
		return this.session
			.sendRequest(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, createMessageRequest, CREATE_MESSAGE_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to create message, session ID: {}, error: {}", this.session.getId(), error.getMessage());
				}
				else {
					logger.debug("Message created successfully, session ID: {}", this.session.getId());
				}
			});
	}

	/**
	 * Get a list of all root directories provided by the client.
	 * @return Sends out the CompletableFuture of the root list result
	 */
	public CompletableFuture<McpSchema.ListRootsResult> listRoots() {
		return this.listRoots(null);
	}

	/**
	 * Get the client-provided list of pagination roots.
	 * Optional pagination cursor @param cursor for the previous list request
	 * @return Emits a CompletableFuture containing the results of the root list
	 */
	public CompletableFuture<McpSchema.ListRootsResult> listRoots(String cursor) {
		logger.debug("Requesting root list, session ID: {}, cursor: {}", this.session.getId(), cursor);
		return this.session
			.sendRequest(McpSchema.METHOD_ROOTS_LIST, new McpSchema.PaginatedRequest(cursor),
					LIST_ROOTS_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to get root list, session ID: {}, error: {}", this.session.getId(), error.getMessage());
				}
				else {
					logger.debug("Root list retrieved successfully, session ID: {}", this.session.getId());
				}
			});
	}

	public CompletableFuture<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {
		if (loggingMessageNotification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("日志消息不能为空"));
			return future;
		}

		if (this.isNotificationForLevelAllowed(loggingMessageNotification.getLevel())) {
			return this.session.sendNotification(McpSchema.METHOD_NOTIFICATION_MESSAGE, loggingMessageNotification)
				.whenComplete((result, error) -> {
					if (error != null) {
						logger.error("Failed to send logging notification, level: {}, session ID: {}, error: {}", loggingMessageNotification.getLevel(),
								this.session.getId(), error.getMessage());
					}
				});
		}
		return CompletableFuture.completedFuture(null);
	}

	public void setMinLoggingLevel(LoggingLevel minLoggingLevel) {
		Assert.notNull(minLoggingLevel, "最低日志级别不能为空");
		logger.debug("Setting minimum logging level: {}, session ID: {}", minLoggingLevel, this.session.getId());
		this.minLoggingLevel = minLoggingLevel;
	}

	private boolean isNotificationForLevelAllowed(LoggingLevel loggingLevel) {
		return loggingLevel.level() >= this.minLoggingLevel.level();
	}

}
