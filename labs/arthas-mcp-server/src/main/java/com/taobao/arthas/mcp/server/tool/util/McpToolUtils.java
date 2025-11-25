package com.taobao.arthas.mcp.server.tool.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.server.McpServerFeatures;
import com.taobao.arthas.mcp.server.protocol.server.McpStatelessServerFeatures;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.tool.ToolContext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class McpToolUtils {

	public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";

    public static final String TOOL_CONTEXT_COMMAND_CONTEXT_KEY = "commandContext";

	public static final String MCP_TRANSPORT_CONTEXT = "mcpTransportContext";

	public static final String PROGRESS_TOKEN = "progressToken";

	private McpToolUtils() {
	}

	public static List<McpServerFeatures.ToolSpecification> toStreamableToolSpecifications(
			List<ToolCallback> tools) {

		if (tools == null || tools.isEmpty()) {
			return Collections.emptyList();
		}

		// De-duplicate tools by their name, keeping the first occurrence of each tool name
		return tools.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(
						tool -> tool.getToolDefinition().getName(), // Key: tool name
						tool -> tool,                               // Value: the tool itself
						(existing, replacement) -> existing          // On duplicate key, keep the existing tool
				))
				.values()
				.stream()
				.filter(McpToolUtils::isStreamableTool)
				.map(McpToolUtils::toToolSpecification)
				.collect(Collectors.toList());
	}

	public static McpServerFeatures.ToolSpecification toToolSpecification(ToolCallback toolCallback) {
		McpSchema.Tool tool = new McpSchema.Tool(
				toolCallback.getToolDefinition().getName(),
				toolCallback.getToolDefinition().getDescription(),
				toolCallback.getToolDefinition().getInputSchema()
		);

		McpServerFeatures.ToolCallFunction callFunction = (exchange, commandContext, request) -> {
			try {
				Map<String, Object> contextMap = new HashMap<>();
				contextMap.put(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange);
				contextMap.put(TOOL_CONTEXT_COMMAND_CONTEXT_KEY, commandContext);
                contextMap.put(PROGRESS_TOKEN, request.progressToken());
				ToolContext toolContext = new ToolContext(contextMap);

				String requestJson = convertParametersToString(request.getArguments());

				String callResult = toolCallback.call(requestJson, toolContext);
				return CompletableFuture.completedFuture(createSuccessResult(callResult));
			} catch (Exception e) {
				return CompletableFuture.completedFuture(createErrorResult(e.getMessage()));
			}
		};
		return new McpServerFeatures.ToolSpecification(tool, callFunction);
	}


	public static List<McpStatelessServerFeatures.ToolSpecification> toStatelessToolSpecifications(List<ToolCallback> providerToolCallbacks) {
		if (providerToolCallbacks == null || providerToolCallbacks.isEmpty()) {
			return Collections.emptyList();
		}

		// De-duplicate tools by their name, keeping the first occurrence of each tool name
		return providerToolCallbacks.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toMap(
						tool -> tool.getToolDefinition().getName(), // Key: tool name
						tool -> tool,                               // Value: the tool itself
						(existing, replacement) -> existing          // On duplicate key, keep the existing tool
				))
				.values()
				.stream()
				.map(McpToolUtils::toStatelessToolSpecification)
				.collect(Collectors.toList());
	}

	public static McpStatelessServerFeatures.ToolSpecification toStatelessToolSpecification(ToolCallback toolCallback) {
		McpSchema.Tool tool = new McpSchema.Tool(
				toolCallback.getToolDefinition().getName(),
				toolCallback.getToolDefinition().getDescription(),
				toolCallback.getToolDefinition().getInputSchema()
		);

		McpStatelessServerFeatures.ToolCallFunction callFunction = (context, commandContext, arguments) -> {
			try {
				Map<String, Object> contextMap = new HashMap<>();
				contextMap.put(MCP_TRANSPORT_CONTEXT, context);
				contextMap.put(TOOL_CONTEXT_COMMAND_CONTEXT_KEY, commandContext);
				ToolContext toolContext = new ToolContext(contextMap);

				String argumentsJson = convertParametersToString(arguments);
				String callResult = toolCallback.call(argumentsJson, toolContext);
				return CompletableFuture.completedFuture(createSuccessResult(callResult));
			} catch (Exception e) {
				return CompletableFuture.completedFuture(createErrorResult("Error executing tool: " + e.getMessage()));
			}
		};

		return new McpStatelessServerFeatures.ToolSpecification(tool, callFunction);
	}

	public static boolean isStreamableTool(ToolCallback toolCallback) {
		return toolCallback.getToolDefinition().isStreamable();
	}


	private static String convertParametersToString(Map<String, Object> parameters) {
		if (parameters == null) {
			return "";
		}
		try {
			return new ObjectMapper().writeValueAsString(parameters);
		} catch (Exception e) {
			return parameters.toString();
		}
	}

	private static McpSchema.CallToolResult createSuccessResult(String content) {
		List<McpSchema.Content> contents = new ArrayList<>();
		String safeContent = (content != null && !content.trim().isEmpty()) ? content : "{}";
		contents.add(new McpSchema.TextContent(safeContent));
        return McpSchema.CallToolResult.builder()
                .content(contents)
                .isError(false)
                .build();
	}

	private static McpSchema.CallToolResult createErrorResult(String errorMessage) {
		List<McpSchema.Content> contents = new ArrayList<>();
		String safeErrorMessage = (errorMessage != null && !errorMessage.trim().isEmpty()) ? 
			errorMessage : "Unknown error occurred";
		contents.add(new McpSchema.TextContent(safeErrorMessage));
        return McpSchema.CallToolResult.builder()
                .content(contents)
                .isError(true)
                .build();
	}

}
