/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.session.ArthasCommandContext;
import com.taobao.arthas.mcp.server.session.ArthasCommandSessionManager;
import com.taobao.arthas.mcp.server.util.McpAuthExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * 默认的MCP无状态服务器处理器
 * <p>
 * 该类实现了McpStatelessServerHandler接口，负责处理MCP协议的请求和通知。
 * 它是无状态的，意味着每个请求都会创建一个临时会话来处理，处理完成后立即清理。
 * </p>
 * <p>
 * 主要功能：
 * 1. 处理MCP请求：为每个请求创建临时会话，执行命令后清理会话
 * 2. 处理MCP通知：委托给相应的通知处理器
 * 3. 会话管理：包括会话的创建、认证信息应用和会话关闭
 * </p>
 */
class DefaultMcpStatelessServerHandler implements McpStatelessServerHandler {

	/**
	 * 日志记录器，用于记录处理过程中的关键信息和错误
	 */
	private static final Logger logger = LoggerFactory.getLogger(DefaultMcpStatelessServerHandler.class);

	/**
	 * 请求处理器映射表
	 * key: 请求方法名称（如"tools/list"等）
	 * value: 对应的请求处理器
	 */
	Map<String, McpStatelessRequestHandler<?>> requestHandlers;

	/**
	 * 通知处理器映射表
	 * key: 通知方法名称
	 * value: 对应的通知处理器
	 */
	Map<String, McpStatelessNotificationHandler> notificationHandlers;

	/**
	 * 命令执行器，负责执行Arthas命令
	 */
	private final CommandExecutor commandExecutor;

	/**
	 * Arthas命令会话管理器，负责管理命令执行的生命周期
	 */
	private final ArthasCommandSessionManager commandSessionManager;

	/**
	 * 构造函数
	 *
	 * @param requestHandlers 请求处理器映射表，用于处理不同类型的MCP请求
	 * @param notificationHandlers 通知处理器映射表，用于处理不同类型的MCP通知
	 * @param commandExecutor 命令执行器，用于执行Arthas命令
	 */
	public DefaultMcpStatelessServerHandler(Map<String, McpStatelessRequestHandler<?>> requestHandlers,
	                                            Map<String, McpStatelessNotificationHandler> notificationHandlers,
	                                            CommandExecutor commandExecutor) {
		// 保存请求处理器映射表
		this.requestHandlers = requestHandlers;
		// 保存通知处理器映射表
		this.notificationHandlers = notificationHandlers;
		// 保存命令执行器
		this.commandExecutor = commandExecutor;
		// 创建命令会话管理器，用于管理命令执行会话
		this.commandSessionManager = new ArthasCommandSessionManager(commandExecutor);
	}

