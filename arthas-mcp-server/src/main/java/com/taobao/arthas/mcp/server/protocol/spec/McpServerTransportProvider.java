/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.protocol.server.handler.McpStreamableHttpRequestHandler;

import java.util.concurrent.CompletableFuture;

/**
 * MCP服务端传输提供者接口
 *
 * <p>该接口定义了MCP服务端传输提供者的抽象，负责管理传输层的生命周期和操作。
 * 传输提供者是服务端和底层传输机制之间的桥梁，提供了统一的服务端操作接口。
 *
 * <p>主要功能：
 * <ul>
 *   <li>向所有连接的客户端发送通知消息</li>
 *   <li>优雅地关闭传输层，释放资源</li>
 *   <li>提供HTTP请求处理器用于处理MCP协议请求</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface McpServerTransportProvider {

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
	 * 优雅地关闭传输提供者
	 *
	 * <p>该方法会：
	 * <ul>
	 *   <li>停止接受新的连接</li>
	 *   <li>完成所有正在进行的请求处理</li>
	 *   <li>关闭所有活跃的客户端连接</li>
	 *   <li>释放相关资源</li>
	 * </ul>
	 *
	 * @return CompletableFuture，当所有资源释放完成后完成
	 */
	CompletableFuture<Void> closeGracefully();

	/**
	 * 立即关闭传输提供者（默认实现）
	 *
	 * <p>该方法调用 {@link #closeGracefully()} 实现优雅关闭。
	 * 子类可以重写此方法以提供立即关闭的实现。
	 */
	default void close() {
		closeGracefully();
	}

	/**
	 * 获取MCP HTTP请求处理器
	 *
	 * <p>返回的处理器用于处理传入的HTTP请求，并将其转换为MCP协议消息。
	 * 该处理器是MCP服务器处理HTTP传输的核心组件。
	 *
	 * @return MCP HTTP请求处理器实例
	 */
	McpStreamableHttpRequestHandler getMcpRequestHandler();

}
