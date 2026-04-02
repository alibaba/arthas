/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;


import java.util.concurrent.CompletableFuture;

/**
 * 无状态MCP服务器的通知处理器接口
 * 用于处理MCP协议中的通知消息
 */
public interface McpStatelessNotificationHandler {

	/**
	 * 处理通知消息
	 *
	 * @param transportContext 与传输层关联的传输上下文对象
	 * @param params MCP通知的消息体内容
	 * @return 异步Future，在处理完成时完成
	 */
	CompletableFuture<Void> handle(McpTransportContext transportContext, Object params);

}
