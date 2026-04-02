/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * MCP传输层抽象接口
 *
 * <p>该接口定义了MCP协议传输层的核心抽象，负责消息的发送和数据的反序列化。
 * 它是所有MCP传输实现（客户端、服务端、有状态、无状态）的基础接口。
 *
 * <p>主要功能：
 * <ul>
 *   <li>发送JSON-RPC消息</li>
 *   <li>将JSON数据反序列化为指定类型的对象</li>
 *   <li>管理传输层的生命周期（优雅关闭或立即关闭）</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface McpTransport {

	/**
	 * 优雅地关闭传输层
	 *
	 * <p>该方法会：
	 * <ul>
	 *   <li>完成所有正在进行的消息发送</li>
	 *   <li>等待所有挂起的操作完成</li>
	 *   <li>释放传输层相关资源</li>
	 *   <li>关闭底层网络连接</li>
	 * </ul>
	 *
	 * @return CompletableFuture，当所有资源释放完成后完成
	 */
	CompletableFuture<Void> closeGracefully();

	/**
	 * 立即关闭传输层（默认实现）
	 *
	 * <p>默认实现调用 {@link #closeGracefully()} 实现优雅关闭。
	 * 子类可以重写此方法以提供立即关闭的行为。
	 */
	default void close() {
		this.closeGracefully();
	}

	/**
	 * 发送JSON-RPC消息
	 *
	 * <p>该方法将消息通过传输层发送到对端。
	 * 消息可以是请求、响应或通知，由JSONRPCMessage类型表示。
	 *
	 * @param message 要发送的JSON-RPC消息对象
	 * @return CompletableFuture，当消息发送完成后完成
	 */
	CompletableFuture<Void> sendMessage(McpSchema.JSONRPCMessage message);

	/**
	 * 将数据反序列化为指定类型的对象
	 *
	 * <p>该方法使用Jackson的ObjectMapper将数据对象转换为指定的Java类型。
	 * 主要用于将JSON-RPC消息中的参数或结果转换为具体的业务对象。
	 *
	 * @param <T>      目标类型
	 * @param data     要反序列化的数据对象（通常是Map或List）
	 * @param typeRef  目标类型的TypeReference，用于处理泛型类型
	 * @return 反序列化后的Java对象
	 */
	<T> T unmarshalFrom(Object data, TypeReference<T> typeRef);

}
