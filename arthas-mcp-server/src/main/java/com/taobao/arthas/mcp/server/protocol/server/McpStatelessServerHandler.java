/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;

/**
 * MCP无状态服务器处理器接口
 *
 * 该接口定义了MCP无状态服务器的核心处理方法，用于处理MCP协议的请求和通知。
 * 无状态意味着每个请求都是独立的，不依赖于之前的请求状态。
 */
public interface McpStatelessServerHandler {

	/**
	 * 处理MCP请求
	 *
	 * 使用用户提供的功能实现来处理请求。此方法接收传输上下文和请求对象，
	 * 并返回异步响应结果。
	 *
	 * @param transportContext MCP传输上下文，承载传输层的元数据信息
	 *                         {@link McpTransportContext}
	 * @param request MCP请求的JSON对象
	 * @return 异步结果，包含JSON响应对象
	 */
	CompletableFuture<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext transportContext,
													  McpSchema.JSONRPCRequest request);

	/**
	 * 处理MCP通知
	 *
	 * 处理MCP协议的通知消息。通知与请求不同，不需要返回响应。
	 * 此方法在处理完成后返回一个完成的Future。
	 *
	 * @param transportContext MCP传输上下文，承载传输层的元数据信息
	 *                         {@link McpTransportContext}
	 * @param notification MCP通知的JSON对象
	 * @return 异步结果，在处理完成后完成
	 */
	CompletableFuture<Void> handleNotification(McpTransportContext transportContext, McpSchema.JSONRPCNotification notification);

}
