package com.taobao.arthas.mcp.server.tool;

import java.util.Collections;
import java.util.Map;

public final class ToolContext {

	private final Map<String, Object> context;

	public ToolContext(Map<String, Object> context) {
		this.context = Collections.unmodifiableMap(context);
	}

	public Map<String, Object> getContext() {
		return this.context;
	}

}
