/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.spec.JsonSchemaValidator;
import com.taobao.arthas.mcp.server.util.JsonParser;
import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpStatelessServerTransport;
import com.taobao.arthas.mcp.server.protocol.spec.McpStreamableServerTransportProvider;
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

	McpSchema.Implementation DEFAULT_SERVER_INFO = new McpSchema.Implementation("mcp-server", "1.0.0");

	static StreamableServerNettySpecification netty(McpStreamableServerTransportProvider transportProvider) {
		return new StreamableServerNettySpecification(transportProvider);
	}

	static StatelessServerNettySpecification netty(McpStatelessServerTransport transport) {
		return new StatelessServerNettySpecification(transport);
	}

	class StreamableServerNettySpecification {

		ObjectMapper objectMapper;

		McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

		McpSchema.ServerCapabilities serverCapabilities;

		String instructions;

		CommandExecutor commandExecutor;

		JsonSchemaValidator validator;

		private final McpStreamableServerTransportProvider transportProvider;

		final List<McpServerFeatures.ToolSpecification> tools = new ArrayList<>();

		final Map<String, McpServerFeatures.ResourceSpecification> resources = new HashMap<>();

		final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();

		final Map<String, McpServerFeatures.PromptSpecification> prompts = new HashMap<>();

		final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeHandlers = new ArrayList<>();

		Duration requestTimeout = Duration.ofSeconds(10); // Default timeout

		public StreamableServerNettySpecification(McpStreamableServerTransportProvider transportProvider) {
			this.transportProvider = transportProvider;
		}

		public StreamableServerNettySpecification serverInfo(McpSchema.Implementation serverInfo) {
			Assert.notNull(serverInfo, "Server info must not be null");
			this.serverInfo = serverInfo;
			return this;
		}

		public StreamableServerNettySpecification requestTimeout(Duration requestTimeout) {
			Assert.notNull(requestTimeout, "Request timeout must not be null");
			this.requestTimeout = requestTimeout;
			return this;
		}

		public StreamableServerNettySpecification serverInfo(String name, String version) {
			Assert.hasText(name, "Name must not be null or empty");
			Assert.hasText(version, "Version must not be null or empty");
			this.serverInfo = new McpSchema.Implementation(name, version);
			return this;
		}

		public StreamableServerNettySpecification instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		public StreamableServerNettySpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
			this.serverCapabilities = serverCapabilities;
			return this;
		}

		public StreamableServerNettySpecification validator(JsonSchemaValidator validator) {
			this.validator = validator;
			return this;
		}

		public StreamableServerNettySpecification tool(McpSchema.Tool tool,
									   McpServerFeatures.ToolCallFunction handler) {
			Assert.notNull(tool, "Tool must not be null");
			Assert.notNull(handler, "Handler must not be null");

			this.tools.add(new McpServerFeatures.ToolSpecification(tool, handler));

			return this;
		}

		public StreamableServerNettySpecification tools(List<McpServerFeatures.ToolSpecification> toolRegistrations) {
			Assert.notNull(toolRegistrations, "Tool handlers list must not be null");
			this.tools.addAll(toolRegistrations);
			return this;
		}

		public StreamableServerNettySpecification tools(McpServerFeatures.ToolSpecification... toolRegistrations) {
			this.tools.addAll(Arrays.asList(toolRegistrations));
			return this;
		}

		public StreamableServerNettySpecification resources(Map<String, McpServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
			this.resources.putAll(resourceSpecifications);
			return this;
		}

		public StreamableServerNettySpecification resources(List<McpServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		public StreamableServerNettySpecification resources(McpServerFeatures.ResourceSpecification... resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		public StreamableServerNettySpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(resourceTemplates);
			return this;
		}

		public StreamableServerNettySpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(Arrays.asList(resourceTemplates));
			return this;
		}

		public StreamableServerNettySpecification prompts(Map<String, McpServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			this.prompts.putAll(prompts);
			return this;
		}

		public StreamableServerNettySpecification prompts(List<McpServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		public StreamableServerNettySpecification prompts(McpServerFeatures.PromptSpecification... prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		public StreamableServerNettySpecification rootsChangeHandler(
				BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> handler) {
			Assert.notNull(handler, "Consumer must not be null");
			this.rootsChangeHandlers.add(handler);
			return this;
		}


		public StreamableServerNettySpecification rootsChangeHandlers(
				List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			this.rootsChangeHandlers.addAll(handlers);
			return this;
		}

		public StreamableServerNettySpecification rootsChangeHandlers(
				@SuppressWarnings("unchecked") BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>... handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			return this.rootsChangeHandlers(Arrays.asList(handlers));
		}

		public StreamableServerNettySpecification objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		public StreamableServerNettySpecification commandExecutor(CommandExecutor commandExecutor) {
			Assert.notNull(commandExecutor, "CommandExecutor must not be null");
			this.commandExecutor = commandExecutor;
			return this;
		}

		public McpNettyServer build() {
			ObjectMapper mapper = this.objectMapper != null ? this.objectMapper : JsonParser.getObjectMapper();
			Assert.notNull(this.commandExecutor, "CommandExecutor must be set before building");
			return new McpNettyServer(
					this.transportProvider, mapper, this.requestTimeout,
					new McpServerFeatures.McpServerConfig(this.serverInfo, this.serverCapabilities, this.tools,
							this.resources, this.resourceTemplates, this.prompts, this.rootsChangeHandlers, this.instructions
					), this.commandExecutor, this.validator
			);
		}
	}

	class StatelessServerNettySpecification {

		private final McpStatelessServerTransport transport;

		ObjectMapper objectMapper;

		McpSchema.Implementation serverInfo = DEFAULT_SERVER_INFO;

		McpSchema.ServerCapabilities serverCapabilities;

		String instructions;

		CommandExecutor commandExecutor;

		JsonSchemaValidator validator;

		final List<McpStatelessServerFeatures.ToolSpecification> tools = new ArrayList<>();

		final Map<String, McpStatelessServerFeatures.ResourceSpecification> resources = new HashMap<>();

		final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();

		final Map<String, McpStatelessServerFeatures.PromptSpecification> prompts = new HashMap<>();

		final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeHandlers = new ArrayList<>();

		Duration requestTimeout = Duration.ofSeconds(10); // Default timeout

		StatelessServerNettySpecification(McpStatelessServerTransport transport) {
			this.transport = transport;
		}

		public StatelessServerNettySpecification serverInfo(McpSchema.Implementation serverInfo) {
			Assert.notNull(serverInfo, "Server info must not be null");
			this.serverInfo = serverInfo;
			return this;
		}

		public StatelessServerNettySpecification requestTimeout(Duration requestTimeout) {
			Assert.notNull(requestTimeout, "Request timeout must not be null");
			this.requestTimeout = requestTimeout;
			return this;
		}

		public StatelessServerNettySpecification serverInfo(String name, String version) {
			Assert.hasText(name, "Name must not be null or empty");
			Assert.hasText(version, "Version must not be null or empty");
			this.serverInfo = new McpSchema.Implementation(name, version);
			return this;
		}

		public StatelessServerNettySpecification instructions(String instructions) {
			this.instructions = instructions;
			return this;
		}

		public StatelessServerNettySpecification capabilities(McpSchema.ServerCapabilities serverCapabilities) {
			this.serverCapabilities = serverCapabilities;
			return this;
		}

		public StatelessServerNettySpecification validator(JsonSchemaValidator validator) {
			this.validator = validator;
			return this;
		}

		public StatelessServerNettySpecification tools(List<McpStatelessServerFeatures.ToolSpecification> toolRegistrations) {
			Assert.notNull(toolRegistrations, "Tool handlers list must not be null");
			this.tools.addAll(toolRegistrations);
			return this;
		}

		public StatelessServerNettySpecification tools(McpStatelessServerFeatures.ToolSpecification... toolRegistrations) {
			for (McpStatelessServerFeatures.ToolSpecification tool : toolRegistrations) {
				this.tools.add(tool);
			}
			return this;
		}

		public StatelessServerNettySpecification resources(Map<String, McpStatelessServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers map must not be null");
			this.resources.putAll(resourceSpecifications);
			return this;
		}

		public StatelessServerNettySpecification resources(List<McpStatelessServerFeatures.ResourceSpecification> resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpStatelessServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		public StatelessServerNettySpecification resources(McpStatelessServerFeatures.ResourceSpecification... resourceSpecifications) {
			Assert.notNull(resourceSpecifications, "Resource handlers list must not be null");
			for (McpStatelessServerFeatures.ResourceSpecification resource : resourceSpecifications) {
				this.resources.put(resource.getResource().getUri(), resource);
			}
			return this;
		}

		public StatelessServerNettySpecification resourceTemplates(List<McpSchema.ResourceTemplate> resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(resourceTemplates);
			return this;
		}

		public StatelessServerNettySpecification resourceTemplates(McpSchema.ResourceTemplate... resourceTemplates) {
			Assert.notNull(resourceTemplates, "Resource templates must not be null");
			this.resourceTemplates.addAll(Arrays.asList(resourceTemplates));
			return this;
		}

		public StatelessServerNettySpecification prompts(Map<String, McpStatelessServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			this.prompts.putAll(prompts);
			return this;
		}

		public StatelessServerNettySpecification prompts(List<McpStatelessServerFeatures.PromptSpecification> prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpStatelessServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		public StatelessServerNettySpecification prompts(McpStatelessServerFeatures.PromptSpecification... prompts) {
			Assert.notNull(prompts, "Prompts map must not be null");
			for (McpStatelessServerFeatures.PromptSpecification prompt : prompts) {
				this.prompts.put(prompt.getPrompt().getName(), prompt);
			}
			return this;
		}

		public StatelessServerNettySpecification rootsChangeHandler(
				BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> handler) {
			Assert.notNull(handler, "Consumer must not be null");
			this.rootsChangeHandlers.add(handler);
			return this;
		}


		public StatelessServerNettySpecification rootsChangeHandlers(
				List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			this.rootsChangeHandlers.addAll(handlers);
			return this;
		}

		public StatelessServerNettySpecification rootsChangeHandlers(
				@SuppressWarnings("unchecked") BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>... handlers) {
			Assert.notNull(handlers, "Handlers list must not be null");
			return this.rootsChangeHandlers(Arrays.asList(handlers));
		}

		public StatelessServerNettySpecification objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		public StatelessServerNettySpecification commandExecutor(CommandExecutor commandExecutor) {
			Assert.notNull(commandExecutor, "CommandExecutor must not be null");
			this.commandExecutor = commandExecutor;
			return this;
		}

		public McpStatelessNettyServer build() {
			ObjectMapper mapper = this.objectMapper != null ? this.objectMapper : JsonParser.getObjectMapper();
			return new McpStatelessNettyServer(
					this.transport,
					mapper,
					this.requestTimeout,
					new McpStatelessServerFeatures.McpServerConfig(
							this.serverInfo,
							this.serverCapabilities,
							this.tools,
							this.resources,
							this.resourceTemplates,
							this.prompts,
							this.instructions
					),
					this.commandExecutor,
					this.validator
			);
		}

	}
}
