/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.taobao.arthas.mcp.server.protocol.spec.*;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.Utils;

/**
 * A Netty-based MCP server implementation that provides access to tools, resources, and prompts.
 *
 * @author Yeaury
 */
public class McpNettyServer {

	private static final Logger logger = LoggerFactory.getLogger(McpNettyServer.class);

	private final McpServerTransportProvider mcpTransportProvider;

	private final ObjectMapper objectMapper;

	private final McpSchema.ServerCapabilities serverCapabilities;

	private final McpSchema.Implementation serverInfo;

	private final String instructions;

	private final CopyOnWriteArrayList<McpServerFeatures.ToolSpecification> tools = new CopyOnWriteArrayList<>();

	private final CopyOnWriteArrayList<McpSchema.ResourceTemplate> resourceTemplates = new CopyOnWriteArrayList<>();

	private final ConcurrentHashMap<String, McpServerFeatures.ResourceSpecification> resources = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, McpServerFeatures.PromptSpecification> prompts = new ConcurrentHashMap<>();

	private McpSchema.LoggingLevel minLoggingLevel = McpSchema.LoggingLevel.DEBUG;

	private List<String> protocolVersions = Collections.singletonList(McpSchema.LATEST_PROTOCOL_VERSION);

	public McpNettyServer(
			McpServerTransportProvider mcpTransportProvider,
			ObjectMapper objectMapper,
			Duration requestTimeout,
			McpServerFeatures.McpServerConfig features) {
		this.mcpTransportProvider = mcpTransportProvider;
		this.objectMapper = objectMapper;
		this.serverInfo = features.getServerInfo();
		this.serverCapabilities = features.getServerCapabilities();
		this.instructions = features.getInstructions();
		this.tools.addAll(features.getTools());
		this.resources.putAll(features.getResources());
		this.resourceTemplates.addAll(features.getResourceTemplates());
		this.prompts.putAll(features.getPrompts());

		Map<String, McpServerSession.RequestHandler<?>> requestHandlers = new HashMap<>();

		// Initialize request handlers for standard MCP methods

		// Ping MUST respond with an empty data, but not NULL response.
		requestHandlers.put(McpSchema.METHOD_PING,
				(exchange, params) -> CompletableFuture.completedFuture(Collections.emptyMap()));

		// Add tools API handlers if the tool capability is enabled
		if (this.serverCapabilities.getTools() != null) {
			requestHandlers.put(McpSchema.METHOD_TOOLS_LIST, toolsListRequestHandler());
			requestHandlers.put(McpSchema.METHOD_TOOLS_CALL, toolsCallRequestHandler());
		}

		// Add resources API handlers if provided
		if (this.serverCapabilities.getResources() != null) {
			requestHandlers.put(McpSchema.METHOD_RESOURCES_LIST, resourcesListRequestHandler());
			requestHandlers.put(McpSchema.METHOD_RESOURCES_READ, resourcesReadRequestHandler());
			requestHandlers.put(McpSchema.METHOD_RESOURCES_TEMPLATES_LIST, resourceTemplateListRequestHandler());
		}

		// Add prompts API handlers if provider exists
		if (this.serverCapabilities.getPrompts() != null) {
			requestHandlers.put(McpSchema.METHOD_PROMPT_LIST, promptsListRequestHandler());
			requestHandlers.put(McpSchema.METHOD_PROMPT_GET, promptsGetRequestHandler());
		}

		if (this.serverCapabilities.getLogging() != null) {
			requestHandlers.put(McpSchema.METHOD_LOGGING_SET_LEVEL, setLoggerRequestHandler());
		}

		Map<String, McpServerSession.NotificationHandler> notificationHandlers = new HashMap<>();

		notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_INITIALIZED,
				(exchange, params) -> CompletableFuture.completedFuture(null));

		List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers = features
			.getRootsChangeConsumers();

		if (Utils.isEmpty(rootsChangeConsumers)) {
			rootsChangeConsumers = Collections.singletonList(
					(exchange, roots) -> CompletableFuture.runAsync(() ->
									logger.warn("Roots list changed notification, but no consumers provided. Roots list changed: {}", roots))
			);
		}

		notificationHandlers.put(McpSchema.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED,
				rootsListChangedNotificationHandler(rootsChangeConsumers));

