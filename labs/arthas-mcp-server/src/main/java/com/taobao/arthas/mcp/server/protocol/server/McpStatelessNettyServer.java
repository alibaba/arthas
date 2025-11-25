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
 * A Netty-based MCP server implementation that provides access to tools, resources, and prompts.
 *
 * @author Yeaury
 */
public class McpStatelessNettyServer {

	private static final Logger logger = LoggerFactory.getLogger(McpStatelessNettyServer.class);

	private final McpStatelessServerTransport mcpTransportProvider;

	private final ObjectMapper objectMapper;

	private final McpSchema.ServerCapabilities serverCapabilities;

	private final McpSchema.Implementation serverInfo;

	private final String instructions;

	private final CopyOnWriteArrayList<McpStatelessServerFeatures.ToolSpecification> tools = new CopyOnWriteArrayList<>();

	private final CopyOnWriteArrayList<McpSchema.ResourceTemplate> resourceTemplates = new CopyOnWriteArrayList<>();

	private final ConcurrentHashMap<String, McpStatelessServerFeatures.ResourceSpecification> resources = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<String, McpStatelessServerFeatures.PromptSpecification> prompts = new ConcurrentHashMap<>();

	private List<String> protocolVersions;

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
		this.tools.addAll(features.getTools());
		this.resources.putAll(features.getResources());
		this.resourceTemplates.addAll(features.getResourceTemplates());
		this.prompts.putAll(features.getPrompts());

		Map<String, McpStatelessRequestHandler<?>> requestHandlers = new HashMap<>();

		// Initialize request handlers for standard MCP methods

		// Ping MUST respond with an empty data, but not NULL response.
		requestHandlers.put(McpSchema.METHOD_PING,
				(exchange, commandContext, params) -> CompletableFuture.completedFuture(Collections.emptyMap()));

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

        this.protocolVersions = new ArrayList<>(mcpTransport.protocolVersions());

		McpStatelessServerHandler handler = new DefaultMcpStatelessServerHandler(requestHandlers, new HashMap<>(), commandExecutor);
		mcpTransport.setMcpHandler(handler);
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

