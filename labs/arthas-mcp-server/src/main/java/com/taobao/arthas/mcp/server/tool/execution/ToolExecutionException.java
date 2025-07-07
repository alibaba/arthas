package com.taobao.arthas.mcp.server.tool.execution;

import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;

/**
 * An exception thrown when a tool execution fails.
 */
public class ToolExecutionException extends RuntimeException {

	private final ToolDefinition toolDefinition;

	public ToolExecutionException(ToolDefinition toolDefinition, Throwable cause) {
		super(cause.getMessage(), cause);
		this.toolDefinition = toolDefinition;
	}

	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

}
