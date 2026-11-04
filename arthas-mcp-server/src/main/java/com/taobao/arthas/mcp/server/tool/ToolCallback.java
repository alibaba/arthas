package com.taobao.arthas.mcp.server.tool;

import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;

/**
 * Define the basic behavior of the tool
 */
public interface ToolCallback {

    ToolDefinition getToolDefinition();

    String call(String toolInput);

    String call(String toolInput, ToolContext toolContext);
}
