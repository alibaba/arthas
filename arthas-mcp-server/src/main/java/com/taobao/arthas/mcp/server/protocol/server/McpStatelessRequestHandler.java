/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.session.ArthasCommandContext;

import java.util.concurrent.CompletableFuture;

/**
 * 无状态MCP服务器的请求处理器接口
 * 用于处理MCP协议中的请求消息
 *
 * @param <R> 请求响应结果的类型
 */
public interface McpStatelessRequestHandler<R> {

	/**
	 * 处理请求消息并返回结果
	 *
	 * @param transportContext 与传输层关联的传输上下文对象
	 * @param arthasCommandContext Arthas命令上下文，包含命令执行的环境信息
	 * @param params MCP请求的消息体内容
	 * @return 异步Future，完成时包含响应对象
	 */
	CompletableFuture<R> handle(McpTransportContext transportContext, ArthasCommandContext arthasCommandContext, Object params);

}
