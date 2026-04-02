/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP可流式服务端传输提供者接口
 *
 * <p>该接口扩展了服务端传输提供者接口，专门用于支持可流式传输的MCP服务器。
 * 可流式传输允许服务器通过HTTP流（如SSE）持续向客户端推送消息。
 *
 * <p>主要功能：
 * <ul>
 *   <li>设置会话工厂，用于创建新的MCP会话</li>
 *   <li>向所有连接的客户端发送通知消息</li>
 *   <li>管理传输提供者的生命周期</li>
 *   <li>声明支持的协议版本</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface McpStreamableServerTransportProvider extends McpServerTransportProvider {

	/**
	 * 设置会话工厂
	 *
	 * <p>该方法设置用于创建新MCP会话的工厂。
	 * 会话工厂负责初始化会话并处理客户端的初始化请求。
	 *
	 * @param sessionFactory 可流式服务端会话工厂实例
	 */
	void setSessionFactory(McpStreamableServerSession.Factory sessionFactory);

	/**
	 * 向所有连接的客户端发送通知消息
	 *
	 * <p>该方法用于广播通知到所有当前连接的客户端。
	 * 通知是单向的，不需要客户端响应。
	 *
	 * @param method 通知方法名，标识通知的类型（如 "tools/list_changed"）
	 * @param params 通知参数，包含通知的具体数据
	 * @return CompletableFuture，当通知发送到所有客户端后完成
	 */
	CompletableFuture<Void> notifyClients(String method, Object params);

	/**
	 * 立即关闭传输提供者（默认实现）
	 *
	 * <p>默认实现调用 {@link #closeGracefully()} 实现优雅关闭。
	 * 子类可以重写此方法以提供立即关闭的实现。
	 */
	default void close() {
		this.closeGracefully();
	}

	/**
	 * 获取传输提供者支持的MCP协议版本列表（默认实现）
	 *
	 * <p>默认实现返回支持2024-11-05版本的协议。
	 * 该版本是MCP可流式传输的初始版本。
	 *
	 * <p>子类可以重写此方法以声明支持不同的协议版本。
	 *
	 * @return 支持的协议版本字符串列表
	 */
	default List<String> protocolVersions() {
		return Arrays.asList(ProtocolVersions.MCP_2024_11_05);
	}

}
