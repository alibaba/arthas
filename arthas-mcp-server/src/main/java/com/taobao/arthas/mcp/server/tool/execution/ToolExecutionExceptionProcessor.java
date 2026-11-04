package com.taobao.arthas.mcp.server.tool.execution;

/**
 * A functional interface to process a {@link ToolExecutionException} by either converting
 * the error message to a String that can be sent back to the AI model or throwing an
 * exception to be handled by the caller.
 */
@FunctionalInterface
public interface ToolExecutionExceptionProcessor {

	/**
	 * Convert an exception thrown by a tool to a String that can be sent back to the AI
	 * model or throw an exception to be handled by the caller.
	 */
	String process(ToolExecutionException exception);

}
