package com.taobao.arthas.core.mcp.tool.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.protocol.server.McpServerFeatures;
import com.taobao.arthas.mcp.server.protocol.server.McpStatelessServerFeatures;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.tool.ToolCallback;
import com.taobao.arthas.mcp.server.tool.ToolContext;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * MCP工具工具类
 * 提供MCP工具相关的转换和处理功能
 *
 * @author Yeaury
 */
public final class McpToolUtils {

    /**
     * 工具上下文中MCP交换器的键名
     * 用于在工具上下文中存储MCP交换器对象
     */
    public static final String TOOL_CONTEXT_MCP_EXCHANGE_KEY = "exchange";

    /**
     * 工具上下文中命令上下文的键名
     * 用于在工具上下文中存储Arthas命令上下文对象
     */
    public static final String TOOL_CONTEXT_COMMAND_CONTEXT_KEY = "commandContext";

    /**
     * MCP传输上下文的键名
     * 用于存储MCP传输层的上下文信息，包含认证信息等
     */
    public static final String MCP_TRANSPORT_CONTEXT = "mcpTransportContext";

    /**
     * 进度令牌的键名
     * 用于在工具执行过程中发送进度通知
     */
    public static final String PROGRESS_TOKEN = "progressToken";

    /**
     * 私有构造函数，防止实例化
     * 这是一个工具类，所有方法都是静态的
     */
    private McpToolUtils() {
    }

