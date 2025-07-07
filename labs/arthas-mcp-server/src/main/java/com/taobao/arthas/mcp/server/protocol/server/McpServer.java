/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.spec.*;
import com.taobao.arthas.mcp.server.util.Assert;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * MCP server interface and builder for Netty-based implementation.
 *
 * @author Yeaury
 */
public interface McpServer {

	static NettySpecification netty(McpServerTransportProvider transportProvider) {
		return new NettySpecification(transportProvider);
	}

	/**
	 * serverSpecification
	 */
	class NettySpecification {

		private static final McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server",
				"1.0.0");

		private final McpServerTransportProvider transportProvider;

		private ObjectMapper objectMapper;

		private McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

		private McpSchema.ServerCapabilities serverCapabilities;

		private String instructions;

		/**
		 * The Model Context Protocol (MCP) allows servers to expose tools that can be
		 * invoked by language models. Tools enable models to interact with external
		 * systems, such as querying databases, calling APIs, or performing computations.
		 * Each tool is uniquely identified by a name and includes metadata describing its
		 * schema.
		 */
		private final List<McpServerFeatures.ToolSpecification> tools = new ArrayList<>();

		/**
		 * The Model Context Protocol (MCP) provides a standardized way for servers to
		 * expose resources to clients. Resources allow servers to share data that
		 * provides context to language models, such as files, database schemas, or
		 * application-specific information. Each resource is uniquely identified by a
		 * URI.
		 */
		private final Map<String, McpServerFeatures.ResourceSpecification> resources = new HashMap<>();

		private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();

		/**
		 * The Model Context Protocol (MCP) provides a standardized way for servers to
		 * expose prompt templates to clients. Prompts allow servers to provide structured
		 * messages and instructions for interacting with language models. Clients can
		 * discover available prompts, retrieve their contents, and provide arguments to
		 * customize them.
		 */
		private final Map<String, McpServerFeatures.PromptSpecification> prompts = new HashMap<>();

		private final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeHandlers = new ArrayList<>();

		private Duration requestTimeout = Duration.ofSeconds(10); // Default timeout

		private NettySpecification(McpServerTransportProvider transportProvider) {
			Assert.notNull(transportProvider, "Transport provider must not be null");
			this.transportProvider = transportProvider;
		}

		/**
		 * Sets the server implementation information that will be shared with clients
		 * during connection initialization. This helps with version compatibility,
		 * debugging, and server identification.
		 * @param serverInfo The server implementation details including name and version.
		 * Must not be null.
		 * @return This builder instance for method chaining
		 * @throws IllegalArgumentException if serverInfo is null
		 */
		public NettySpecification serverInfo(McpSchema.Implementation serverInfo) {
			Assert.notNull(serverInfo, "Server info must not be null");
			this.serverInfo = serverInfo;
			return this;
		}

		public NettySpecification requestTimeout(Duration requestTimeout) {
			Assert.notNull(requestTimeout, "Request timeout must not be null");
			this.requestTimeout = requestTimeout;
			return this;
		}

		public NettySpecification serverInfo(String name, String version) {
			Assert.hasText(name, "Name must not be null or empty");
			Assert.hasText(version, "Version must not be null or empty");
			this.serverInfo = new McpSchema.Implementation(name, version);
			return this;
		}

		public NettySpecification instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		public NettySpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
			this.serverCapabilities = serverCapabilities;
			return this;
		}

		public NettySpecification tool(McpSchema.Tool tool,
									   BiFunction<McpNettyServerExchange, Map<String, Object>, CompletableFuture<McpSchema.CallToolResult>> handler) {
			Assert.notNull(tool, "Tool must not be null");
			Assert.notNull(handler, "Handler must not be null");

			this.tools.add(new McpServerFeatures.ToolSpecification(tool, handler));

			return this;
		}

		public NettySpecification tools(List<McpServerFeatures.ToolSpecification> toolRegistrations) {
			Assert.notNull(toolRegistrations, "Tool handlers list must not be null");
			this.tools.addAll(toolRegistrations);
			return this;
		}

		public NettySpecification tools(McpServerFeatures.ToolSpecification... toolRegistrations) {
			for (McpServerFeatures.ToolSpecification tool : toolRegistrations) {
				this.tools.add(tool);
			}
			return this;
		}

		public NettySpecification resources(Map<String, McpServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
			this.resources.putAll(resourceSpecifications);
			return this;
		}

		public NettySpecification resources(List<McpServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		public NettySpecification resources(McpServerFeatures.ResourceSpecification... resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		public NettySpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(resourceTemplates);
			return this;
		}

		public NettySpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(Arrays.asList(resourceTemplates));
			return this;
		}

		public NettySpecification prompts(Map<String, McpServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			this.prompts.putAll(prompts);
			return this;
		}

		public NettySpecification prompts(List<McpServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		public NettySpecification prompts(McpServerFeatures.PromptSpecification... prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		public NettySpecification rootsChangeHandler(
				BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> handler) {
			Assert.notNull(handler, "Consumer must not be null");
			this.rootsChangeHandlers.add(handler);
			return this;
		}


		public NettySpecification rootsChangeHandlers(
				List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			this.rootsChangeHandlers.addAll(handlers);
			return this;
		}

		public NettySpecification rootsChangeHandlers(
				@SuppressWarnings("unchecked") BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>... handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			return this.rootsChangeHandlers(Arrays.asList(handlers));
		}

		public NettySpecification objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		public McpNettyServer build() {
			ObjectMapper mapper = this.objectMapper != null ? this.objectMapper : new ObjectMapper();
			return new McpNettyServer(
					this.transportProvider,
					mapper,
					this.requestTimeout,
					new McpServerFeatures.McpServerConfig(
							this.serverInfo,
							this.serverCapabilities,
							this.tools,
							this.resources,
							this.resourceTemplates,
							this.prompts,
							this.rootsChangeHandlers,
							this.instructions
					)
			);
		}
	}
}
