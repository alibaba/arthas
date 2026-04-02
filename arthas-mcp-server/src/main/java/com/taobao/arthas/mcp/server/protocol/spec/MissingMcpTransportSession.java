/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.CompletableFuture;

/**
 * 缺失的MCP传输会话的占位实现
 *
 * <p>该类是McpSession接口的一个占符实现，表示传输会话缺失或不可用。
 * 当没有活动的传输可用但尝试会话操作时，使用此类作为默认会话。
 *
 * <p>主要用途：
 * <ul>
 *   <li>作为会话未初始化时的默认值</li>
 *   <li>在传输断开时替换活动会话</li>
 *   <li>防止空指针异常，提供明确的错误信息</li>
 * </ul>
 *
 * <p>所有方法都会返回一个已异常完成的Future，异常消息指示会话不可用。
 *
 * @author Yeaury
 */
public class MissingMcpTransportSession implements McpSession {

	// 会话标识符，用于错误消息中标识是哪个会话不可用
	private final String sessionId;

	/**
	 * 构造一个缺失传输会话的占符对象
	 *
	 * @param sessionId 会话标识符
	 */
	public MissingMcpTransportSession(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * 尝试发送请求（操作失败）
	 *
	 * <p>由于传输不可用，该方法返回一个已异常完成的Future。
	 *
	 * @param <T>           响应结果的类型
	 * @param method        JSON-RPC方法名
	 * @param requestParams 请求参数对象
	 * @param typeRef       响应类型的TypeReference
	 * @return 已异常完成的CompletableFuture，包含IllegalStateException
	 */
	@Override
	public <T> CompletableFuture<T> sendRequest(String method, Object requestParams, TypeReference<T> typeRef) {
		CompletableFuture<T> future = new CompletableFuture<>();
		// 标记Future为异常完成，错误信息指出流不可用
		future.completeExceptionally(
				new IllegalStateException("Stream unavailable for session " + this.sessionId)
		);
		return future;
	}

	/**
	 * 尝试发送通知（操作失败）
	 *
	 * <p>由于传输不可用，该方法返回一个已异常完成的Future。
	 *
	 * @param method 通知方法名
	 * @param params 通知参数对象
	 * @return 已异常完成的CompletableFuture，包含IllegalStateException
	 */
	@Override
	public CompletableFuture<Void> sendNotification(String method, Object params) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		// 标记Future为异常完成，错误信息指出流不可用
		future.completeExceptionally(
				new IllegalStateException("Stream unavailable for session " + this.sessionId)
		);
		return future;
	}

	/**
	 * 优雅关闭会话（空操作）
	 *
	 * <p>由于会话已经缺失，关闭操作是空操作，立即返回。
	 *
	 * @return 已完成的CompletableFuture
	 */
	@Override
	public CompletableFuture<Void> closeGracefully() {
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * 立即关闭会话（空操作）
	 *
	 * <p>由于会话已经缺失，关闭操作是空操作。
	 */
	@Override
	public void close() {
		// 缺失会话无需关闭，空操作
	}


}
