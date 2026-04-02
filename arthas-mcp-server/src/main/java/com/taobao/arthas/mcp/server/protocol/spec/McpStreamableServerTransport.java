/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.CompletableFuture;

/**
 * MCP可流式服务端传输接口
 *
 * <p>该接口是服务端可流式传输的标记接口，扩展了基本的服务端传输接口。
 * 可流式传输支持通过HTTP流（如SSE）持续向客户端发送消息的能力。
 *
 * <p>主要功能：
 * <ul>
 *   <li>发送带有消息ID的JSON-RPC消息（用于事件流）</li>
 *   <li>将数据反序列化为指定类型的对象</li>
 *   <li>访问底层Netty通道（继承自McpServerTransport）</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface McpStreamableServerTransport extends McpServerTransport {

	/**
	 * 发送带有消息ID的JSON-RPC消息
	 *
	 * <p>该方法发送消息并关联一个消息ID，用于：
	 * <ul>
	 *   <li>在SSE流中标识消息</li>
	 *   <li>支持客户端的断点续传</li>
	 *   <li>跟踪消息的发送顺序</li>
	 * </ul>
	 *
	 * @param message   要发送的JSON-RPC消息对象
	 * @param messageId 消息的唯一标识符，用于在事件流中追踪该消息
	 * @return CompletableFuture，当消息发送完成后完成
	 */
	CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId);

	/**
	 * 将数据反序列化为指定类型的对象
	 *
	 * <p>该方法使用Jackson的ObjectMapper将数据对象转换为指定的Java类型。
	 * 主要用于将JSON-RPC消息中的参数或结果转换为具体的业务对象。
	 *
	 * @param <T>      目标类型
	 * @param value    要反序列化的数据对象（通常是Map或List）
	 * @param typeRef  目标类型的TypeReference，用于处理泛型类型
	 * @return 反序列化后的Java对象
	 */
	<T> T unmarshalFrom(Object value, TypeReference<T> typeRef);
}
