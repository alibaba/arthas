package com.taobao.arthas.mcp.server.tool.execution;

import com.taobao.arthas.mcp.server.tool.definition.ToolDefinition;

/**
 * Tool 执行失败时抛出的运行时异常。
 * <p>
 * 当 MCP Tool 方法在调用过程中抛出任意异常时，框架会将其捕获并包装为本异常，
 * 附带触发异常的 Tool 的定义信息（{@link ToolDefinition}），
 * 再交由 {@link ToolExecutionExceptionProcessor} 统一处理。
 * <p>
 * 继承自 {@link RuntimeException}，无需在方法签名中声明，
 * 可在调用链的任意层次进行捕获处理。
 */
public class ToolExecutionException extends RuntimeException {

	/**
	 * 触发本次异常的 Tool 的定义信息。
	 * <p>
	 * 包含 Tool 的名称、描述、输入 Schema 等元信息，
	 * 可在异常处理或日志记录时用于定位是哪个工具执行失败。
	 */
	private final ToolDefinition toolDefinition;

	/**
	 * 构造 Tool 执行异常。
	 * <p>
	 * 异常消息直接取自原始 {@code cause} 的消息（{@code cause.getMessage()}），
	 * 同时保留原始异常作为 cause，便于完整还原调用栈。
	 *
	 * @param toolDefinition 触发异常的 Tool 的定义信息，不应为 {@code null}
	 * @param cause          Tool 方法执行时抛出的原始异常，不应为 {@code null}
	 */
	public ToolExecutionException(ToolDefinition toolDefinition, Throwable cause) {
		super(cause.getMessage(), cause);
		this.toolDefinition = toolDefinition;
	}

	/**
	 * 获取触发本次异常的 Tool 的定义信息。
	 * <p>
	 * 可通过返回值进一步获取 Tool 名称（{@link ToolDefinition#getName()}）等信息，
	 * 用于日志记录、监控上报或构造用户友好的错误提示。
	 *
	 * @return Tool 定义信息，不为 {@code null}
	 */
	public ToolDefinition getToolDefinition() {
		return this.toolDefinition;
	}

}
