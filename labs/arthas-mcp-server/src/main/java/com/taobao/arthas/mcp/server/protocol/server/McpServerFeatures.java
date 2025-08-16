package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.util.Assert;
import com.taobao.arthas.mcp.server.util.Utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * MCP server function specification, the server can choose the supported features.
 * This implementation only provides an asynchronous API.
 *
 * @author Yeaury
 */
public class McpServerFeatures {

	public static class McpServerConfig {
		private final McpSchema.Implementation serverInfo;
		private final McpSchema.ServerCapabilities serverCapabilities;
		private final List<ToolSpecification> tools;
		private final Map<String, ResourceSpecification> resources;
		private final List<McpSchema.ResourceTemplate> resourceTemplates;
		private final Map<String, PromptSpecification> prompts;
		private final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers;
		private final String instructions;

		public McpServerConfig(
				McpSchema.Implementation serverInfo,
				McpSchema.ServerCapabilities serverCapabilities,
				List<ToolSpecification> tools,
				Map<String, ResourceSpecification> resources,
				List<McpSchema.ResourceTemplate> resourceTemplates,
				Map<String, PromptSpecification> prompts,
				List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers,
				String instructions) {
			
			Assert.notNull(serverInfo, "The server information cannot be empty");

			// If serverCapabilities is empty, the appropriate capability configuration
			// is automatically built based on the provided capabilities
			if (serverCapabilities == null) {
				serverCapabilities = new McpSchema.ServerCapabilities(
						null, // experimental
						new McpSchema.ServerCapabilities.LoggingCapabilities(),
						!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
						!Utils.isEmpty(resources) ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
						!Utils.isEmpty(tools) ? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null);
			}

			this.tools = (tools != null) ? tools : Collections.emptyList();
			this.resources = (resources != null) ? resources : Collections.emptyMap();
			this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : Collections.emptyList();
			this.prompts = (prompts != null) ? prompts : Collections.emptyMap();
			this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : Collections.emptyList();
			this.serverInfo = serverInfo;
			this.serverCapabilities = serverCapabilities;
			this.instructions = instructions;
		}

		public McpSchema.Implementation getServerInfo() {
			return serverInfo;
		}

		public McpSchema.ServerCapabilities getServerCapabilities() {
			return serverCapabilities;
		}

		public List<ToolSpecification> getTools() {
			return tools;
		}

		public Map<String, ResourceSpecification> getResources() {
			return resources;
		}

		public List<McpSchema.ResourceTemplate> getResourceTemplates() {
			return resourceTemplates;
		}

		public Map<String, PromptSpecification> getPrompts() {
			return prompts;
		}

		public List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> getRootsChangeConsumers() {
			return rootsChangeConsumers;
		}

		public String getInstructions() {
			return instructions;
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private McpSchema.Implementation serverInfo;
			private McpSchema.ServerCapabilities serverCapabilities;
			private final List<ToolSpecification> tools = new ArrayList<>();
			private final Map<String, ResourceSpecification> resources = new HashMap<>();
			private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();
			private final Map<String, PromptSpecification> prompts = new HashMap<>();
			private final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers = new ArrayList<>();
			private String instructions;

			public Builder serverInfo(McpSchema.Implementation serverInfo) {
				this.serverInfo = serverInfo;
				return this;
			}

			public Builder serverCapabilities(McpSchema.ServerCapabilities serverCapabilities) {
				this.serverCapabilities = serverCapabilities;
				return this;
			}

			public Builder addTool(ToolSpecification tool) {
				this.tools.add(tool);
				return this;
			}

			public Builder addResource(String key, ResourceSpecification resource) {
				this.resources.put(key, resource);
				return this;
			}

			public Builder addResourceTemplate(McpSchema.ResourceTemplate template) {
				this.resourceTemplates.add(template);
				return this;
			}

			public Builder addPrompt(String key, PromptSpecification prompt) {
				this.prompts.put(key, prompt);
				return this;
			}

			public Builder addRootsChangeConsumer(
					BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>> consumer) {
				this.rootsChangeConsumers.add(consumer);
				return this;
			}

			public Builder instructions(String instructions) {
				this.instructions = instructions;
				return this;
			}

			public McpServerConfig build() {
				return new McpServerConfig(serverInfo, serverCapabilities, tools, resources, resourceTemplates, prompts,
						rootsChangeConsumers, instructions);
			}
		}
	}

	public static class ToolSpecification {
		private final McpSchema.Tool tool;
		private final ToolCallFunction call;

		public ToolSpecification(
				McpSchema.Tool tool,
				ToolCallFunction call) {
			this.tool = tool;
			this.call = call;
		}

		public McpSchema.Tool getTool() {
			return tool;
		}

		public ToolCallFunction getCall() {
			return call;
		}
	}

	/**
	 * Tool call function interface with three parameters
	 */
	@FunctionalInterface
	public interface ToolCallFunction {
		CompletableFuture<McpSchema.CallToolResult> apply(
				McpNettyServerExchange exchange,
				ArthasCommandContext commandContext,
				McpSchema.CallToolRequest arguments
		);
	}

	public static class ResourceSpecification {
		private final McpSchema.Resource resource;
		private final BiFunction<McpNettyServerExchange, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> readHandler;

		public ResourceSpecification(
				McpSchema.Resource resource,
				BiFunction<McpNettyServerExchange, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> readHandler) {
			this.resource = resource;
			this.readHandler = readHandler;
		}

		public McpSchema.Resource getResource() {
			return resource;
		}

		public BiFunction<McpNettyServerExchange, McpSchema.ReadResourceRequest, CompletableFuture<McpSchema.ReadResourceResult>> getReadHandler() {
			return readHandler;
		}
	}

	public static class PromptSpecification {
		private final McpSchema.Prompt prompt;
		private final BiFunction<McpNettyServerExchange, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> promptHandler;

		public PromptSpecification(
				McpSchema.Prompt prompt,
				BiFunction<McpNettyServerExchange, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> promptHandler) {
			this.prompt = prompt;
			this.promptHandler = promptHandler;
		}

		public McpSchema.Prompt getPrompt() {
			return prompt;
		}

		public BiFunction<McpNettyServerExchange, McpSchema.GetPromptRequest, CompletableFuture<McpSchema.GetPromptResult>> getPromptHandler() {
			return promptHandler;
		}
	}
}
