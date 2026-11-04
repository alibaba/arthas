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
import com.taobao.arthas.mcp.server.task.QueuedMessage;
import com.taobao.arthas.mcp.server.task.TaskDefaults;
import com.taobao.arthas.mcp.server.task.TaskMessageQueue;
import com.taobao.arthas.mcp.server.task.TaskStore;
import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

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

	private final TaskMessageQueue taskMessageQueue;

	private final TaskStore<McpSchema.ServerTaskPayloadResult> taskStore;

	private volatile LoggingLevel minLoggingLevel = LoggingLevel.INFO;

	private final AtomicLong sideChannelRequestCounter = new AtomicLong(0);

	private static final Duration SIDE_CHANNEL_TIMEOUT = Duration.ofMinutes(TaskDefaults.DEFAULT_SIDE_CHANNEL_TIMEOUT_MINUTES);

	private static final TypeReference<McpSchema.CreateMessageResult> CREATE_MESSAGE_RESULT_TYPE_REF =
			new TypeReference<McpSchema.CreateMessageResult>() {
	};

	private static final TypeReference<McpSchema.ListRootsResult> LIST_ROOTS_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ListRootsResult>() {
	};

	private static final TypeReference<McpSchema.ElicitResult> ELICIT_USER_INPUT_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ElicitResult>() {
    };
	
	private static final TypeReference<McpSchema.GetTaskResult> GET_TASK_RESULT_TYPE_REF =
			new TypeReference<McpSchema.GetTaskResult>() {
	};
	
	private static final TypeReference<McpSchema.CreateTaskResult> CREATE_TASK_RESULT_TYPE_REF =
			new TypeReference<McpSchema.CreateTaskResult>() {
	};
	
	private static final TypeReference<McpSchema.ListTasksResult> LIST_TASKS_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ListTasksResult>() {
	};
	
	private static final TypeReference<McpSchema.CancelTaskResult> CANCEL_TASK_RESULT_TYPE_REF =
			new TypeReference<McpSchema.CancelTaskResult>() {
	};

	public static final TypeReference<Object> OBJECT_TYPE_REF = new TypeReference<Object>() {
	};

	public McpNettyServerExchange(String sessionId, McpSession session,
								  McpSchema.ClientCapabilities clientCapabilities, 
								  McpSchema.Implementation clientInfo,
								  McpTransportContext transportContext,
								  TaskMessageQueue taskMessageQueue) {
		this(sessionId, session, clientCapabilities, clientInfo, transportContext, taskMessageQueue, null);
	}
	

	public McpNettyServerExchange(String sessionId, McpSession session,
								  McpSchema.ClientCapabilities clientCapabilities, 
								  McpSchema.Implementation clientInfo,
								  McpTransportContext transportContext,
								  TaskMessageQueue taskMessageQueue,
								  TaskStore<McpSchema.ServerTaskPayloadResult> taskStore) {
		this.sessionId = sessionId;
		this.session = session;
		this.clientCapabilities = clientCapabilities;
		this.clientInfo = clientInfo;
		this.transportContext = transportContext;
		this.taskMessageQueue = taskMessageQueue;
		this.taskStore = taskStore;
	}

    public CompletableFuture<Void> sendNotification(String method, Object params) {
        return session.sendNotification(method, params);
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

    public String sessionId() {
        return this.sessionId;
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
		return createMessage(createMessageRequest, null);
	}

	public CompletableFuture<McpSchema.CreateMessageResult> createMessage(
			McpSchema.CreateMessageRequest createMessageRequest,
			String taskId) {
		if (this.clientCapabilities == null) {
			logger.error("Client not initialized, cannot create message");
			CompletableFuture<McpSchema.CreateMessageResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Client must be initialized. Call the initialize method first!"));
			return future;
		}
		if (this.clientCapabilities.getSampling() == null) {
			logger.error("Client not configured with sampling capability, cannot create message");
			CompletableFuture<McpSchema.CreateMessageResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Client must be configured with sampling capabilities"));
			return future;
		}

		// Side-channel flow: enqueue request and wait for response via tasks/result
		if (taskId != null && this.taskMessageQueue != null && this.taskStore != null) {
			return sideChannelRequest(taskId, McpSchema.METHOD_SAMPLING_CREATE_MESSAGE,
					createMessageRequest, McpSchema.CreateMessageResult.class,
					"Waiting for sampling response");
		}

		// No task context: send immediately
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
		return loggingNotification(loggingMessageNotification, null);
	}

	public CompletableFuture<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification, String taskId) {
		if (loggingMessageNotification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Logging message must not be null"));
			return future;
		}

		if (this.isNotificationForLevelAllowed(loggingMessageNotification.getLevel())) {
			// Side-channel flow: enqueue notification for delivery via tasks/result
			if (taskId != null && this.taskMessageQueue != null) {
				return sideChannelNotification(taskId, McpSchema.METHOD_NOTIFICATION_MESSAGE,
						loggingMessageNotification);
			}

			return this.session.sendNotification(McpSchema.METHOD_NOTIFICATION_MESSAGE, loggingMessageNotification)
				.whenComplete((result, error) -> {
					if (error != null) {
						logger.error("Failed to send logging notification, level: {}, session ID: {}, error: {}",
								loggingMessageNotification.getLevel(), this.sessionId, error.getMessage());
					}
				});
		}
		return CompletableFuture.completedFuture(null);
	}

	public CompletableFuture<Object> ping() {
		return this.session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF);
	}


	public CompletableFuture<McpSchema.ElicitResult> createElicitation(McpSchema.ElicitRequest elicitRequest) {
		return createElicitation(elicitRequest, null);
	}

	public CompletableFuture<McpSchema.ElicitResult> createElicitation(McpSchema.ElicitRequest elicitRequest, String taskId) {
        if (elicitRequest == null) {
            CompletableFuture<McpSchema.ElicitResult> future = new CompletableFuture<>();
            future.completeExceptionally(new McpError("Elicit request must not be null"));
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

		// Side-channel flow: enqueue request and wait for response via tasks/result
		if (taskId != null && this.taskMessageQueue != null && this.taskStore != null) {
			return sideChannelRequest(taskId, McpSchema.METHOD_ELICITATION_CREATE,
					elicitRequest, McpSchema.ElicitResult.class,
					"Waiting for user input");
		}

		// No task context: send immediately
		return this.session
			.sendRequest(McpSchema.METHOD_ELICITATION_CREATE, elicitRequest, ELICIT_USER_INPUT_RESULT_TYPE_REF)
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
		return progressNotification(progressNotification, null);
	}

	public CompletableFuture<Void> progressNotification(McpSchema.ProgressNotification progressNotification, String taskId) {
		if (progressNotification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Progress notification must not be null"));
			return future;
		}

		// Side-channel flow: enqueue notification for delivery via tasks/result
		if (taskId != null && this.taskMessageQueue != null) {
			return sideChannelNotification(taskId, McpSchema.METHOD_NOTIFICATION_PROGRESS,
					progressNotification);
		}

		// Send immediately
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


	public CompletableFuture<McpSchema.GetTaskResult> getTask(McpSchema.GetTaskRequest getTaskRequest) {
		return this.session.sendRequest(McpSchema.METHOD_TASKS_GET, getTaskRequest, GET_TASK_RESULT_TYPE_REF);
	}

	public CompletableFuture<McpSchema.GetTaskResult> getTask(String taskId) {
		return this.getTask(new McpSchema.GetTaskRequest(taskId, null));
	}

	public <T> CompletableFuture<T> getTaskResult(
			McpSchema.GetTaskPayloadRequest getTaskPayloadRequest,
			TypeReference<T> resultTypeRef) {
		return this.session.sendRequest(McpSchema.METHOD_TASKS_RESULT, getTaskPayloadRequest, resultTypeRef);
	}

	public <T> CompletableFuture<T> getTaskResult(
			String taskId,
			TypeReference<T> resultTypeRef) {
		return this.getTaskResult(new McpSchema.GetTaskPayloadRequest(taskId, null), resultTypeRef);
	}

	public CompletableFuture<McpSchema.ListTasksResult> listTasks() {
		return this.listTasks(null);
	}

	public CompletableFuture<McpSchema.ListTasksResult> listTasks(String cursor) {
		return this.session.sendRequest(McpSchema.METHOD_TASKS_LIST, 
				new McpSchema.PaginatedRequest(cursor), 
				LIST_TASKS_RESULT_TYPE_REF);
	}

	public CompletableFuture<McpSchema.CancelTaskResult> cancelTask(McpSchema.CancelTaskRequest cancelTaskRequest) {
		return this.session.sendRequest(McpSchema.METHOD_TASKS_CANCEL, cancelTaskRequest, CANCEL_TASK_RESULT_TYPE_REF);
	}

	public CompletableFuture<McpSchema.CancelTaskResult> cancelTask(String taskId) {
		Assert.notNull(taskId, "Task ID must not be null");
		if (taskId.trim().isEmpty()) {
			CompletableFuture<McpSchema.CancelTaskResult> future = new CompletableFuture<>();
			future.completeExceptionally(new IllegalArgumentException("Task ID must not be empty"));
			return future;
		}
		return cancelTask(new McpSchema.CancelTaskRequest(taskId, null));
	}

	// === Side-Channel Helpers ===

	@SuppressWarnings("unchecked")
	private <T extends McpSchema.Result> CompletableFuture<T> sideChannelRequest(
			String taskId, String method, McpSchema.Request request,
			Class<T> resultType, String inputMessage) {

		String requestId = "sc-" + this.sessionId + "-" + this.sideChannelRequestCounter.getAndIncrement();

		logger.debug("Side-channel request: taskId={}, method={}, requestId={}", taskId, method, requestId);

		// 1. Enqueue the request for the side-channel handler to pick up
		QueuedMessage.Request queuedRequest = new QueuedMessage.Request(requestId, method, request);

		return this.taskMessageQueue.enqueue(taskId, queuedRequest)
				.thenCompose(v -> {
					// 2. Set task to INPUT_REQUIRED so client polls tasks/result
					return this.taskStore.updateTaskStatus(taskId, this.sessionId,
							McpSchema.TaskStatus.INPUT_REQUIRED, inputMessage);
				})
				.thenCompose(v -> {
					// 3. Wait for the response to arrive via the queue
					return this.taskMessageQueue.waitForResponse(taskId, requestId, SIDE_CHANNEL_TIMEOUT);
				})
				.thenCompose(response -> {
					// 4. Restore task to WORKING status
					return this.taskStore.updateTaskStatus(taskId, this.sessionId,
									McpSchema.TaskStatus.WORKING, null)
							.thenApply(v -> (T) response.result());
				});
	}

	private CompletableFuture<Void> sideChannelNotification(String taskId, String method, Object notification) {
		logger.debug("Side-channel notification: taskId={}, method={}", taskId, method);
		QueuedMessage.Notification queuedNotification = new QueuedMessage.Notification(method, notification);
		return this.taskMessageQueue.enqueue(taskId, queuedNotification);
	}
}
