/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import com.taobao.arthas.mcp.server.CommandExecutor;
import com.taobao.arthas.mcp.server.protocol.server.McpInitRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpNotificationHandler;
import com.taobao.arthas.mcp.server.protocol.server.McpRequestHandler;
import com.taobao.arthas.mcp.server.protocol.server.store.InMemoryEventStore;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MCP可流式传输的服务器会话工厂的默认实现类
 *
 * 该工厂负责根据提供的配置创建新的MCP可流式传输服务器会话。
 * 它整合了请求超时设置、初始化请求处理器、各类请求处理器、通知处理器以及命令执行器等组件。
 *
 * @author Yeaury
 */
public class DefaultMcpStreamableServerSessionFactory implements McpStreamableServerSession.Factory {

    /**
     * 请求超时时长
     * 用于控制每个请求的最大执行时间，防止请求无限期阻塞
     */
    private final Duration requestTimeout;

    /**
     * MCP初始化请求处理器
     * 负责处理客户端的初始化请求，建立会话前的握手协商
     */
    private final McpInitRequestHandler mcpInitRequestHandler;

    /**
     * 请求处理器映射表
     * 键为方法名称，值为对应的请求处理器，用于处理各类MCP协议请求
     */
    private final Map<String, McpRequestHandler<?>> requestHandlers;

    /**
     * 通知处理器映射表
     * 键为方法名称，值为对应的通知处理器，用于处理各类MCP协议通知
     */
    private final Map<String, McpNotificationHandler> notificationHandlers;

    /**
     * 命令执行器
     * 负责执行具体的命令逻辑，如Arthas诊断命令等
     */
    private final CommandExecutor commandExecutor;

    /**
     * 构造函数 - 创建MCP可流式传输服务器会话工厂实例
     *
     * @param requestTimeout 请求超时时长，用于控制单个请求的最大执行时间
     * @param mcpInitRequestHandler MCP初始化请求处理器，负责处理客户端的初始化请求
     * @param requestHandlers 请求处理器映射表，包含各种MCP协议方法的处理器
     * @param notificationHandlers 通知处理器映射表，包含各种MCP协议通知的处理器
     * @param commandExecutor 命令执行器，用于执行具体的命令逻辑
     */
    public DefaultMcpStreamableServerSessionFactory(Duration requestTimeout,
                                                    McpInitRequestHandler mcpInitRequestHandler,
                                                    Map<String, McpRequestHandler<?>> requestHandlers,
                                                    Map<String, McpNotificationHandler> notificationHandlers,
                                                    CommandExecutor commandExecutor) {
        // 保存请求超时时长配置
        this.requestTimeout = requestTimeout;
        // 保存初始化请求处理器
        this.mcpInitRequestHandler = mcpInitRequestHandler;
        // 保存请求处理器映射表
        this.requestHandlers = requestHandlers;
        // 保存通知处理器映射表
        this.notificationHandlers = notificationHandlers;
        // 保存命令执行器
        this.commandExecutor = commandExecutor;
    }

    /**
     * 启动一个新的MCP服务器会话
     *
     * 该方法会创建一个新的服务器会话实例，并异步处理初始化请求。
     * 会话使用UUID作为唯一标识符，并继承客户端的能力信息和配置。
     *
     * @param initializeRequest 客户端发送的初始化请求，包含协议版本、客户端能力和客户端信息
     * @return 返回会话初始化结果，包含新创建的会话对象和初始化请求的异步处理结果
     */
    @Override
    public McpStreamableServerSession.McpStreamableServerSessionInit startSession(
            McpSchema.InitializeRequest initializeRequest) {

        // 创建一个新的会话，使用随机UUID作为会话ID
        McpStreamableServerSession session = new McpStreamableServerSession(
                UUID.randomUUID().toString(),  // 生成唯一的会话标识符
                initializeRequest.getCapabilities(),  // 客户端声明的能力（如支持的工具、资源等）
                initializeRequest.getClientInfo(),  // 客户端信息（名称、版本等）
                requestTimeout,  // 请求超时配置
                requestHandlers,  // 请求处理器映射表
                notificationHandlers,  // 通知处理器映射表
                commandExecutor,  // 命令执行器
                new InMemoryEventStore());  // 内存事件存储，用于记录会话事件

        // 异步处理初始化请求
        // 初始化过程包括协议版本协商、服务器能力声明等
        CompletableFuture<McpSchema.InitializeResult> initResult =
                this.mcpInitRequestHandler.handle(initializeRequest);

        // 返回会话初始化结果，包含会话对象和初始化请求的异步处理结果
        return new McpStreamableServerSession.McpStreamableServerSessionInit(session, initResult);
    }
}
