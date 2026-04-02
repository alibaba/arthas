/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.taobao.arthas.mcp.server.protocol.spec.McpError;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.LoggingLevel;
import com.taobao.arthas.mcp.server.protocol.spec.McpSchema.LoggingMessageNotification;
import com.taobao.arthas.mcp.server.protocol.spec.McpSession;
import com.taobao.arthas.mcp.server.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 表示 MCP 服务器与客户端之间的交互对象
 * 该类提供通信、日志记录和上下文管理的方法
 * 本类仅专注于 MCP 协议通信，不直接处理命令执行
 *
 * <p>
 * McpNettyServerExchange 提供了多种与客户端通信的方法，包括：
 * <ul>
 * <li>发送请求和通知
 * <li>获取客户端能力和信息
 * <li>处理日志通知
 * <li>创建客户端消息
 * <li>管理根目录
 * <li>可流化任务管理
 * </ul>
 * <p>
 * 每个 exchange 对象都与特定的客户端会话关联，为该会话提供上下文和能力
 */
public class McpNettyServerExchange {

	/**
	 * 日志记录器，用于记录该类的运行时信息
	 */
	private static final Logger logger = LoggerFactory.getLogger(McpNettyServerExchange.class);

	/**
	 * 会话 ID，用于唯一标识一个客户端会话
	 */
	private final String sessionId;

	/**
	 * MCP 会话对象，表示与客户端的底层连接会话
	 */
	private final McpSession session;

	/**
	 * 客户端能力描述，包含客户端支持的功能和能力信息
	 */
	private final McpSchema.ClientCapabilities clientCapabilities;

	/**
	 * 客户端实现信息，包含客户端的名称和版本等
	 */
	private final McpSchema.Implementation clientInfo;

	/**
	 * 传输上下文，包含传输层的相关信息和配置
	 */
	private final McpTransportContext transportContext;

	/**
	 * 最小日志级别，用于过滤日志通知
	 * 只有大于等于该级别的日志才会被发送到客户端
	 * 默认为 INFO 级别
	 */
	private volatile LoggingLevel minLoggingLevel = LoggingLevel.INFO;

	/**
	 * 创建消息结果的类型引用
	 * 用于 JSON 反序列化时指定目标类型
	 */
	private static final TypeReference<McpSchema.CreateMessageResult> CREATE_MESSAGE_RESULT_TYPE_REF =
			new TypeReference<McpSchema.CreateMessageResult>() {
	};

	/**
	 * 列出根目录结果的类型引用
	 * 用于 JSON 反序列化时指定目标类型
	 */
	private static final TypeReference<McpSchema.ListRootsResult> LIST_ROOTS_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ListRootsResult>() {
	};

	/**
	 * 获取用户输入结果的类型引用
	 * 用于 JSON 反序列化时指定目标类型
	 */
	private static final TypeReference<McpSchema.ElicitResult> ELICIT_USER_INPUT_RESULT_TYPE_REF =
			new TypeReference<McpSchema.ElicitResult>() {
    };

	/**
	 * 通用对象类型引用
	 * 用于 JSON 反序列化时指定目标类型为 Object
	 */
	public static final TypeReference<Object> OBJECT_TYPE_REF = new TypeReference<Object>() {
	};

	/**
	 * 构造函数，创建一个新的 McpNettyServerExchange 实例
	 *
	 * @param sessionId 会话 ID，用于唯一标识客户端会话
	 * @param session MCP 会话对象，表示与客户端的底层连接
	 * @param clientCapabilities 客户端能力描述
	 * @param clientInfo 客户端实现信息
	 * @param transportContext 传输上下文
	 */
	public McpNettyServerExchange(String sessionId, McpSession session,
									  McpSchema.ClientCapabilities clientCapabilities, McpSchema.Implementation clientInfo,
									  McpTransportContext transportContext) {
		this.sessionId = sessionId;
		this.session = session;
		this.clientCapabilities = clientCapabilities;
		this.clientInfo = clientInfo;
		this.transportContext = transportContext;
	}

	/**
	 * 获取客户端能力描述
	 * @return 客户端能力对象，包含客户端支持的所有功能
	 */
	public McpSchema.ClientCapabilities getClientCapabilities() {
		return this.clientCapabilities;
	}

