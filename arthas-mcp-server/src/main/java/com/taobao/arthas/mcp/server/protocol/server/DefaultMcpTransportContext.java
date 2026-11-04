/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation for {@link McpTransportContext} which uses a Thread-safe map.
 * Objects of this kind are mutable.
 */
public class DefaultMcpTransportContext implements McpTransportContext {

	private final Map<String, Object> storage;

	/**
	 * Create an empty instance.
	 */
	public DefaultMcpTransportContext() {
		this.storage = new ConcurrentHashMap<>();
	}

	DefaultMcpTransportContext(Map<String, Object> storage) {
		this.storage = storage;
	}

	@Override
	public Object get(String key) {
		return this.storage.get(key);
	}

	@Override
	public void put(String key, Object value) {
		if (value != null) {
			this.storage.put(key, value);
		} else {
			this.storage.remove(key);
		}
	}

	/**
	 * Allows copying the contents.
	 * @return new instance with the copy of the underlying map
	 */
	public McpTransportContext copy() {
		return new DefaultMcpTransportContext(new ConcurrentHashMap<>(this.storage));
	}

}
