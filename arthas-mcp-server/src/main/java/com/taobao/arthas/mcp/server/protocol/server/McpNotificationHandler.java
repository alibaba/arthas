/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;

import java.util.concurrent.CompletableFuture;

/**
 * MCP 通知处理器接口
 * 用于处理来自客户端的 MCP 通知，使用 CompletableFuture 实现异步操作
 * 这是基于 Netty 的实现版本，不依赖 Reactor 框架
 *
 * <p>
 * 通知（Notification）与请求（Request）的区别：
 * <ul>
 * <li>通知是单向的，服务器处理后不需要向客户端发送响应
 * <li>请求是双向的，服务器处理完成后必须向客户端返回响应结果
 * </ul>
 *
 * <p>
 * 实现此接口的处理器可以处理各种类型的客户端通知，
 * 如生命周期事件、状态变更通知等
 */
public interface McpNotificationHandler {

	/**
	 * 处理来自客户端的通知
	 *
	 * @param exchange 与客户端关联的交换对象，用于回调连接的客户端或检查其能力
	 * @param arthasCommandContext Arthas 命令执行上下文，包含命令执行的相关信息
	 * @param params 通知的参数对象，具体类型取决于通知类型
	 * @return CompletableFuture，在通知处理完成后完成
	 *         由于通知不需要响应，返回的 Future 完成时表示处理结束
	 */
	CompletableFuture<Void> handle(McpNettyServerExchange exchange, ArthasCommandContext arthasCommandContext, Object params);

}