	/**
	 * 获取客户端实现信息
	 * @return 客户端实现信息对象，包含名称和版本
	 */
	public McpSchema.Implementation getClientInfo() {
		return this.clientInfo;
	}

	/**
	 * 获取与此 exchange 关联的 MCP 服务器会话
	 * @return MCP 服务器会话对象
	 */
	public McpSession getSession() {
		return this.session;
	}

	/**
	 * 获取与此 exchange 关联的传输上下文
	 * @return 传输上下文对象
	 */
	public McpTransportContext getTransportContext() {
		return this.transportContext;
	}

	/**
	 * 使用客户端采样能力创建新消息
	 * MCP 提供了一种标准化的方式，让服务器通过客户端请求 LLM 采样（"补全"或"生成"）
	 * 这种流程允许客户端保持对模型访问、选择和权限的控制，同时使服务器能够利用 AI 功能，而无需服务器 API 密钥
	 * 服务器可以请求基于文本或图像的交互，并可以选择在其提示中包含来自 MCP 服务器的上下文
	 *
	 * @param createMessageRequest 创建消息的请求对象
	 * @return CompletableFuture，在消息创建完成时完成
	 */
	public CompletableFuture<McpSchema.CreateMessageResult> createMessage(
			McpSchema.CreateMessageRequest createMessageRequest) {
		// 检查客户端是否已初始化
		if (this.clientCapabilities == null) {
			logger.error("Client not initialized, cannot create message");
			CompletableFuture<McpSchema.CreateMessageResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Client must be initialized first. Please call initialize method!"));
			return future;
		}
		// 检查客户端是否配置了采样能力
		if (this.clientCapabilities.getSampling() == null) {
			logger.error("Client not configured with sampling capability, cannot create message");
			CompletableFuture<McpSchema.CreateMessageResult> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("Client must be configured with sampling capability"));
			return future;
		}

