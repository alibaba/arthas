package com.taobao.arthas.mcp.server.tool;

import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;

/**
 * 工具回调接口
 * 定义了工具的基本行为规范，所有MCP服务器的工具实现都需要实现此接口
 * 提供工具的定义信息和执行调用的能力
 */
public interface ToolCallback {

    /**
     * 获取工具定义
     * 返回该工具的元数据信息，包括工具名称、描述、参数配置等
     *
     * @return 工具定义对象，包含工具的完整元数据
     */
    ToolDefinition getToolDefinition();

    /**
     * 执行工具调用（无上下文版本）
     * 使用指定的输入参数执行工具功能
     *
     * @param toolInput 工具输入参数，通常为JSON格式的字符串
     * @return 工具执行结果的字符串表示
     */
    String call(String toolInput);

    /**
     * 执行工具调用（带上下文版本）
     * 使用指定的输入参数和工具上下文执行工具功能
     * 上下文可以提供额外的执行环境信息，如会话数据、用户信息等
     *
     * @param toolInput    工具输入参数，通常为JSON格式的字符串
     * @param toolContext 工具执行上下文，包含执行环境的相关信息
     * @return 工具执行结果的字符串表示
     */
    String call(String toolInput, ToolContext toolContext);
}
