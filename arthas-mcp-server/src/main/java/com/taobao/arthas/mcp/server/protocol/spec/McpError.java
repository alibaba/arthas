/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.JSONRPCResponse.JSONRPCError;

/**
 * MCP协议中JSON-RPC错误的异常表示类
 *
 * <p>该类继承自RuntimeException，用于封装和处理JSON-RPC协议中定义的错误。
 * 它将协议层的错误转换为Java异常，便于在代码中进行错误处理和传播。
 *
 * <p>主要用途：
 * <ul>
 *   <li>封装JSON-RPC错误对象，保留错误码、错误消息和额外数据</li>
 *   <li>提供统一的异常处理机制</li>
 *   <li>支持协议错误和普通错误的转换</li>
 * </ul>
 *
 * @author Yeaury
 */
public class McpError extends RuntimeException {

	// JSON-RPC错误对象，包含错误码、消息和额外数据
	private JSONRPCError jsonRpcError;

	/**
	 * 构造一个包含JSON-RPC错误对象的MCP异常
	 *
	 * @param jsonRpcError JSON-RPC错误对象，包含完整的错误信息
	 */
	public McpError(JSONRPCError jsonRpcError) {
		// 使用错误消息作为异常消息，便于日志记录和调试
		super(jsonRpcError.getMessage());
		this.jsonRpcError = jsonRpcError;
	}

	/**
	 * 构造一个包含普通错误对象的MCP异常
	 *
	 * @param error 任意错误对象，将被转换为字符串作为异常消息
	 */
	public McpError(Object error) {
		// 将错误对象转换为字符串作为异常消息
		super(error.toString());
	}

	/**
	 * 获取JSON-RPC错误对象
	 *
	 * @return JSON-RPC错误对象，包含错误码、消息和数据
	 */
	public JSONRPCError getJsonRpcError() {
		return jsonRpcError;
	}

}
