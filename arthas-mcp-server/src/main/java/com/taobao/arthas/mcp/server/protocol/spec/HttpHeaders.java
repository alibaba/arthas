/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

/**
 * HTTP协议头常量定义接口
 *
 * <p>该接口定义了MCP协议在HTTP传输层使用的标准HTTP头字段名称。
 * 这些头字段用于MCP客户端和服务器之间的协议协商和会话管理。
 *
 * <p>主要头字段：
 * <ul>
 *   <li>{@link #MCP_SESSION_ID} - 标识特定的MCP会话</li>
 *   <li>{@link #LAST_EVENT_ID} - 用于SSE流的断点续传</li>
 *   <li>{@link #PROTOCOL_VERSION} - 协商MCP协议版本</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface HttpHeaders {

	/**
	 * MCP会话ID头字段
	 *
	 * <p>用于在HTTP请求或响应中标识特定的MCP会话。
	 * 该头字段允许客户端和服务器在有多个并发会话时正确关联消息。
	 *
	 * <p>示例值: "mcp-session-id: session-abc123"
	 */
	String MCP_SESSION_ID = "mcp-session-id";

	/**
	 * 最后事件ID头字段
	 *
	 * <p>用于服务器发送事件(SSE)流中，标识客户端已接收到的最后一个事件的ID。
	 * 该头字段主要用于实现断点续传功能，当客户端重连时可以从上次断开的位置继续接收事件。
	 *
	 * <p>示例值: "last-event-id: event-456"
	 */
	String LAST_EVENT_ID = "last-event-id";

	/**
	 * MCP协议版本头字段
	 *
	 * <p>用于标识客户端或服务器支持的MCP协议版本。
	 * 该头字段在协议握手阶段使用，确保通信双方使用兼容的协议版本。
	 *
	 * <p>支持的版本包括：
	 * <ul>
	 *   <li>2024-11-05 - 初始版本</li>
	 *   <li>2025-03-26 - 第二个版本</li>
	 *   <li>2025-06-18 - 最新版本</li>
	 * </ul>
	 *
	 * <p>示例值: "MCP-Protocol-Version: 2025-06-18"
	 */
	String PROTOCOL_VERSION = "MCP-Protocol-Version";

}
