/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * MCP会话接口
 *
 * <p>该接口表示服务端的MCP会话，管理与客户端之间的双向JSON-RPC通信。
 * 每个会话代表一个客户端连接，提供了发送请求、接收响应和发送通知的能力。
 *
 * <p>主要功能：
 * <ul>
 *   <li>向客户端发送JSON-RPC请求并等待响应</li>
 *   <li>向客户端发送单向通知</li>
 *   <li>管理会话的生命周期（优雅关闭或立即关闭）</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface McpSession {

	/**
	 * 向客户端发送JSON-RPC请求
	 *
	 * <p>该方法发送一个需要响应的请求，并返回Future用于异步获取响应。
	 * 请求方法名和参数由调用者指定，响应类型通过TypeReference指定。
	 *
	 * @param <T>           响应结果的类型
	 * @param method        JSON-RPC方法名，标识要调用的远程方法
	 * @param requestParams 请求参数对象，将作为JSON-RPC的params字段发送
	 * @param typeRef       响应类型的TypeReference，用于反序列化响应结果
	 * @return CompletableFuture，完成时包含类型化的响应结果
	 */
	<T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef);

	/**
	 * 向客户端发送无参数的通知（默认实现）
	 *
	 * <p>这是一个便捷方法，用于发送不需要参数的通知。
	 * 内部调用 {@link #sendNotification(String, Object)} 并传入null作为参数。
	 *
	 * @param method 通知方法名，标识通知的类型
	 * @return CompletableFuture，当通知发送完成后完成
	 */
	default CompletableFuture<Void> sendNotification(String method) {
		return sendNotification(method, null);
	}

	/**
	 * 向客户端发送带参数的通知
	 *
	 * <p>通知是单向的消息，不需要客户端响应。
	 * 适用于服务器向客户端推送信息，如状态更新、事件通知等。
	 *
	 * @param method 通知方法名，标识通知的类型
	 * @param params 通知参数对象，将作为JSON-RPC的params字段发送（可为null）
	 * @return CompletableFuture，当通知发送完成后完成
	 */
	CompletableFuture<Void> sendNotification(String method, Object params);

	/**
	 * 优雅地关闭会话
	 *
	 * <p>该方法会：
	 * <ul>
	 *   <li>完成所有正在进行的请求</li>
	 *   <li>等待所有挂起的操作完成</li>
	 *   <li>释放会话相关资源</li>
	 * </ul>
	 *
	 * @return CompletableFuture，当会话优雅关闭后完成
	 */
	CompletableFuture<Void> closeGracefully();

	/**
	 * 立即关闭会话
	 *
	 * <p>该方法会立即关闭会话，不等待正在进行的操作完成。
	 * 默认实现调用 {@link #closeGracefully()}，子类可以重写以提供立即关闭的行为。
	 */
	void close();

}
