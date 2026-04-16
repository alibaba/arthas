/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.task;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.server.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpTransportContext;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Manages task-aware tool registration and task lifecycle on the server side.
 *
 * @see TaskManager
 * @see TaskManagerHost
 */
public class ServerTaskToolHandler extends AbstractTaskHandler<McpSchema.ServerTaskPayloadResult> {

    private static final Logger logger = LoggerFactory.getLogger(ServerTaskToolHandler.class);

    private final ObjectMapper objectMapper;
    private final TaskManagerOptions taskOptions;
    private final Duration automaticPollingTimeout;
    private final ArthasCommandSessionManager sessionManager;

    private final CopyOnWriteArrayList<TaskAwareToolSpecification> taskTools = new CopyOnWriteArrayList<>();

    private final ConcurrentHashMap<String, TaskAwareToolSpecification> taskToolsByName = new ConcurrentHashMap<>();

    private final Object toolRegistrationLock = new Object();

    /** Notifies all connected clients of a method/params pair. */
    private final BiFunction<String, Object, CompletableFuture<Void>> clientNotifier;

    @SuppressWarnings("unchecked")
    public ServerTaskToolHandler(
            List<TaskAwareToolSpecification> taskTools,
            TaskManagerOptions taskOptions,
            ObjectMapper objectMapper,
            BiFunction<String, Object, CompletableFuture<Void>> clientNotifier,
            Duration automaticPollingTimeout,
            ArthasCommandSessionManager sessionManager) {
        
        super(
            taskOptions != null ? (TaskStore<McpSchema.ServerTaskPayloadResult>) taskOptions.taskStore() : null,
            taskOptions
        );

        this.objectMapper = objectMapper;
        this.clientNotifier = clientNotifier;
        this.automaticPollingTimeout = automaticPollingTimeout;
        this.taskOptions = taskOptions;
        this.sessionManager = sessionManager;

        this.taskTools.addAll(taskTools);
        for (TaskAwareToolSpecification taskTool : taskTools) {
            this.taskToolsByName.put(taskTool.tool().getName(), taskTool);
        }
    }

    // ---------------------------------------
    // Task Tool Registration
    // ---------------------------------------

