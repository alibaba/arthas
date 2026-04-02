/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

/**
 * MCP传输上下文提取器接口
 *
 * 该接口定义了从服务器请求中提取传输上下文的规范。
 * 通过实现此接口，可以在请求处理过程中检查和提取HTTP传输层的元数据，
 * 例如请求头、客户端信息等。这允许在业务逻辑中访问传输层的相关信息。
 *
 * @param <T> 服务器请求的类型参数，可以是任何类型的请求对象
 */
@FunctionalInterface
public interface McpTransportContextExtractor<T> {

    /**
     * 从服务器请求中提取传输上下文信息
     *
     * 此方法接收原始的服务器请求对象，并从中提取相关的传输层元数据，
     * 然后将这些信息填充到提供的上下文对象中。提取的信息可以包括：
     * - 客户端IP地址
     * - 请求头信息
     * - 认证信息
     * - 协议版本
     * - 其他传输层相关的元数据
     *
     * @param serverRequest 要提取上下文的服务器请求对象
     * @param context 要填充的基础上下文对象，可能已经包含一些预设信息
     * @return 更新后的上下文对象，包含从请求中提取的所有传输层信息
     */
    McpTransportContext extract(T serverRequest, McpTransportContext context);

}