	private McpNotificationHandler rootsListChangedNotificationHandler(
			List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers) {
		return (exchange, commandContext, params) -> {
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

	public CompletableFuture<Void> addTool(McpStatelessServerFeatures.ToolSpecification toolSpecification) {
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

		return CompletableFuture
				.runAsync(() -> {
					if (this.tools.stream().anyMatch(th ->
							th.getTool().getName().equals(toolSpecification.getTool().getName()))) {
						throw new CompletionException(
								new McpError("Tool with name '" + toolSpecification.getTool().getName() + "' already exists"));
					}
					this.tools.add(toolSpecification);
					logger.debug("Added tool handler: {}", toolSpecification.getTool().getName());
				})
				.exceptionally(ex -> {
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

		return CompletableFuture
				.runAsync(() -> {
					boolean removed = this.tools.removeIf(
							spec -> spec.getTool().getName().equals(toolName));
					if (!removed) {
						throw new CompletionException(
								new McpError("Tool with name '" + toolName + "' not found"));
					}
					logger.debug("Removed tool handler: {}", toolName);
				})
				.exceptionally(ex -> {
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					logger.error("Error while removing tool '{}'", toolName, cause);
					throw new CompletionException(cause);
				});
	}

	private McpStatelessRequestHandler<McpSchema.ListToolsResult> toolsListRequestHandler() {
		return (exchange, commandContext, params) -> {
			List<McpSchema.Tool> tools = new ArrayList<>();
			for (McpStatelessServerFeatures.ToolSpecification toolSpec : this.tools) {
				tools.add(toolSpec.getTool());
			}

			return CompletableFuture.completedFuture(new McpSchema.ListToolsResult(tools, null));
		};
	}

	private McpStatelessRequestHandler<McpSchema.CallToolResult> toolsCallRequestHandler() {
		return (context, commandContext, params) -> {
			McpSchema.CallToolRequest callToolRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.CallToolRequest>() {
					});
			Optional<McpStatelessServerFeatures.ToolSpecification> toolSpecification = this.tools.stream()
				.filter(tr -> callToolRequest.getName().equals(tr.getTool().getName()))
				.findAny();

			if (!toolSpecification.isPresent()) {
				CompletableFuture<McpSchema.CallToolResult> future = new CompletableFuture<>();
				future.completeExceptionally(new McpError("no tool found: " + callToolRequest.getName()));
				return future;
			}

			return toolSpecification.get().getCall().apply(context, commandContext, callToolRequest.getArguments());
		};
	}

	// ---------------------------------------
	// Resource Management
	// ---------------------------------------

	public CompletableFuture<Void> addResource(McpStatelessServerFeatures.ResourceSpecification resourceSpecification) {
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

		return CompletableFuture
				.runAsync(() -> {
					String uri = resourceSpecification.getResource().getUri();
					if (this.resources.putIfAbsent(uri, resourceSpecification) != null) {
						throw new CompletionException(new McpError("Resource with URI '" + uri + "' already exists"));
					}
					logger.debug("Added resource handler: {}", uri);
				})
				.exceptionally(ex -> {
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					logger.error("Error while adding resource '{}'",
							resourceSpecification.getResource().getUri(), cause);
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

		return CompletableFuture
				.runAsync(() -> {
					McpStatelessServerFeatures.ResourceSpecification removed = this.resources.remove(resourceUri);
					if (removed == null) {
						throw new CompletionException(new McpError("Resource with URI '" + resourceUri + "' not found"));
					}
					logger.debug("Removed resource handler: {}", resourceUri);
				})
				.exceptionally(ex -> {
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					logger.error("Error while removing resource '{}'", resourceUri, cause);
					throw new CompletionException(cause);
				});
	}

	private McpStatelessRequestHandler<McpSchema.ListResourcesResult> resourcesListRequestHandler() {
		return (exchange, commandContext,  params) -> {
			List<McpSchema.Resource> resourceList = new ArrayList<>();
			for (McpStatelessServerFeatures.ResourceSpecification spec : this.resources.values()) {
				resourceList.add(spec.getResource());
			}
			return CompletableFuture.completedFuture(new McpSchema.ListResourcesResult(resourceList, null));
		};
	}

	private McpStatelessRequestHandler<McpSchema.ListResourceTemplatesResult> resourceTemplateListRequestHandler() {
		return (context, commandContext, params) -> CompletableFuture
			.completedFuture(new McpSchema.ListResourceTemplatesResult(this.resourceTemplates, null));
	}

	private McpStatelessRequestHandler<McpSchema.ReadResourceResult> resourcesReadRequestHandler() {
		return (context, commandContext, params) -> {
			McpSchema.ReadResourceRequest resourceRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.ReadResourceRequest>() {
					});
			String resourceUri = resourceRequest.getUri();
			McpStatelessServerFeatures.ResourceSpecification specification = this.resources.get(resourceUri);
			if (specification != null) {
				return specification.getReadHandler().apply(context, resourceRequest);
			}
			CompletableFuture<McpSchema.ReadResourceResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Resource not found: " + resourceUri));
			return future;
		};
	}

	// ---------------------------------------
	// Prompt Management
	// ---------------------------------------

	public CompletableFuture<Void> addPrompt(McpStatelessServerFeatures.PromptSpecification promptSpecification) {
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

		return CompletableFuture
				.runAsync(() -> {
					String name = promptSpecification.getPrompt().getName();
					McpStatelessServerFeatures.PromptSpecification existing =
							this.prompts.putIfAbsent(name, promptSpecification);
					if (existing != null) {
						throw new CompletionException(new McpError("Prompt with name '" + name + "' already exists"));
					}
					logger.debug("Added prompt handler: {}", name);
				})
				.exceptionally(ex -> {
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					String name = promptSpecification.getPrompt().getName();
					logger.error("Error while adding prompt '{}'", name, cause);
					throw new CompletionException(cause);
				});
	}

	public CompletableFuture<Void> removePrompt(String promptName) {
		if (promptName == null || promptName.isEmpty()) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt name must not be null or empty"));
			return future;
		}
		if (this.serverCapabilities.getPrompts() == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Server must be configured with prompt capabilities"));
			return future;
		}

		return CompletableFuture
				.runAsync(() -> {
					McpStatelessServerFeatures.PromptSpecification removed =
							this.prompts.remove(promptName);
					if (removed == null) {
						throw new CompletionException(new McpError("Prompt with name '" + promptName + "' not found"));
					}
					logger.debug("Removed prompt handler: {}", promptName);
				})
				.exceptionally(ex -> {
					Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
					logger.error("Error while removing prompt '{}'", promptName, cause);
					throw new CompletionException(cause);
				});
	}


	private McpStatelessRequestHandler<McpSchema.ListPromptsResult> promptsListRequestHandler() {
		return (exchange, commandContext, params) -> {
			List<McpSchema.Prompt> promptList = new ArrayList<>();
			for (McpStatelessServerFeatures.PromptSpecification promptSpec : this.prompts.values()) {
				promptList.add(promptSpec.getPrompt());
			}
			return CompletableFuture.completedFuture(new McpSchema.ListPromptsResult(promptList, null));
		};
	}

	private McpStatelessRequestHandler<McpSchema.GetPromptResult> promptsGetRequestHandler() {
		return (context, commandContext, params) -> {
			McpSchema.GetPromptRequest promptRequest = objectMapper.convertValue(params,
					new TypeReference<McpSchema.GetPromptRequest>() {
					});

			McpStatelessServerFeatures.PromptSpecification specification = this.prompts.get(promptRequest.getName());
			if (specification != null) {
				return specification.getPromptHandler().apply(context, promptRequest);
			}
			CompletableFuture<McpSchema.GetPromptResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Prompt not found: " + promptRequest.getName()));
			return future;
		};
	}


	// ---------------------------------------
	// Sampling
	// ---------------------------------------

	public void setProtocolVersions(List<String> protocolVersions) {
		this.protocolVersions = protocolVersions;
	}

}