	/**
	 * 处理MCP请求
	 * <p>
	 * 该方法为每个请求创建一个临时会话，执行命令后立即清理会话。
	 * 处理流程：
	 * 1. 创建临时会话ID和会话绑定
	 * 2. 从传输上下文中提取认证信息并应用到会话
	 * 3. 查找对应的请求处理器
	 * 4. 执行请求处理
	 * 5. 清理会话
	 * 6. 返回处理结果或错误信息
	 * </p>
	 *
	 * @param ctx MCP传输上下文，包含认证信息等元数据
	 * @param req MCP JSON-RPC请求对象，包含请求方法和参数
	 * @return CompletableFuture包含JSON-RPC响应对象
	 */
	@Override
	public CompletableFuture<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext ctx, McpSchema.JSONRPCRequest req) {
		// 为当前请求创建一个临时会话ID
		// 使用UUID确保会话ID的唯一性
		String tempSessionId = UUID.randomUUID().toString();
		// 创建命令会话绑定，用于管理临时会话的生命周期
		ArthasCommandSessionManager.CommandSessionBinding binding = commandSessionManager.createCommandSession(tempSessionId);
		// 创建Arthas命令上下文，包含命令执行器和会话绑定
		ArthasCommandContext commandContext = new ArthasCommandContext(commandExecutor, binding);

		// 从传输上下文中提取认证主题并应用到会话
		// MCP_AUTH_SUBJECT_KEY是预定义的键，用于存储认证主题
		Object authSubject = ctx.get(McpAuthExtractor.MCP_AUTH_SUBJECT_KEY);
		if (authSubject != null) {
			// 将认证主题设置到会话中，用于后续的权限验证
			commandExecutor.setSessionAuth(binding.getArthasSessionId(), authSubject);
			logger.debug("Applied auth subject to stateless session: {}", binding.getArthasSessionId());
		}

		// 从传输上下文中提取用户ID并应用到会话
		// MCP_USER_ID_KEY是预定义的键，用于存储用户ID
		String userId = (String) ctx.get(McpAuthExtractor.MCP_USER_ID_KEY);
		if (userId != null) {
			// 将用户ID设置到会话中，用于审计和追踪
			commandExecutor.setSessionUserId(binding.getArthasSessionId(), userId);
			logger.debug("Applied userId to stateless session: {}", binding.getArthasSessionId());
		}

		// 根据请求方法名称查找对应的处理器
		McpStatelessRequestHandler<?> handler = requestHandlers.get(req.getMethod());
		if (handler == null) {
			// 如果找不到对应的处理器，清理会话并返回错误
			closeSession(binding);
			// 创建一个已完成的异常Future
			CompletableFuture<McpSchema.JSONRPCResponse> f = new CompletableFuture<>();
			// 设置异常，表示缺少对应的请求处理器
			f.completeExceptionally(new McpError("Missing handler for request type: " + req.getMethod()));
			return f;
		}

		// 找到处理器后，执行请求处理
		try {
			// 调用处理器的handle方法处理请求
			// 使用@SuppressWarnings("unchecked")来抑制类型转换警告
			// 因为requestHandlers的值类型是McpStatelessRequestHandler<?>，这里需要转换为具体类型
			@SuppressWarnings("unchecked")
			CompletableFuture<Object> result = (CompletableFuture<Object>) handler
					.handle(ctx, commandContext, req.getParams());

			// 处理结果，在完成后清理会话并构建响应
			return result.handle((r, ex) -> {
				// 请求执行完成后，清理临时会话
				// 这确保了无论成功或失败，会话都会被正确清理
				closeSession(binding);

				// 检查是否有异常发生
				if (ex != null) {
					// 如果有异常，提取根本原因
					// CompletionException包装了实际的异常，需要解包
					Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
					// 构建错误响应，包含错误码和错误消息
					return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, req.getId(), null,
							new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR, cause.getMessage(), null));
				}
				// 没有异常，构建成功响应
				// 包含JSON-RPC版本、请求ID、结果对象
				return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, req.getId(), r, null);
			});
		} catch (Throwable t) {
			// 处理过程中发生异常，清理会话
			closeSession(binding);

			// 创建异常Future并返回
			CompletableFuture<McpSchema.JSONRPCResponse> f = new CompletableFuture<>();
			f.completeExceptionally(t);
			return f;
		}
	}

	/**
	 * 关闭会话
	 * <p>
	 * 该方法用于关闭临时会话，释放相关资源。
	 * 会关闭Arthas会话并捕获可能的异常。
	 * </p>
	 *
	 * @param binding 命令会话绑定对象，包含会话ID等信息
	 */
	private void closeSession(ArthasCommandSessionManager.CommandSessionBinding binding) {
		try {
			// 关闭Arthas会话，释放相关资源
			// 这会清理与该会话相关的所有状态和资源
			commandExecutor.closeSession(binding.getArthasSessionId());
		} catch (Exception e) {
			// 记录关闭会话时的警告信息
			// 即使关闭失败也不应该影响主流程，所以只记录警告
			logger.warn("Failed to close temporary session: {}", binding.getArthasSessionId(), e);
		}
	}

	/**
	 * 处理MCP通知
	 * <p>
	 * 通知与请求不同，不需要返回响应。该方法会：
	 * 1. 查找对应的通知处理器
	 * 2. 执行通知处理
	 * 3. 返回完成的Future
	 * </p>
	 *
	 * @param ctx MCP传输上下文
	 * @param note MCP JSON-RPC通知对象
	 * @return CompletableFuture表示通知处理的完成状态
	 */
	@Override
	public CompletableFuture<Void> handleNotification(McpTransportContext ctx,
													  McpSchema.JSONRPCNotification note) {
		// 根据通知方法名称查找对应的通知处理器
		McpStatelessNotificationHandler handler = notificationHandlers.get(note.getMethod());
		if (handler == null) {
			// 如果找不到处理器，记录警告并返回已完成的Future
			logger.warn("Missing handler for notification: {}", note.getMethod());
			return CompletableFuture.completedFuture(null);
		}

		// 找到处理器后，执行通知处理
		try {
			// 调用处理器的handle方法处理通知
			// 通知处理不需要返回结果，只需要完成即可
			return handler.handle(ctx, note.getParams());
		} catch (Throwable t) {
			// 处理过程中发生异常，返回异常Future
			CompletableFuture<Void> f = new CompletableFuture<>();
			f.completeExceptionally(t);
			return f;
		}
	}

}
