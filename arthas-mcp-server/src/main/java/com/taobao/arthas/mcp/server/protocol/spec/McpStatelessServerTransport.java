/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.protocol.server.McpStatelessServerHandler;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP无状态服务端传输接口
 *
 * <p>该接口定义了无状态MCP服务器的传输层抽象。
 * 与有状态会话不同，无状态服务器不维护客户端会话状态，每个请求独立处理。
 *
 * <p>主要功能：
 * <ul>
 *   <li>设置无状态MCP处理器，处理传入的请求</li>
 *   <li>管理传输层的生命周期（关闭操作）</li>
 *   <li>声明支持的协议版本</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface McpStatelessServerTransport {

	/**
	 * 设置无状态MCP处理器
	 *
	 * <p>该方法设置用于处理传入MCP请求的处理器。
	 * 处理器负责解析请求、执行业务逻辑并生成响应。
	 *
	 * @param mcpHandler 无状态MCP服务器处理器实例
	 */
	void setMcpHandler(McpStatelessServerHandler mcpHandler);

	/**
	 * 立即关闭传输层（默认实现）
	 *
	 * <p>默认实现调用 {@link #closeGracefully()} 实现优雅关闭。
	 * 子类可以重写此方法以提供立即关闭的实现。
	 */
	default void close() {
		this.closeGracefully();
	}

	/**
	 * 优雅地关闭传输层
	 *
	 * <p>该方法会：
	 * <ul>
	 *   <li>停止接受新的请求</li>
	 *   <li>完成所有正在进行的请求处理</li>
	 *   <li>释放传输层相关资源</li>
	 * </ul>
	 *
	 * @return CompletableFuture，当所有资源释放完成后完成
	 */
	CompletableFuture<Void> closeGracefully();

	/**
	 * 获取传输层支持的MCP协议版本列表（默认实现）
	 *
	 * <p>默认实现返回支持的协议版本：
	 * <ul>
	 *   <li>2025-03-26</li>
	 *   <li>2025-06-18</li>
	 * </ul>
	 *
	 * <p>子类可以重写此方法以声明支持不同的协议版本。
	 *
	 * @return 支持的协议版本字符串列表
	 */
	default List<String> protocolVersions() {
		return Arrays.asList(ProtocolVersions.MCP_2025_03_26, ProtocolVersions.MCP_2025_06_18);
	}

}