    public CompletableFuture<Void> addTaskTool(
            TaskAwareToolSpecification taskToolSpecification,
            McpSchema.ServerCapabilities.ToolCapabilities toolCapabilities) {
        
        if (taskToolSpecification == null) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalArgumentException("Task tool specification must not be null"));
            return f;
        }
        if (taskToolSpecification.tool() == null) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalArgumentException("Tool must not be null"));
            return f;
        }
        if (taskToolSpecification.createTaskHandler() == null) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalArgumentException("createTask handler must not be null"));
            return f;
        }

        return CompletableFuture.supplyAsync(() -> {
            String toolName = taskToolSpecification.tool().getName();
            synchronized (this.toolRegistrationLock) {
                if (this.taskTools.removeIf(th -> th.tool().getName().equals(toolName))) {
                    logger.warn("Replace existing TaskTool with name '{}'", toolName);
                }

                this.taskTools.add(taskToolSpecification);
                this.taskToolsByName.put(toolName, taskToolSpecification);
            }
            logger.debug("Added task tool handler: {}", toolName);

            if (toolCapabilities != null && toolCapabilities.getListChanged() != null 
                    && toolCapabilities.getListChanged()) {
                return notifyToolsListChanged();
            }
            return CompletableFuture.<Void>completedFuture(null);
        }).thenCompose(f -> f);
    }

    public CompletableFuture<Void> removeTaskTool(
            String toolName,
            McpSchema.ServerCapabilities.ToolCapabilities toolCapabilities) {
        
        if (toolName == null) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(new IllegalArgumentException("Tool name must not be null"));
            return f;
        }

        return CompletableFuture.supplyAsync(() -> {
            if (this.taskTools.removeIf(toolSpec -> toolSpec.tool().getName().equals(toolName))) {
                this.taskToolsByName.remove(toolName);
                logger.debug("Removed task tool handler: {}", toolName);
                if (toolCapabilities != null && toolCapabilities.getListChanged() != null 
                        && toolCapabilities.getListChanged()) {
                    return notifyToolsListChanged();
                }
            }
            else {
                logger.warn("Ignore as a TaskTool with name '{}' not found", toolName);
            }
            return CompletableFuture.<Void>completedFuture(null);
        }).thenCompose(f -> f);
    }

    public List<McpSchema.Tool> listTaskTools() {
        return this.taskTools.stream()
                .map(TaskAwareToolSpecification::tool)
                .collect(Collectors.toList());
    }

    public List<McpSchema.Tool> getToolDefinitions() {
        return this.taskTools.stream()
                .map(TaskAwareToolSpecification::tool)
                .collect(Collectors.toList());
    }

    public boolean hasToolNamed(String name) {
        return this.taskToolsByName.containsKey(name);
    }

    public Object getToolRegistrationLock() {
        return this.toolRegistrationLock;
    }

    // ---------------------------------------
    // Task Tool Call Handling
    // ---------------------------------------

    public CompletableFuture<Object> handleToolCall(
            McpNettyServerExchange exchange,
            ArthasCommandContext commandContext,
            McpSchema.CallToolRequest callToolRequest) {
        
        TaskAwareToolSpecification taskTool = this.taskToolsByName.get(callToolRequest.getName());
        if (taskTool == null) {
            return null;
        }
        
        return doHandleTaskToolCall(exchange, commandContext, callToolRequest, taskTool)
                .thenApply(r -> (Object) r);
    }

    /** Dispatches a task-aware tool call; handles task creation or automatic polling. */
    private CompletableFuture<?> doHandleTaskToolCall(
            McpNettyServerExchange exchange,
            ArthasCommandContext commandContext,
            McpSchema.CallToolRequest request,
            TaskAwareToolSpecification taskTool) {

        McpSchema.ToolExecution execution = taskTool.tool().getExecution();
        McpSchema.TaskSupportMode taskSupportMode = execution != null ? execution.getTaskSupport() : null;

        if (request.getTask() != null) {
            if (getTaskStore() == null) {
                CompletableFuture<McpSchema.CreateTaskResult> f = new CompletableFuture<>();
                f.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
                        .message("Server does not support tasks")
                        .data("Task store not configured")
                        .build());
                return f;
            }
            return handleTaskToolCreateTask(exchange, commandContext, request, taskTool);
        }

        if (taskSupportMode == McpSchema.TaskSupportMode.REQUIRED) {
            CompletableFuture<McpSchema.CallToolResult> f = new CompletableFuture<>();
            f.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                    .message("This tool requires task-augmented execution")
                    .data("Tool '" + request.getName() + "' requires task metadata in the request")
                    .build());
            return f;
        }

        if (getTaskStore() != null) {
            return handleAutomaticTaskPolling(exchange, commandContext, request, taskTool);
        }

        if (taskTool.callHandler() != null) {
            return taskTool.callHandler().apply(exchange, commandContext, request);
        }

        CompletableFuture<McpSchema.CallToolResult> f = new CompletableFuture<>();
        f.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                .message("Tool requires task store or callHandler for execution")
                .build());
        return f;
    }

    /** Handles task creation for a task-aware tool call. */
    private CompletableFuture<McpSchema.CreateTaskResult> handleTaskToolCreateTask(
            McpNettyServerExchange exchange,
            ArthasCommandContext commandContext,
            McpSchema.CallToolRequest request,
            TaskAwareToolSpecification taskTool) {

        Long requestTtl = request.getTask() != null ? request.getTask().getTtl() : null;

        String sessionId = extractSessionId(exchange);
        logger.info("handleTaskToolCreateTask: Creating task for tool '{}' with sessionId: {}", 
                request.getName(), sessionId);

        CreateTaskContext extra = new DefaultCreateTaskContext(
                this.taskStore,
                getTaskMessageQueue(),
                exchange,
                sessionId,
                requestTtl,
                request,
                commandContext,
                this.sessionManager
        );

        Map<String, Object> args = request.getArguments() != null ? request.getArguments() : Collections.emptyMap();

        return taskTool.createTaskHandler().createTask(args, extra)
                .exceptionally(ex -> {
                    Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                    if (!(cause instanceof McpError)) {
                        throw new CompletionException(new McpError(
                                new McpSchema.JSONRPCResponse.JSONRPCError(
                                        McpSchema.ErrorCodes.INTERNAL_ERROR,
                                        "Task creation failed: " + cause.getMessage(),
                                        null
                                )
                        ));
                    }
                    throw new CompletionException(cause);
                });
    }

    /** Handles automatic task polling for a task-aware tool call without task metadata. */
    private CompletableFuture<McpSchema.CallToolResult> handleAutomaticTaskPolling(
            McpNettyServerExchange exchange,
            ArthasCommandContext commandContext,
            McpSchema.CallToolRequest request,
            TaskAwareToolSpecification taskTool) {

        CreateTaskContext extra = new DefaultCreateTaskContext(
                this.taskStore,
                getTaskMessageQueue(),
                exchange,
                extractSessionId(exchange),
                null,
                request,
                commandContext,
                this.sessionManager
        );

        Map<String, Object> args = request.getArguments() != null ? request.getArguments() : Collections.emptyMap();

        return taskTool.createTaskHandler().createTask(args, extra)
                .thenCompose(createResult -> {
                    McpSchema.Task task = createResult.getTask();
                    if (task == null) {
                        CompletableFuture<McpSchema.CallToolResult> f = new CompletableFuture<>();
                        f.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                                .message("createTaskHandler did not return a task")
                                .build());
                        return f;
                    }

                    String taskId = task.getTaskId();
                    String sessionId = extractSessionId(exchange);

                    return pollTaskUntilTerminal(taskId, sessionId, task, taskTool);
                });
    }

    /** Polls a task until it reaches a terminal state, then returns the result. */
    private CompletableFuture<McpSchema.CallToolResult> pollTaskUntilTerminal(
            String taskId,
            String sessionId,
            McpSchema.Task initialTask,
            TaskAwareToolSpecification taskTool) {
        
        long pollInterval = initialTask.getPollInterval() != null 
                ? initialTask.getPollInterval() 
                : TaskDefaults.DEFAULT_POLL_INTERVAL_MS;
        
        Duration timeout = this.automaticPollingTimeout != null 
                ? this.automaticPollingTimeout 
                : Duration.ofMillis(TaskDefaults.DEFAULT_AUTOMATIC_POLLING_TIMEOUT_MS);
        
        CompletableFuture<List<McpSchema.Task>> watchFuture = taskStore.watchTaskUntilTerminal(
                taskId, 
                sessionId, 
                timeout.toMillis()
        );
        
        return watchFuture.thenCompose(tasks -> {
            if (tasks.isEmpty()) {
                CompletableFuture<McpSchema.CallToolResult> f = new CompletableFuture<>();
                f.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                        .message("Task watch returned empty list")
                        .data("Task ID: " + taskId)
                        .build());
                return f;
            }
            
            McpSchema.Task finalTask = tasks.get(tasks.size() - 1);
            
            if (finalTask.getStatus() == McpSchema.TaskStatus.INPUT_REQUIRED) {
                CompletableFuture<McpSchema.CallToolResult> f = new CompletableFuture<>();
                f.completeExceptionally(new McpError(
                        new McpSchema.JSONRPCResponse.JSONRPCError(
                                McpSchema.ErrorCodes.INTERNAL_ERROR,
                                "Task requires interactive input which is not supported in automatic polling mode. " +
                                "Use task-augmented requests (with TaskMetadata) to enable interactive input. " +
                                "Task ID: " + taskId,
                                null
                        )
                ));
                return f;
            }

            // For FAILED/CANCELLED: fetch the stored payload (FAILED has one via failTask;
            // CANCELLED has none, so fall back to a synthetic error result).
            if (finalTask.getStatus() == McpSchema.TaskStatus.FAILED
                    || finalTask.getStatus() == McpSchema.TaskStatus.CANCELLED) {
                return taskStore.getTaskResult(taskId, sessionId)
                        .thenApply(result -> {
                            if (result != null) {
                                return (McpSchema.CallToolResult) result;
                            }
                            // CANCELLED (or FAILED without payload as a safety net)
                            String msg = finalTask.getStatus() == McpSchema.TaskStatus.CANCELLED
                                    ? "Task was cancelled" +
                                        (finalTask.getStatusMessage() != null ? ": " + finalTask.getStatusMessage() : "")
                                    : "Task failed" +
                                        (finalTask.getStatusMessage() != null ? ": " + finalTask.getStatusMessage() : "");
                            return new McpSchema.CallToolResult(msg, true, null);
                        });
            }

            return taskStore.getTaskResult(taskId, sessionId)
                    .thenApply(result -> (McpSchema.CallToolResult) result);
        }).exceptionally(ex -> {
            Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
            if (cause instanceof java.util.concurrent.TimeoutException) {
                throw new java.util.concurrent.CompletionException(new McpError(
                        new McpSchema.JSONRPCResponse.JSONRPCError(
                                McpSchema.ErrorCodes.INTERNAL_ERROR,
                                "Task timed out waiting for completion: " + taskId,
                                null
                        )
                ));
            }
            throw new java.util.concurrent.CompletionException(cause);
        });
    }

    private String extractSessionId(McpNettyServerExchange exchange) {
        return exchange.sessionId();
    }

    // ---------------------------------------
    // Task Request Handler Wiring
    // ---------------------------------------

    public Map<String, McpRequestHandler<?>> getRequestHandlers(
            McpSchema.ServerCapabilities.TaskCapabilities taskCapabilities) {
        Map<String, McpRequestHandler<?>> handlers = new HashMap<>();
        if (taskCapabilities != null && getTaskStore() != null) {
            this.taskHandlerRegistry.wireHandlers(
                    taskCapabilities.getList() != null,
                    taskCapabilities.getCancel() != null,
                    this::adaptTaskHandler,
                    handlers::put
            );
        }
        return handlers;
    }

    public void logCapabilityMismatches(McpSchema.ServerCapabilities.TaskCapabilities taskCapabilities) {
        if (taskCapabilities != null && getTaskStore() == null) {
            logger.warn("Server has tasks capability enabled but no TaskStore configured. " +
                    "Task operations will be unavailable. Provide a TaskStore or remove the tasks capability.");
        }
        if (getTaskStore() != null && taskCapabilities == null) {
            logger.warn("Server has a TaskStore configured but tasks capability is not enabled. " +
                    "Task operations will be unavailable. Enable the tasks capability or remove the TaskStore.");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> McpRequestHandler<T> adaptTaskHandler(String method, TaskManagerHost.TaskRequestHandler taskHandler) {
        return (exchange, commandContext, params) -> {
            String sessionId = extractSessionId(exchange);
            
            TaskManagerHost.TaskHandlerContext ctx = createTaskHandlerContext(
                    sessionId,
                    (reqMethod, reqParams, resultType) -> {
                        TypeReference<McpSchema.Result> typeRef = new TypeReference<McpSchema.Result>() {};
                        return exchange.getSession().sendRequest(reqMethod, reqParams, typeRef);
                    },
                    (notifMethod, notification) -> {
                        return exchange.sendNotification(notifMethod, notification);
                    }
            );

            return this.taskHandlerRegistry.<T>invokeHandler(method, params, ctx).thenApply(result -> {
                if (McpSchema.METHOD_TASKS_RESULT.equals(method) && result instanceof McpSchema.Result) {
                    try {
                        McpSchema.GetTaskPayloadRequest payloadReq = 
                                objectMapper.convertValue(params, McpSchema.GetTaskPayloadRequest.class);
                        if (payloadReq != null && payloadReq.getTaskId() != null) {
                            return (T) addRelatedTaskMetadata(payloadReq.getTaskId(), (McpSchema.Result) result);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to add related-task metadata", e);
                    }
                }
                return result;
            });
        };
    }

    // ---------------------------------------
    // Metadata Helpers
    // ---------------------------------------

    private McpSchema.Result addRelatedTaskMetadata(String taskId, McpSchema.Result result) {
        if (result instanceof McpSchema.CallToolResult) {
            McpSchema.CallToolResult ctr = (McpSchema.CallToolResult) result;
            Map<String, Object> newMeta = TaskMetadataUtils.mergeRelatedTaskMetadata(taskId, ctr.getMeta());
            return new McpSchema.CallToolResult(ctr.getContent(), ctr.getIsError(), newMeta);
        }
        return result;
    }

    public TaskMessageQueue getTaskMessageQueue() {
        return this.taskOptions != null ? this.taskOptions.messageQueue() : null;
    }

    // ---------------------------------------
    // Lifecycle
    // ---------------------------------------

    public CompletableFuture<Void> notifyTaskStatus(McpSchema.TaskStatusNotification taskStatusNotification) {
        if (taskStatusNotification == null) {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
                    .message("Task status notification must not be null")
                    .build());
            return f;
        }
        return this.clientNotifier.apply(McpSchema.METHOD_NOTIFICATION_TASKS_STATUS, taskStatusNotification);
    }

    private CompletableFuture<Void> notifyToolsListChanged() {
        return this.clientNotifier.apply(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, 
                Collections.emptyMap());
    }

    @Override
    public void close() {
        super.close();
        logger.info("ServerTaskToolHandler closed");
    }

    // ---------------------------------------
    // TaskManagerHost Implementation
    // ---------------------------------------

    @Override
    public <T extends McpSchema.Result> CompletableFuture<T> request(McpSchema.Request request, Class<T> resultType) {
        logger.debug("TaskManagerHost.request called on server. For session-specific requests, use exchange methods.");
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(new UnsupportedOperationException(
                "Broadcast requests not supported for tasks. Use session-specific exchange methods."));
        return f;
    }

    @Override
    public CompletableFuture<Void> notification(String notificationMethod, Object notification) {
        return this.clientNotifier.apply(notificationMethod, notification);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends McpSchema.Result> CompletableFuture<T> findAndInvokeCustomHandler(
            GetTaskFromStoreResult storeResult, String method, McpSchema.Request request,
            TaskManagerHost.TaskHandlerContext context, Class<T> resultType) {

        String toolName = null;
        if (storeResult.originatingRequest() instanceof McpSchema.CallToolRequest) {
            McpSchema.CallToolRequest ctr = (McpSchema.CallToolRequest) storeResult.originatingRequest();
            toolName = ctr.getName();
        }

        TaskAwareToolSpecification taskTool = toolName != null ? this.taskToolsByName.get(toolName) : null;

        if (taskTool == null) {
            return CompletableFuture.completedFuture(null);
        }

        McpNettyServerExchange exchange = new McpNettyServerExchange(context.sessionId(), null, null,
                null, McpTransportContext.EMPTY, null);

        if (McpSchema.METHOD_TASKS_GET.equals(method)) {
            GetTaskHandler handler = taskTool.getTaskHandler();
            if (handler != null && request instanceof McpSchema.GetTaskRequest) {
                McpSchema.GetTaskRequest getRequest = (McpSchema.GetTaskRequest) request;
                return handler.handle(exchange, getRequest)
                        .thenApply(result -> resultType.cast(result));
            }
        }
        else if (McpSchema.METHOD_TASKS_RESULT.equals(method)) {
            GetTaskResultHandler handler = taskTool.getTaskResultHandler();
            if (handler != null && request instanceof McpSchema.GetTaskPayloadRequest) {
                McpSchema.GetTaskPayloadRequest payloadRequest = (McpSchema.GetTaskPayloadRequest) request;
                return handler.handle(exchange, payloadRequest)
                        .thenApply(result -> resultType.cast(result));
            }
        }

        return CompletableFuture.completedFuture(null);
    }
}
