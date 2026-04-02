/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;

import java.util.concurrent.CompletableFuture;

/**
 * MCP 请求处理器接口
 * 用于处理来自客户端的 MCP 请求，使用 CompletableFuture 实现异步操作
 * 这是基于 Netty 的实现版本，不依赖 Reactor 框架
 *
 * <p>
 * 请求（Request）与通知（Notification）的区别：
 * <ul>
 * <li>请求是双向的，服务器处理完成后必须向客户端返回响应结果
 * <li>通知是单向的，服务器处理后不需要向客户端发送响应
 * </ul>
 *
 * <p>
 * 实现此接口的处理器可以处理各种类型的客户端请求，
 * 如工具调用、资源访问、提示获取等
 *
 * @param <T> 响应结果的类型，取决于具体的请求类型
 */
public interface McpRequestHandler<T> {

	/**
	 * 处理来自客户端的请求
	 *
	 * @param exchange 与客户端关联的交换对象，用于回调连接的客户端或检查其能力
	 * @param arthasCommandContext Arthas 命令执行上下文，包含命令执行的相关信息
	 * @param params 请求的参数对象，具体类型取决于请求类型
	 * @return CompletableFuture，将发出请求的响应结果
	 *         泛型 T 表示响应结果的类型，处理完成后必须返回响应
	 */
	CompletableFuture<T> handle(McpNettyServerExchange exchange, ArthasCommandContext arthasCommandContext, Object params);

}
