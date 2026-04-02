/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;

import java.util.concurrent.CompletableFuture;

/**
 * MCP初始化请求处理器接口
 *
 * 该接口定义了处理客户端MCP初始化请求的契约。
 * 初始化请求是MCP协议握手过程的第一步，客户端通过该请求向服务器声明其能力、
 * 协议版本和客户端信息。服务器响应其服务器能力、支持的协议版本和服务器信息。
 *
 * 该接口使用CompletableFuture进行异步操作，适合Netty等非阻塞IO框架。
 * 这是Netty特定的版本，不依赖于Reactor框架。
 *
 * @see McpSchema.InitializeRequest
 * @see McpSchema.InitializeResult
 */
public interface McpInitRequestHandler {

	/**
	 * 处理客户端的初始化请求
	 *
	 * 该方法负责处理客户端发送的初始化请求，执行以下主要逻辑：
	 * 1. 验证客户端请求的协议版本
	 * 2. 协商双方都支持的协议版本
	 * 3. 返回服务器的能力声明（如支持的工具、资源、提示等）
	 * 4. 返回服务器实现信息（名称、版本等）
	 * 5. 返回可选的使用说明文档
	 *
	 * @param initializeRequest 客户端发送的初始化请求对象，包含：
	 *                          - protocolVersion: 客户端请求的协议版本
	 *                          - capabilities: 客户端支持的能力（工具、资源等）
	 *                          - clientInfo: 客户端信息（名称、版本等）
	 * @return CompletableFuture对象，异步返回初始化结果，包含：
	 *         - protocolVersion: 协商后的协议版本
	 *         - capabilities: 服务器支持的能力
	 *         - serverInfo: 服务器信息
	 *         - instructions: 可选的使用说明
	 */
	CompletableFuture<McpSchema.InitializeResult> handle(McpSchema.InitializeRequest initializeRequest);

}
