
package com.taobao.arthas.mcp.server.tool.execution;
import java.lang.reflect.Type;

/**
 * A functional interface to convert tool call results to a String that can be sent back
 * to the AI model.
 */
@FunctionalInterface
public interface ToolCallResultConverter {

	/**
	 * Given an Object returned by a tool, convert it to a String compatible with the
	 * given class type.
	 */
	String convert(Object result, Type returnType);

}