		mcpTransportProvider.setSessionFactory(transport -> {
			Channel channel = transport.getChannel();
			return new McpServerSession(UUID.randomUUID().toString(),
					requestTimeout, transport, this::initializeRequestHandler,
					() -> CompletableFuture.completedFuture(null), requestHandlers, notificationHandlers,
					channel);
		});
	}

	// ---------------------------------------
	// Lifecycle Management
	// ---------------------------------------
	private CompletableFuture<McpSchema.InitializeResult> initializeRequestHandler(
			McpSchema.InitializeRequest initializeRequest) {
		return CompletableFuture.supplyAsync(() -> {
			logger.info("Client initialize request - Protocol: {}, Capabilities: {}, Info: {}",
					initializeRequest.getProtocolVersion(), initializeRequest.getCapabilities(),
					initializeRequest.getClientInfo());

			// The server MUST respond with the highest protocol version it supports
			// if
			// it does not support the requested (e.g. Client) version.
			String serverProtocolVersion = protocolVersions.get(protocolVersions.size() - 1);

			if (protocolVersions.contains(initializeRequest.getProtocolVersion())) {
				serverProtocolVersion = initializeRequest.getProtocolVersion();
			}
			else {
				logger.warn(
						"Client requested unsupported protocol version: {}, " + "so the server will suggest {} instead",
						initializeRequest.getProtocolVersion(), serverProtocolVersion);
			}

			return new McpSchema.InitializeResult(serverProtocolVersion, serverCapabilities, serverInfo, instructions);
		});
	}

	public McpSchema.ServerCapabilities getServerCapabilities() {
		return this.serverCapabilities;
	}

	public McpSchema.Implementation getServerInfo() {
		return this.serverInfo;
	}

	public CompletableFuture<Void> closeGracefully() {
		return this.mcpTransportProvider.closeGracefully();
	}

	public void close() {
		this.mcpTransportProvider.close();
	}

	private McpServerSession.NotificationHandler rootsListChangedNotificationHandler(
			List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers) {
		return (exchange, params) -> {
			CompletableFuture<McpSchema.ListRootsResult> futureRoots = exchange.listRoots();

			return futureRoots.thenCompose(listRootsResult -> {
				List<McpSchema.Root> roots = listRootsResult.getRoots();
				List<CompletableFuture<?>> futures = new ArrayList<>();
				for (BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> consumer : rootsChangeConsumers) {
					CompletableFuture<Void> future = consumer.apply(exchange, roots).exceptionally(error -> {
						logger.error("Error handling roots list change notification", error);
						return null;
					});
					futures.add(future);
				}
				return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			});
		};
	}

	// ---------------------------------------
	// Tool Management
	// ---------------------------------------

	public CompletableFuture<Void> addTool(McpServerFeatures.ToolSpecification toolSpecification) {
		if (toolSpecification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool specification must not be null"));
			return future;
		}
		if (toolSpecification.getTool() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool must not be null"));
			return future;
		}
		if (toolSpecification.getCall() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool call handler must not be null"));
			return future;
		}
		if (this.serverCapabilities.getTools() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with tool capabilities"));
			return future;
		}

		return CompletableFuture.supplyAsync(() -> {
			// Check for duplicate tool names
			if (this.tools.stream().anyMatch(th -> th.getTool().getName().equals(toolSpecification.getTool().getName()))) {
				throw new CompletionException(
						new McpError("Tool with name '" + toolSpecification.getTool().getName() + "' already exists"));
			}
			this.tools.add(toolSpecification);
			logger.debug("Added tool handler: {}", toolSpecification.getTool().getName());
			return null;
		}).thenCompose(ignored -> {
			if (this.serverCapabilities.getTools().getListChanged()) {
				return notifyToolsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
			logger.error("Error while adding tool", cause);
			throw new CompletionException(cause);
		});
	}

	public CompletableFuture<Void> removeTool(String toolName) {
		if (toolName == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Tool name must not be null"));
			return future;
		}
		if (this.serverCapabilities.getTools() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with tool capabilities"));
			return future;
		}

		return CompletableFuture.supplyAsync(() -> {
			boolean removed = this.tools.removeIf(spec -> spec.getTool().getName().equals(toolName));
			if (!removed) {
				throw new CompletionException(new McpError("Tool with name '" + toolName + "' not found"));
			}
			logger.debug("Removed tool handler: {}", toolName);
			return null;
		}).thenCompose(ignored -> {
			if (this.serverCapabilities.getTools().getListChanged()) {
				return notifyToolsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
			logger.error("Error while removing tool '{}'", toolName, cause);
			throw new CompletionException(cause);
		});
	}

	public CompletableFuture<Void> notifyToolsListChanged() {
		logger.debug("Notifying clients about tool list changes");
		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_TOOLS_LIST_CHANGED, null);
	}

	private McpServerSession.RequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
		return (exchange, params) -> {
			List<McpSchema.Tool> tools = new ArrayList<>();
			for (McpServerFeatures.ToolSpecification toolSpec : this.tools) {
				tools.add(toolSpec.getTool());
			}

			return CompletableFuture.completedFuture(new McpSchema.ListToolsResult(tools, null));
		};
	}

	private McpServerSession.RequestHandler<McpSchema.CallToolResult> toolsCallRequestHandler() {
		return (exchange, params) -> {
			McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.CallToolRequest>() {
					});
			Optional<McpServerFeatures.ToolSpecification> toolSpecification = this.tools.stream()
				.filter(tr -> callToolRequest.getName().equals(tr.getTool().getName()))
				.findAny();

			if (!toolSpecification.isPresent()) {
				CompletableFuture<McpSchema.CallToolResult> future = new CompletableFuture<>();
				future.completeExceptionally(new McpError("no tool found: " + callToolRequest.getName()));
				return future;
			}

			return toolSpecification.get().getCall().apply(exchange, callToolRequest.getArguments());
		};
	}

	// ---------------------------------------
	// Resource Management
	// ---------------------------------------

	public CompletableFuture<Void> addResource(McpServerFeatures.ResourceSpecification resourceSpecification) {
		if (resourceSpecification == null || resourceSpecification.getResource() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource must not be null"));
			return future;
		}
		if (this.serverCapabilities.getResources() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with resource capabilities"));
			return future;
		}

		return CompletableFuture.supplyAsync(() -> {
			if (this.resources.putIfAbsent(resourceSpecification.getResource().getUri(), resourceSpecification) != null) {
				throw new CompletionException(new McpError(
						"Resource with URI '" + resourceSpecification.getResource().getUri() + "' already exists"));
			}
			logger.debug("Added resource handler: {}", resourceSpecification.getResource().getUri());
			return null;
		}).thenCompose(ignored -> {
			if (this.serverCapabilities.getResources().getListChanged()) {
				return notifyResourcesListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
			logger.error("Error while adding resource '{}'", resourceSpecification.getResource().getUri(), cause);
			throw new CompletionException(cause);
		});
	}

	public CompletableFuture<Void> removeResource(String resourceUri) {
		if (resourceUri == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource URI must not be null"));
			return future;
		}
		if (this.serverCapabilities.getResources() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with resource capabilities"));
			return future;
		}

		return CompletableFuture.supplyAsync(() -> {
			McpServerFeatures.ResourceSpecification removed = this.resources.remove(resourceUri);
			if (removed == null) {
				throw new CompletionException(new McpError("Resource with URI '" + resourceUri + "' not found"));
			}

			logger.debug("Removed resource handler: {}", resourceUri);
			return null;
		}).thenCompose(ignored -> {
			if (this.serverCapabilities.getResources().getListChanged()) {
				return notifyResourcesListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
			logger.error("Error while removing resource '{}'", resourceUri, cause);
			throw new CompletionException(cause);
		});
	}

	public CompletableFuture<Void> notifyResourcesListChanged() {
		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED, null);
	}

	private McpServerSession.RequestHandler<McpSchema.ListResourcesResult> resourcesListRequestHandler() {
		return (exchange, params) -> {
			List<McpSchema.Resource> resourceList = new ArrayList<>();
			for (McpServerFeatures.ResourceSpecification spec : this.resources.values()) {
				resourceList.add(spec.getResource());
			}
			return CompletableFuture.completedFuture(new McpSchema.ListResourcesResult(resourceList, null));
		};
	}

	private McpServerSession.RequestHandler<McpSchema.ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
		return (exchange, params) -> CompletableFuture
			.completedFuture(new McpSchema.ListResourceTemplatesResult(this.resourceTemplates, null));
	}

	private McpServerSession.RequestHandler<McpSchema.ReadResourceResult> resourcesReadRequestHandler() {
		return (exchange, params) -> {
			McpSchema.ReadResourceRequest resourceRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.ReadResourceRequest>() {
					});
			String resourceUri = resourceRequest.getUri();
			McpServerFeatures.ResourceSpecification specification = this.resources.get(resourceUri);
			if (specification != null) {
				return specification.getReadHandler().apply(exchange, resourceRequest);
			}
			CompletableFuture<McpSchema.ReadResourceResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource not found: " + resourceUri));
			return future;
		};
	}

	// ---------------------------------------
	// Prompt Management
	// ---------------------------------------

	public CompletableFuture<Void> addPrompt(McpServerFeatures.PromptSpecification promptSpecification) {
		if (promptSpecification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt specification must not be null"));
			return future;
		}
		if (this.serverCapabilities.getPrompts() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with prompt capabilities"));
			return future;
		}

		return CompletableFuture.supplyAsync(() -> {
			McpServerFeatures.PromptSpecification existing = this.prompts
				.putIfAbsent(promptSpecification.getPrompt().getName(), promptSpecification);
			if (existing != null) {
				throw new CompletionException(
						new McpError("Prompt with name '" + promptSpecification.getPrompt().getName() + "' already exists"));
			}

			logger.debug("Added prompt handler: {}", promptSpecification.getPrompt().getName());
			return null;
		}).thenCompose(ignored -> {
			if (this.serverCapabilities.getPrompts().getListChanged()) {
				return notifyPromptsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
			logger.error("Error while adding prompt '{}'", promptSpecification.getPrompt().getName(), cause);
			throw new CompletionException(cause);
		});
	}

	public CompletableFuture<Void> removePrompt(String promptName) {
		if (promptName == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt name must not be null"));
			return future;
		}
		if (this.serverCapabilities.getPrompts() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with prompt capabilities"));
			return future;
		}

		return CompletableFuture.supplyAsync(() -> {
			McpServerFeatures.PromptSpecification removed = this.prompts.remove(promptName);
			if (removed == null) {
				throw new CompletionException(new McpError("Prompt with name '" + promptName + "' not found"));
			}
			logger.debug("Removed prompt handler: {}", promptName);
			return null;
		}).thenCompose(ignored -> {
			if (this.serverCapabilities.getPrompts().getListChanged()) {
				return notifyPromptsListChanged();
			}
			return CompletableFuture.completedFuture(null);
		}).exceptionally(ex -> {
			Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
			logger.error("Error while removing prompt '{}'", promptName, cause);
			throw new CompletionException(cause);
		});
	}

	public CompletableFuture<Void> notifyPromptsListChanged() {
		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED, null);
	}

	private McpServerSession.RequestHandler<McpSchema.ListPromptsResult> promptsListRequestHandler() {
		return (exchange, params) -> {
			List<McpSchema.Prompt> promptList = new ArrayList<>();
			for (McpServerFeatures.PromptSpecification promptSpec : this.prompts.values()) {
				promptList.add(promptSpec.getPrompt());
			}
			return CompletableFuture.completedFuture(new McpSchema.ListPromptsResult(promptList, null));
		};
	}

	private McpServerSession.RequestHandler<McpSchema.GetPromptResult> promptsGetRequestHandler() {
		return (exchange, params) -> {
			McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.GetPromptRequest>() {
					});

			McpServerFeatures.PromptSpecification specification = this.prompts.get(promptRequest.getName());
			if (specification != null) {
				return specification.getPromptHandler().apply(exchange, promptRequest);
			}
			CompletableFuture<McpSchema.GetPromptResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt not found: " + promptRequest.getName()));
			return future;
		};
	}

	// ---------------------------------------
	// Logging Management
	// ---------------------------------------
	public CompletableFuture<Void> loggingNotification(
			McpSchema.LoggingMessageNotification loggingMessageNotification) {
		Assert.notNull(loggingMessageNotification, "Logging message must not be null");

		if (loggingMessageNotification.getLevel().level() < minLoggingLevel.level()) {
			return CompletableFuture.completedFuture(null);
		}

		return this.mcpTransportProvider.notifyClients(McpSchema.METHOD_NOTIFICATION_MESSAGE,
				loggingMessageNotification);
	}

	private McpServerSession.RequestHandler<Map<String, Object>> setLoggerRequestHandler() {
		return (exchange, params) -> {
			try {
				McpSchema.SetLevelRequest request = this.objectMapper.convertValue(params,
						McpSchema.SetLevelRequest.class);
				this.minLoggingLevel = request.getLevel();
				return CompletableFuture.completedFuture(Collections.emptyMap());
			}
			catch (Exception e) {
				CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
				future.completeExceptionally(new McpError("An error occurred while processing a request to set the log level: " + e.getMessage()));
				return future;
			}
		};
	}

	// ---------------------------------------
	// Sampling
	// ---------------------------------------

	public void setProtocolVersions(List<String> protocolVersions) {
		this.protocolVersions = protocolVersions;
	}

}