		// 发送创建消息请求
		logger.debug("Creating client message, session ID: {}", this.sessionId);
		return this.session
			.sendRequest(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, createMessageRequest, CREATE_MESSAGE_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to create message, session ID: {}, error: {}", this.sessionId, error.getMessage());
				}
				else {
					logger.debug("Message created successfully, session ID: {}", this.sessionId);
				}
			});
	}

	/**
	 * 获取客户端提供的所有根目录列表
	 * @return CompletableFuture，包含根目录列表结果
	 */
	public CompletableFuture<McpSchema.ListRootsResult> listRoots() {
		// 调用带游标参数的方法，传入 null 表示不分页
		return this.listRoots(null);
	}

	/**
	 * 获取客户端提供的根目录列表，支持分页
	 *
	 * @param cursor 可选的分页游标，来自上一次列表请求
	 * @return CompletableFuture，包含根目录列表结果
	 */
	public CompletableFuture<McpSchema.ListRootsResult> listRoots(String cursor) {
		logger.debug("Requesting root list, session ID: {}, cursor: {}", this.sessionId, cursor);
		// 发送列出根目录的请求
		return this.session
			.sendRequest(McpSchema.METHOD_ROOTS_LIST, new McpSchema.PaginatedRequest(cursor),
					LIST_ROOTS_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to get root list, session ID: {}, error: {}", this.sessionId, error.getMessage());
				}
				else {
					logger.debug("Root list retrieved successfully, session ID: {}", this.sessionId);
				}
			});
	}

	/**
	 * 向客户端发送日志通知
	 *
	 * @param loggingMessageNotification 日志消息通知对象
	 * @return CompletableFuture，在通知发送完成时完成
	 */
	public CompletableFuture<Void> loggingNotification(LoggingMessageNotification loggingMessageNotification) {
		// 验证日志消息通知不能为空
		if (loggingMessageNotification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("log messages cannot be empty"));
			return future;
		}

		// 检查日志级别是否允许发送此通知
		if (this.isNotificationForLevelAllowed(loggingMessageNotification.getLevel())) {
			// 发送日志通知
			return this.session.sendNotification(McpSchema.METHOD_NOTIFICATION_MESSAGE, loggingMessageNotification)
				.whenComplete((result, error) -> {
					if (error != null) {
						logger.error("Failed to send logging notification, level: {}, session ID: {}, error: {}", loggingMessageNotification.getLevel(),
								this.sessionId, error.getMessage());
					}
				});
		}
		// 如果日志级别不够，返回已完成的 Future
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * 向客户端发送 ping 请求，用于检测连接是否活跃
	 *
	 * @return CompletableFuture，在 ping 完成时完成
	 */
	public CompletableFuture<Object> ping() {
		return this.session.sendRequest(McpSchema.METHOD_PING, null, OBJECT_TYPE_REF);
	}

	/**
	 * 创建一个用户输入请求
	 * 用于向客户端请求获取用户输入或确认
	 *
	 * @param request 获取用户输入的请求对象
	 * @return CompletableFuture，在获取到用户输入时完成
	 */
	public CompletableFuture<McpSchema.ElicitResult> createElicitation(McpSchema.ElicitRequest request) {
		// 验证请求对象不能为空
        if (request == null) {
            CompletableFuture<McpSchema.ElicitResult> future = new CompletableFuture<>();
            future.completeExceptionally(new McpError("elicit request cannot be null"));
            return future;
        }
        // 检查客户端是否已初始化
        if (this.clientCapabilities == null) {
            CompletableFuture<McpSchema.ElicitResult> future = new CompletableFuture<>();
            future.completeExceptionally(new McpError("Client must be initialized. Call the initialize method first!"));
            return future;
        }
        // 检查客户端是否配置了获取用户输入的能力
        if (this.clientCapabilities.getElicitation() == null) {
            CompletableFuture<McpSchema.ElicitResult> future = new CompletableFuture<>();
            future.completeExceptionally(new McpError("Client must be configured with elicitation capabilities"));
            return future;
        }
		// 发送获取用户输入的请求
		return this.session
			.sendRequest(McpSchema.METHOD_ELICITATION_CREATE, request, ELICIT_USER_INPUT_RESULT_TYPE_REF)
			.whenComplete((result, error) -> {
				if (error != null) {
					logger.error("Failed to elicit user input, session ID: {}, error: {}", this.sessionId, error.getMessage());
				} else {
					logger.debug("User input elicitation completed, session ID: {}", this.sessionId);
				}
			});
	}

	/**
	 * 设置最小日志级别
	 * 只有大于等于此级别的日志才会被发送到客户端
	 *
	 * @param minLoggingLevel 最小日志级别
	 */
	public void setMinLoggingLevel(LoggingLevel minLoggingLevel) {
		Assert.notNull(minLoggingLevel, "the minimum log level cannot be empty");
		logger.debug("Setting minimum logging level: {}, session ID: {}", minLoggingLevel, this.sessionId);
		this.minLoggingLevel = minLoggingLevel;
	}

	/**
	 * 检查是否允许发送指定级别的日志通知
	 *
	 * @param loggingLevel 要检查的日志级别
	 * @return 如果允许发送返回 true，否则返回 false
	 */
	private boolean isNotificationForLevelAllowed(LoggingLevel loggingLevel) {
		// 比较日志级别，只有大于等于最小级别的才允许发送
		return loggingLevel.level() >= this.minLoggingLevel.level();
	}

	/**
	 * 向客户端发送进度通知
	 * 用于报告长时间运行操作的进度
	 *
	 * @param progressNotification 进度通知对象
	 * @return CompletableFuture，在通知发送完成时完成
	 */
	public CompletableFuture<Void> progressNotification(McpSchema.ProgressNotification progressNotification) {
		// 验证进度通知不能为空
		if (progressNotification == null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			future.completeExceptionally(new McpError("progress notifications cannot be empty"));
			return future;
		}

		// 发送进度通知
		return this.session
				.sendNotification(McpSchema.METHOD_NOTIFICATION_PROGRESS, progressNotification)
				.whenComplete((result, error) -> {
					if (error != null) {
						logger.error("Failed to send progress notification, session ID: {}, error: {}", this.sessionId, error.getMessage());
					} else {
						logger.debug("Progress notification sent successfully, session ID: {}", this.sessionId);
					}
				});
	}
}
