package com.taobao.arthas.mcp.server.tool.execution;

/**
 * 处理 Tool 执行异常的函数式接口。
 * <p>
 * 当 MCP Tool 方法在执行过程中抛出异常时，框架会将异常包装为 {@link ToolExecutionException}，
 * 并交由本接口的实现进行处理。处理策略有两种：
 * <ol>
 *   <li><b>降级为字符串</b>：将异常信息转换为可读字符串，返回给 AI 模型，
 *       让 AI 模型感知到工具执行出错并据此做出后续决策（例如重试或使用备选方案）。</li>
 *   <li><b>向上抛出</b>：将异常原样或重新包装后抛出，由调用方（通常是 MCP 请求处理链）
 *       统一处理，适用于需要让调用方感知错误的场景。</li>
 * </ol>
 * <p>
 * 框架提供了默认实现 {@link DefaultToolExecutionExceptionProcessor}，
 * 默认策略为降级（返回错误消息字符串），可通过 Builder 配置为强制抛出。
 * <p>
 * 作为函数式接口（{@link FunctionalInterface}），也可以通过 lambda 表达式快速提供自定义实现。
 */
@FunctionalInterface
public interface ToolExecutionExceptionProcessor {

	/**
	 * 处理 Tool 执行过程中抛出的异常。
	 * <p>
	 * 实现方可以选择：
	 * <ul>
	 *   <li>返回一个描述错误的字符串，框架将其作为 Tool 调用结果发送给 AI 模型</li>
	 *   <li>抛出任意 {@link RuntimeException}，由调用方捕获并处理</li>
	 * </ul>
	 *
	 * @param exception Tool 执行异常，包含触发异常的 Tool 定义信息和原始错误原因，不为 {@code null}
	 * @return 描述错误的字符串，将作为 Tool 调用结果发送给 AI 模型；
	 *         若实现选择抛出异常，则此方法不会正常返回
	 * @throws RuntimeException 若实现决定将异常向上传播
	 */
	String process(ToolExecutionException exception);

}
