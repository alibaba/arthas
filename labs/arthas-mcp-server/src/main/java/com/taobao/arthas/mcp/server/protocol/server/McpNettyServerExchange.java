/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.LoggingLevel;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.LoggingMessageNotification;
import com.taobao.arthas.mcp.server.protocol.spec.McpSession;
import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the interaction between MCP server and client. Provides methods for communication, logging, and context management.
 * This class is focused only on MCP protocol communication and does not handle command execution directly.
 *
 * <p>
 * McpNettyServerExchange provides various methods for communicating with the client, including:
 * <ul>
 * <li>Sending requests and notifications
 * <li>Getting client capabilities and information
 * <li>Handling logging notifications
 * <li>Creating client messages
 * <li>Managing root directories
 * <li>Streamable task management
 * </ul>
 * <p>
 * Each exchange object is associated with a specific client session, providing context and capabilities for that session.
 */
public class McpNettyServerExchange {

	private static final Logger logger = LoggerFactory.getLogger(McpNettyServerExchange.class);

	private final String sessionId;

	private final McpSession session;

	private final McpSchema.ClientCapabilities clientCapabilities;

	private final McpSchema.Implementation clientInfo;

	private final McpTransportContext transportContext;

	private volatile LoggingLevel minLoggingLevel = LoggingLevel.INFO;

	private static final TypeReference<McpSchema.CreateMessageResult> CREATE_MESSAGE_RESULT_TYPE_REF =
			new TypeReference<McpSchema.CreateMessageResult>() {
	};

	private static final TypeReference<McpSchema.ListRootsResult> LIST_ROOTS_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ListRootsResult>() {
	};

	private static final TypeReference<McpSchema.ElicitResult> ELICIT_USER_INPUT_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ElicitResult>() {
    };

	public static final TypeReference<Object> OBJECT_TYPE_REF = new TypeReference<Object>() {
	};

	public McpNettyServerExchange(String sessionId, McpSession session,
								  McpSchema.ClientCapabilities clientCapabilities, McpSchema.Implementation clientInfo,
								  McpTransportContext transportContext) {
		this.sessionId = sessionId;
		this.session = session;
		this.clientCapabilities = clientCapabilities;
		this.clientInfo = clientInfo;
		this.transportContext = transportContext;
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
	 * Get the MCP server session associated with this exchange.
	 * @return The MCP server session
	 */
	public McpSession getSession() {
		return this.session;
	}

	/**
	 * Get the transport context associated with this exchange.
	 * @return The transport context
	 */
	public McpTransportContext getTransportContext() {
		return this.transportContext;
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

		logger.debug("Creating client message, session ID: {}", this.sessionId);
		return this.session
			.sendRequest(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, createMessageRequest, CREATE_MESSAGE_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to create message, session ID: {}, error: {}", this.sessionId, error.getMessage());
				}
				else {
					logger.debug("Message created successfully, session ID: {}", this.sessionId);
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
		logger.debug("Requesting root list, session ID: {}, cursor: {}", this.sessionId, cursor);
		return this.session
			.sendRequest(McpSchema.METHOD_ROOTS_LIST, new McpSchema.PaginatedRequest(cursor),
					LIST_ROOTS_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to get root list, session ID: {}, error: {}", this.sessionId, error.getMessage());
				}
				else {
					logger.debug("Root list retrieved successfully, session ID: {}", this.sessionId);
				}
			});
	}

	public CompletableFuture<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {
		if (loggingMessageNotification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("log messages cannot be empty"));
			return future;
		}

		if (this.isNotificationForLevelAllowed(loggingMessageNotification.getLevel())) {
			return this.session.sendNotification(McpSchema.METHOD_NOTIFICATION_MESSAGE, loggingMessageNotification)
				.whenComplete((result, error) -> {
					if (error != null) {
						logger.error("Failed to send logging notification, level: {}, session ID: {}, error: {}", loggingMessageNotification.getLevel(),
								this.sessionId, error.getMessage());
					}
				});
		}
		return CompletableFuture.completedFuture(null);
	}

	public CompletableFuture<Object> ping() {
		return this.session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF);
	}

	public CompletableFuture<McpSchema.ElicitResult> createElicitation(McpSchema.ElicitRequest request) {
        if (request == null) {
            CompletableFuture<McpSchema.ElicitResult> future = new CompletableFuture<>();
            future.completeExceptionally(new McpError("elicit request cannot be null"));
            return future;
        }
        if (this.clientCapabilities == null) {
            CompletableFuture<McpSchema.ElicitResult> future = new CompletableFuture<>();
            future.completeExceptionally(new McpError("Client must be initialized. Call the initialize method first!"));
            return future;
        }
        if (this.clientCapabilities.getElicitation() == null) {
            CompletableFuture<McpSchema.ElicitResult> future = new CompletableFuture<>();
            future.completeExceptionally(new McpError("Client must be configured with elicitation capabilities"));
            return future;
        }
		return this.session
			.sendRequest(McpSchema.METHOD_ELICITATION_CREATE, request, ELICIT_USER_INPUT_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to elicit user input, session ID: {}, error: {}", this.sessionId, error.getMessage());
				} else {
					logger.debug("User input elicitation completed, session ID: {}", this.sessionId);
				}
			});
	}

	public void setMinLoggingLevel(LoggingLevel minLoggingLevel) {
		Assert.notNull(minLoggingLevel, "the minimum log level cannot be empty");
		logger.debug("Setting minimum logging level: {}, session ID: {}", minLoggingLevel, this.sessionId);
		this.minLoggingLevel = minLoggingLevel;
	}

	private boolean isNotificationForLevelAllowed(LoggingLevel loggingLevel) {
		return loggingLevel.level() >= this.minLoggingLevel.level();
	}

	public CompletableFuture<Void> progressNotification(McpSchema.ProgressNotification progressNotification) {
		if (progressNotification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("progress notifications cannot be empty"));
			return future;
		}

		return this.session
				.sendNotification(McpSchema.METHOD_NOTIFICATION_PROGRESS, progressNotification)
				.whenComplete((result, error) -> {
					if (error != null) {
						logger.error("Failed to send progress notification, session ID: {}, error: {}", this.sessionId, error.getMessage());
					} else {
						logger.debug("Progress notification sent successfully, session ID: {}", this.sessionId);
					}
				});
	}
}
