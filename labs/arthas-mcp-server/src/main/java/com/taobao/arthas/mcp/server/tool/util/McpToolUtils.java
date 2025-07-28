package com.taobao.arthas.mcp.server.tool.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.protocol.config.McpServerProperties;
import com.taobao.arthas.mcp.server.protocol.server.McpNettyServerExchange;
import com.taobao.arthas.mcp.server.protocol.server.McpServerFeatures;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.tool.ToolContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public final class McpToolUtils {

	public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";

	private McpToolUtils() {
	}

    public static List<McpServerFeatures.ToolSpecification> toToolSpecifications(
            List<ToolCallback> tools,
            McpServerProperties serverProperties) {

        // De-duplicate tools by their name, keeping the first occurrence of each tool name
        return tools.stream()
                .collect(Collectors.toMap(
                        tool -> tool.getToolDefinition().getName(), // Key: tool name
                        tool -> tool,                               // Value: the tool itself
                        (existing, replacement) -> existing          // On duplicate key, keep the existing tool
                ))
                .values()
                .stream()
                .map(McpToolUtils::toToolSpecification)
                .collect(Collectors.toList());
    }

    public static McpServerFeatures.ToolSpecification toToolSpecification(ToolCallback toolCallback) {

        McpSchema.Tool tool = new McpSchema.Tool(toolCallback.getToolDefinition().getName(),
                toolCallback.getToolDefinition().getDescription(), toolCallback.getToolDefinition().getInputSchema());

        return new McpServerFeatures.ToolSpecification(tool, (exchange, request) -> {
            try {
                Map<String, Object> contextMap = new HashMap<>();
                contextMap.put(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange);
                ToolContext toolContext = new ToolContext(contextMap);

                String callResult = toolCallback.call(convertRequestToString(request), toolContext);

                List<McpSchema.Content> contents = new ArrayList<>();
                contents.add(new McpSchema.TextContent(callResult));

                return CompletableFuture.completedFuture(new McpSchema.CallToolResult(contents, false));
            } catch (Exception e) {
                List<McpSchema.Content> contents = new ArrayList<>();
                contents.add(new McpSchema.TextContent(e.getMessage()));

                return CompletableFuture.completedFuture(new McpSchema.CallToolResult(contents, true));
            }
        });
    }

}
