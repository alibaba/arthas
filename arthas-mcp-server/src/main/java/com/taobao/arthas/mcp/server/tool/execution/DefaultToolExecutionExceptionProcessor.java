package com.taobao.arthas.mcp.server.tool.execution;

import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ToolExecutionExceptionProcessor} 的默认实现。
 * <p>
 * 提供两种异常处理策略，通过构造参数 {@code alwaysThrow} 控制：
 * <ul>
 *   <li><b>降级模式（{@code alwaysThrow = false}，默认）</b>：
 *       将异常消息转换为字符串返回，AI 模型可感知工具执行出错并据此做出后续决策。</li>
 *   <li><b>抛出模式（{@code alwaysThrow = true}）</b>：
 *       直接将 {@link ToolExecutionException} 向上抛出，由调用方统一处理，
 *       适用于需要让上层感知并中断流程的场景。</li>
 * </ul>
 * <p>
 * 推荐通过 {@link Builder} 创建实例，例如：
 * <pre>{@code
 * DefaultToolExecutionExceptionProcessor processor = DefaultToolExecutionExceptionProcessor.builder()
 *     .alwaysThrow(true)
 *     .build();
 * }</pre>
 */
public class DefaultToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {

	private final static Logger logger = LoggerFactory.getLogger(DefaultToolExecutionExceptionProcessor.class);

	/**
	 * {@code alwaysThrow} 的默认值：{@code false}，即默认使用降级模式，
	 * 将异常消息转为字符串返回给 AI 模型，而不是向上抛出。
	 */
	private static final boolean DEFAULT_ALWAYS_THROW = false;

	/**
	 * 是否在任何情况下都将异常向上抛出。
	 * <p>
	 * {@code true}：调用 {@link #process} 时直接抛出 {@link ToolExecutionException}；
	 * {@code false}：调用 {@link #process} 时将异常消息转为字符串返回（降级处理）。
	 */
	private final boolean alwaysThrow;

	/**
	 * 通过指定抛出策略创建处理器。
	 *
	 * @param alwaysThrow {@code true} 表示始终抛出异常，{@code false} 表示降级为字符串返回
	 */
	public DefaultToolExecutionExceptionProcessor(boolean alwaysThrow) {
		this.alwaysThrow = alwaysThrow;
	}

	/**
	 * 处理 Tool 执行过程中抛出的 {@link ToolExecutionException}。
	 * <p>
	 * 处理逻辑如下：
	 * <ol>
	 *   <li>首先通过 {@link Assert#notNull} 校验入参不为 {@code null}，
	 *       避免后续操作产生空指针异常。</li>
	 *   <li>若 {@link #alwaysThrow} 为 {@code true}，则直接重新抛出该异常，
	 *       不做任何转换，由调用方处理。</li>
	 *   <li>若 {@link #alwaysThrow} 为 {@code false}（降级模式），
	 *       则打印 debug 级别日志（含 Tool 名称和异常消息），
	 *       并将异常的消息字符串（{@code exception.getMessage()}）作为结果返回，
	 *       使 AI 模型能感知到工具执行出错的原因。</li>
	 * </ol>
	 *
	 * @param exception Tool 执行异常，不能为 {@code null}
	 * @return 异常消息字符串（降级模式下）；抛出模式下此方法不会正常返回
	 * @throws ToolExecutionException 若 {@link #alwaysThrow} 为 {@code true}
	 * @throws IllegalArgumentException 若 {@code exception} 为 {@code null}
	 */
	@Override
	public String process(ToolExecutionException exception) {
		Assert.notNull(exception, "exception cannot be null");
		if (this.alwaysThrow) {
			// 抛出模式：直接将异常向上传播，不做任何降级处理
			throw exception;
		}
		// 降级模式：记录 debug 日志，将异常消息作为字符串返回给 AI 模型
		logger.debug("Exception thrown by tool: {}. Message: {}", exception.getToolDefinition().getName(),
				exception.getMessage());
		return exception.getMessage();
	}

	/**
	 * 创建 {@link Builder} 实例，用于以链式方式构造 {@link DefaultToolExecutionExceptionProcessor}。
	 *
	 * @return 新的 {@link Builder} 实例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * {@link DefaultToolExecutionExceptionProcessor} 的构造器，支持链式调用配置各项参数。
	 */
	public static class Builder {

		/**
		 * 是否始终抛出异常，默认为 {@link DefaultToolExecutionExceptionProcessor#DEFAULT_ALWAYS_THROW}（{@code false}）。
		 */
		private boolean alwaysThrow = DEFAULT_ALWAYS_THROW;

		/**
		 * 设置是否始终将 Tool 执行异常向上抛出。
		 * <p>
		 * {@code true}：异常将直接抛出，调用方需处理；
		 * {@code false}（默认）：异常消息将转为字符串返回给 AI 模型（降级处理）。
		 *
		 * @param alwaysThrow 是否始终抛出异常
		 * @return 当前 Builder 实例，支持链式调用
		 */
		public Builder alwaysThrow(boolean alwaysThrow) {
			this.alwaysThrow = alwaysThrow;
			return this;
		}

		/**
		 * 根据当前 Builder 的配置构造 {@link DefaultToolExecutionExceptionProcessor} 实例。
		 *
		 * @return 配置完成的 {@link DefaultToolExecutionExceptionProcessor} 实例
		 */
		public DefaultToolExecutionExceptionProcessor build() {
			return new DefaultToolExecutionExceptionProcessor(this.alwaysThrow);
		}

	}

}
