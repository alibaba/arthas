/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP传输上下文的默认实现类
 *
 * 该类使用线程安全的ConcurrentHashMap作为存储后端，提供键值对形式的上下文数据存储。
 * 该对象是可变的，允许在运行时动态添加、修改和删除上下文数据。
 * 主要用于在MCP协议通信过程中存储和传递会话级别的数据。
 *
 * @see McpTransportContext
 */
public class DefaultMcpTransportContext implements McpTransportContext {

	/**
	 * 内部存储映射表
	 * 使用ConcurrentHashMap保证线程安全，支持并发读写操作
	 * 键为字符串类型，值为对象类型，可以存储任意类型的数据
	 */
	private final Map<String, Object> storage;

	/**
	 * 默认构造函数 - 创建一个空的传输上下文实例
	 *
	 * 该构造函数会初始化一个空的ConcurrentHashMap作为存储后端。
	 * 新创建的实例不包含任何数据，可以通过put方法添加数据。
	 */
	public DefaultMcpTransportContext() {
		this.storage = new ConcurrentHashMap<>();
	}

	/**
	 * 包型构造函数 - 使用指定的存储映射表创建传输上下文实例
	 *
	 * 该构造函数允许使用现有的Map作为存储后端，可以用于创建上下文的副本或共享存储。
	 * 注意：如果传入的Map被外部修改，可能会影响该实例的行为。
	 *
	 * @param storage 用作存储后端的映射表，不能为null
	 */
	DefaultMcpTransportContext(Map<String, Object> storage) {
		this.storage = storage;
	}

	/**
	 * 根据键获取上下文中的值
	 *
	 * 该方法从存储中检索与指定键关联的值。
	 * 如果键不存在，则返回null。
	 *
	 * @param key 要查找的键，不能为null
	 * @return 与键关联的值，如果键不存在则返回null
	 */
	@Override
	public Object get(String key) {
		return this.storage.get(key);
	}

	/**
	 * 向上下文中存储键值对
	 *
	 * 该方法将指定的键值对存储到上下文中。
	 * 如果键已存在，则覆盖其旧值。
	 * 如果值为null，则从存储中删除该键（而非存储null值）。
	 *
	 * @param key 要存储的键，不能为null
	 * @param value 要存储的值，如果为null则删除该键
	 */
	@Override
	public void put(String key, Object value) {
		// 如果值不为null，则存储键值对
		if (value != null) {
			this.storage.put(key, value);
		} else {
			// 如果值为null，则从存储中删除该键
			this.storage.remove(key);
		}
	}

	/**
	 * 创建当前传输上下文的副本
	 *
	 * 该方法创建一个新的传输上下文实例，包含当前上下文中所有数据的副本。
	 * 新实例与原实例相互独立，修改其中一个不会影响另一个。
	 * 底层Map的复制是浅拷贝，但新Map本身是独立的实例。
	 *
	 * @return 包含当前上下文数据副本的新传输上下文实例
	 */
	public McpTransportContext copy() {
		// 创建新的ConcurrentHashMap，包含当前存储的所有数据
		return new DefaultMcpTransportContext(new ConcurrentHashMap<>(this.storage));
	}

}
