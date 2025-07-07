package com.taobao.arthas.mcp.server.tool.definition;

import com.taobao.arthas.mcp.server.tool.annotation.Tool;
import com.taobao.arthas.mcp.server.tool.util.JsonSchemaGenerator;
import com.taobao.arthas.mcp.server.util.Assert;

import java.lang.reflect.Method;

public class ToolDefinitions {

	public static ToolDefinition.Builder builder(Method method) {
		Assert.notNull(method, "method cannot be null");
		return ToolDefinition.builder()
			.name(getToolName(method))
			.description(getToolDescription(method))
			.inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
			.streamable(isStreamable(method));
	}

	public static ToolDefinition from(Method method) {
		return builder(method).build();
	}

	public static String getToolName(Method method) {
		Assert.notNull(method, "method cannot be null");
		Tool tool = method.getAnnotation(Tool.class);
		if (tool == null) {
			return method.getName();
		}
		return tool.name() != null ? tool.name() : method.getName();
	}

	public static String getToolDescription(Method method) {
		Assert.notNull(method, "method cannot be null");
		Tool tool = method.getAnnotation(Tool.class);
		if (tool == null) {
			return method.getName();
		}
		return tool.description() != null ? tool.description() : method.getName();
	}

	public static boolean isStreamable(Method method) {
		Assert.notNull(method, "method cannot be null");
		Tool tool = method.getAnnotation(Tool.class);
		if (tool == null) {
			return false;
		}
		return tool.streamable();
	}

}