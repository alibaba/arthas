/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

/**
 * MCP传输上下文接口
 *
 * 该接口定义了MCP传输层上下文的规范，用于在请求处理过程中传递和存储传输层相关的元数据。
 * 传输上下文是无状态的，可以在请求之间传递信息，但不依赖于特定的会话状态。
 */
public interface McpTransportContext {

	/**
	 * 传输上下文在属性存储中使用的键名常量
	 *
	 * 该常量用于在各种存储机制中标识MCP传输上下文对象
	 */
	String KEY = "MCP_TRANSPORT_CONTEXT";

	/**
	 * 空的传输上下文实例
	 *
	 * 当不需要传递任何传输层元数据时，可以使用此空实例
	 */
	McpTransportContext EMPTY = new DefaultMcpTransportContext();

	/**
	 * 从上下文中获取指定键的值
	 *
	 * @param key 属性键名
	 * @return 对应的属性值，如果不存在则返回null
	 */
	Object get(String key);

	/**
	 * 向上下文中添加或更新指定键的值
	 *
	 * @param key 属性键名
	 * @param value 属性值
	 */
	void put(String key, Object value);

	/**
	 * 创建当前传输上下文的副本
	 *
	 * 此方法用于创建上下文的独立副本，修改副本不会影响原始上下文。
	 * 这在需要在不同处理阶段传递独立上下文时非常有用。
	 *
	 * @return 当前传输上下文的副本
	 */
	McpTransportContext copy();

}
