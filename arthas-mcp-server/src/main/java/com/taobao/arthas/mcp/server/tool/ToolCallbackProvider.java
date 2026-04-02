package com.taobao.arthas.mcp.server.tool;

/**
 * 工具回调提供者接口
 * 用于提供MCP服务器中所有可用的工具回调实例
 * 实现此接口的类负责创建和管理工具的生命周期
 */
public interface ToolCallbackProvider {

	/**
	 * 获取所有可用的工具回调实例
	 * 返回MCP服务器支持的完整工具列表
	 *
	 * @return 工具回调数组，包含所有已注册的工具实例
	 */
	ToolCallback[] getToolCallbacks();

}
