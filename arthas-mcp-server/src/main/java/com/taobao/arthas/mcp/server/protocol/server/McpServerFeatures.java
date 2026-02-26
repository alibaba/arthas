/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.task.TaskAwareToolSpecification;
import com.taobao.arthas.mcp.server.task.TaskManagerOptions;
import com.taobao.arthas.mcp.server.task.TaskMessageQueue;
import com.taobao.arthas.mcp.server.task.TaskStore;
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
		private final List<TaskAwareToolSpecification> taskTools;
		private final Map<String, ResourceSpecification> resources;
		private final List<McpSchema.ResourceTemplate> resourceTemplates;
		private final Map<String, PromptSpecification> prompts;
		private final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers;
		private final String instructions;

		private final TaskStore<McpSchema.ServerTaskPayloadResult> taskStore;

		private final TaskMessageQueue taskMessageQueue;

		public McpServerConfig(
				McpSchema.Implementation serverInfo,
				McpSchema.ServerCapabilities serverCapabilities,
				List<ToolSpecification> tools,
				List<TaskAwareToolSpecification> taskTools,
				Map<String, ResourceSpecification> resources,
				List<McpSchema.ResourceTemplate> resourceTemplates,
				Map<String, PromptSpecification> prompts,
				List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers,
				String instructions,
				TaskStore<McpSchema.ServerTaskPayloadResult> taskStore,
				TaskMessageQueue taskMessageQueue) {
			
			Assert.notNull(serverInfo, "Server info must not be null");

			// 如果 serverCapabilities 为空，根据提供的功能自动构建合适的能力配置
			if (serverCapabilities == null) {
				serverCapabilities = new McpSchema.ServerCapabilities(
						null, // experimental
						new McpSchema.ServerCapabilities.LoggingCapabilities(),
						!Utils.isEmpty(prompts) ? new McpSchema.ServerCapabilities.PromptCapabilities(false) : null,
						!Utils.isEmpty(resources) ? new McpSchema.ServerCapabilities.ResourceCapabilities(false, false) : null,
						(!Utils.isEmpty(tools) || !Utils.isEmpty(taskTools)) 
								? new McpSchema.ServerCapabilities.ToolCapabilities(false) : null,
						!Utils.isEmpty(taskTools) ? McpSchema.ServerCapabilities.TaskCapabilities.builder()
								.list()
								.cancel()
								.toolsCall()
								.build() : null);
			}

			this.serverInfo = serverInfo;
			this.serverCapabilities = serverCapabilities;
			this.tools = (tools != null) ? tools : Collections.emptyList();
			this.taskTools = (taskTools != null) ? taskTools : Collections.emptyList();
			this.resources = (resources != null) ? resources : Collections.emptyMap();
			this.resourceTemplates = (resourceTemplates != null) ? resourceTemplates : Collections.emptyList();
			this.prompts = (prompts != null) ? prompts : Collections.emptyMap();
			this.rootsChangeConsumers = (rootsChangeConsumers != null) ? rootsChangeConsumers : Collections.emptyList();
			this.instructions = instructions;
			this.taskStore = taskStore;
			this.taskMessageQueue = taskMessageQueue;
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
		
		public List<com.taobao.arthas.mcp.server.task.TaskAwareToolSpecification> getTaskTools() {
			return taskTools;
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

		public TaskStore<McpSchema.ServerTaskPayloadResult> getTaskStore() {
			return taskStore;
		}

		public TaskMessageQueue getTaskMessageQueue() {
			return taskMessageQueue;
		}

		public TaskManagerOptions getTaskOptions() {
			return buildTaskOptions(this.taskStore, this.taskMessageQueue);
		}

		private static TaskManagerOptions buildTaskOptions(
				TaskStore<McpSchema.ServerTaskPayloadResult> taskStore,
				TaskMessageQueue taskMessageQueue) {
			if (taskStore == null && taskMessageQueue == null) {
				return null;
			}
			return TaskManagerOptions.builder()
					.store(taskStore)
					.messageQueue(taskMessageQueue)
					.build();
		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {
			private McpSchema.Implementation serverInfo;
			private McpSchema.ServerCapabilities serverCapabilities;
			private final List<ToolSpecification> tools = new ArrayList<>();
			private final List<com.taobao.arthas.mcp.server.task.TaskAwareToolSpecification> taskTools = new ArrayList<>();
			private final Map<String, ResourceSpecification> resources = new HashMap<>();
			private final List<McpSchema.ResourceTemplate> resourceTemplates = new ArrayList<>();
			private final Map<String, PromptSpecification> prompts = new HashMap<>();
			private final List<BiFunction<McpNettyServerExchange, List<McpSchema.Root>, CompletableFuture<Void>>> rootsChangeConsumers = new ArrayList<>();
			private String instructions;
			private TaskStore<McpSchema.ServerTaskPayloadResult> taskStore;
			private TaskMessageQueue taskMessageQueue;

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

			public Builder addTaskTool(com.taobao.arthas.mcp.server.task.TaskAwareToolSpecification taskTool) {
				this.taskTools.add(taskTool);
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

			public Builder taskStore(TaskStore<McpSchema.ServerTaskPayloadResult> taskStore) {
				this.taskStore = taskStore;
				return this;
			}

			public Builder taskMessageQueue(TaskMessageQueue taskMessageQueue) {
				this.taskMessageQueue = taskMessageQueue;
				return this;
			}

			public McpServerConfig build() {
				return new McpServerConfig(serverInfo, serverCapabilities, tools, taskTools, resources, 
						resourceTemplates, prompts, rootsChangeConsumers, instructions, taskStore, taskMessageQueue);
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