    /**
     * 将工具回调列表转换为可流式处理的工具规范列表
     * 该方法会对工具进行去重，保留每个工具名称的第一个出现项
     *
     * @param tools 工具回调列表
     * @return 工具规范列表，如果输入为空则返回空列表
     */
    public static List<McpServerFeatures.ToolSpecification> toStreamableToolSpecifications(
            List<ToolCallback> tools) {

        // 如果工具列表为空或null，返回空列表
        if (tools == null || tools.isEmpty()) {
            return Collections.emptyList();
        }

        // 通过工具名称进行去重，保留每个工具名称的第一个出现项
        // 使用Collectors.toMap收集，key为工具名称，value为工具本身
        // 当遇到重复的key时，保留已有的值(existing)，丢弃新值(replacement)
        return tools.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        tool -> tool.getToolDefinition().getName(), // Key: 工具名称
                        tool -> tool,                               // Value: 工具本身
                        (existing, replacement) -> existing          // 当key重复时，保留已有的工具
                ))
                .values()
                .stream()
                .map(McpToolUtils::toToolSpecification)
                .collect(Collectors.toList());
    }

    /**
     * 将单个工具回调转换为工具规范
     * 工具规范包含工具的定义和调用函数
     *
     * @param toolCallback 工具回调对象
     * @return MCP服务器工具规范
     */
    public static McpServerFeatures.ToolSpecification toToolSpecification(ToolCallback toolCallback) {
        // 创建MCP工具定义对象，包含名称、描述和输入模式
        McpSchema.Tool tool = new McpSchema.Tool(
                toolCallback.getToolDefinition().getName(),
                toolCallback.getToolDefinition().getDescription(),
                toolCallback.getToolDefinition().getInputSchema()
        );

        // 创建工具调用函数，该函数会在工具被调用时执行
        McpServerFeatures.ToolCallFunction callFunction = (exchange, commandContext, request) -> {
            try {
                // 创建工具上下文Map，用于传递各种上下文信息
                Map<String, Object> contextMap = new HashMap<>();
                // 添加MCP交换器到上下文
                contextMap.put(TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange);
                // 添加Arthas命令上下文
                contextMap.put(TOOL_CONTEXT_COMMAND_CONTEXT_KEY, commandContext);
                // 添加进度令牌，用于发送进度通知
                contextMap.put(PROGRESS_TOKEN, request.progressToken());
                // 如果交换器不为空且存在传输上下文，则添加到上下文中
                // 这使得可流式工具能够访问认证信息
                if (exchange != null && exchange.getTransportContext() != null) {
                    contextMap.put(MCP_TRANSPORT_CONTEXT, exchange.getTransportContext());
                }
                // 创建工具上下文对象
                ToolContext toolContext = new ToolContext(contextMap);

                // 将请求参数转换为JSON字符串
                String requestJson = convertParametersToString(request.getArguments());

                // 调用工具回调，执行实际的工具逻辑
                String callResult = toolCallback.call(requestJson, toolContext);
                // 将成功结果封装为CompletableFuture返回
                return CompletableFuture.completedFuture(createSuccessResult(callResult));
            } catch (Exception e) {
                // 如果发生异常，将错误消息封装为CompletableFuture返回
                return CompletableFuture.completedFuture(createErrorResult(e.getMessage()));
            }
        };
        // 返回包含工具定义和调用函数的工具规范
        return new McpServerFeatures.ToolSpecification(tool, callFunction);
    }


    /**
     * 将工具回调列表转换为无状态工具规范列表
     * 该方法会对工具进行去重，保留每个工具名称的第一个出现项
     *
     * @param providerToolCallbacks 工具提供者的工具回调列表
     * @return 无状态工具规范列表，如果输入为空则返回空列表
     */
    public static List<McpStatelessServerFeatures.ToolSpecification> toStatelessToolSpecifications(List<ToolCallback> providerToolCallbacks) {
        // 如果工具回调列表为空或null，返回空列表
        if (providerToolCallbacks == null || providerToolCallbacks.isEmpty()) {
            return Collections.emptyList();
        }

        // 通过工具名称进行去重，保留每个工具名称的第一个出现项
        // 使用Collectors.toMap收集，key为工具名称，value为工具本身
        // 当遇到重复的key时，保留已有的值(existing)，丢弃新值(replacement)
        return providerToolCallbacks.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        tool -> tool.getToolDefinition().getName(), // Key: 工具名称
                        tool -> tool,                               // Value: 工具本身
                        (existing, replacement) -> existing          // 当key重复时，保留已有的工具
                ))
                .values()
                .stream()
                .map(McpToolUtils::toStatelessToolSpecification)
                .collect(Collectors.toList());
    }

    /**
     * 将单个工具回调转换为无状态工具规范
     * 无状态工具规范用于无状态的MCP服务器
     *
     * @param toolCallback 工具回调对象
     * @return 无状态工具规范
     */
    public static McpStatelessServerFeatures.ToolSpecification toStatelessToolSpecification(ToolCallback toolCallback) {
        // 创建MCP工具定义对象，包含名称、描述和输入模式
        McpSchema.Tool tool = new McpSchema.Tool(
                toolCallback.getToolDefinition().getName(),
                toolCallback.getToolDefinition().getDescription(),
                toolCallback.getToolDefinition().getInputSchema()
        );

        // 创建工具调用函数，该函数会在工具被调用时执行
        McpStatelessServerFeatures.ToolCallFunction callFunction = (context, commandContext, arguments) -> {
            try {
                // 创建工具上下文Map，用于传递各种上下文信息
                Map<String, Object> contextMap = new HashMap<>();
                // 添加MCP传输上下文（包含认证信息等）
                contextMap.put(MCP_TRANSPORT_CONTEXT, context);
                // 添加Arthas命令上下文
                contextMap.put(TOOL_CONTEXT_COMMAND_CONTEXT_KEY, commandContext);
                // 创建工具上下文对象
                ToolContext toolContext = new ToolContext(contextMap);

                // 将参数转换为JSON字符串
                String argumentsJson = convertParametersToString(arguments);
                // 调用工具回调，执行实际的工具逻辑
                String callResult = toolCallback.call(argumentsJson, toolContext);
                // 将成功结果封装为CompletableFuture返回
                return CompletableFuture.completedFuture(createSuccessResult(callResult));
            } catch (Exception e) {
                // 如果发生异常，将错误消息封装为CompletableFuture返回
                return CompletableFuture.completedFuture(createErrorResult("Error executing tool: " + e.getMessage()));
            }
        };

        // 返回包含工具定义和调用函数的无状态工具规范
        return new McpStatelessServerFeatures.ToolSpecification(tool, callFunction);
    }

    /**
     * 将参数Map转换为JSON字符串
     *
     * @param parameters 参数Map
     * @return JSON字符串，如果参数为null则返回空字符串，转换失败则返回toString结果
     */
    private static String convertParametersToString(Map<String, Object> parameters) {
        // 如果参数为null，返回空字符串
        if (parameters == null) {
            return "";
        }
        try {
            // 使用Jackson ObjectMapper将参数转换为JSON字符串
            return new ObjectMapper().writeValueAsString(parameters);
        } catch (Exception e) {
            // 如果转换失败，返回参数的toString结果
            return parameters.toString();
        }
    }

    /**
     * 创建成功调用的结果对象
     *
     * @param content 结果内容
     * @return MCP工具调用结果对象
     */
    private static McpSchema.CallToolResult createSuccessResult(String content) {
        // 创建内容列表
        List<McpSchema.Content> contents = new ArrayList<>();
        // 确保内容不为空，如果为空或仅包含空白字符，则使用"{}"作为默认值
        String safeContent = (content != null && !content.trim().isEmpty()) ? content : "{}";
        // 将内容添加为文本内容
        contents.add(new McpSchema.TextContent(safeContent));
        // 构建并返回成功的结果对象
        return McpSchema.CallToolResult.builder()
                .content(contents)
                .isError(false)
                .build();
    }

    /**
     * 创建错误调用的结果对象
     *
     * @param errorMessage 错误消息
     * @return MCP工具调用结果对象，标记为错误状态
     */
    private static McpSchema.CallToolResult createErrorResult(String errorMessage) {
        // 创建内容列表
        List<McpSchema.Content> contents = new ArrayList<>();
        // 确保错误消息不为空，如果为空则使用默认错误消息
        String safeErrorMessage = (errorMessage != null && !errorMessage.trim().isEmpty()) ?
            errorMessage : "Unknown error occurred";
        // 将错误消息添加为文本内容
        contents.add(new McpSchema.TextContent(safeErrorMessage));
        // 构建并返回错误的结果对象
        return McpSchema.CallToolResult.builder()
                .content(contents)
                .isError(true)
                .build();
    }

}
