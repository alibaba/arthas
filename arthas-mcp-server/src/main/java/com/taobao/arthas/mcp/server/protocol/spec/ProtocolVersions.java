package com.taobao.arthas.mcp.server.protocol.spec;

/**
 * MCP协议版本常量定义接口
 *
 * <p>该接口定义了MCP（Model Context Protocol）协议支持的所有版本常量。
 * 每个版本对应协议的一个发布日期，版本号采用日期格式（YYYY-MM-DD）。
 *
 * <p>版本历史：
 * <ul>
 *   <li><b>2024-11-05</b> - MCP协议的初始版本，定义了基本的协议规范</li>
 *   <li><b>2025-03-26</b> - 第二个版本，增加了新的功能和改进</li>
 *   <li><b>2025-06-18</b> - 最新版本，包含最新的功能和修复</li>
 * </ul>
 *
 * <p>协议版本的规范文档可在以下地址找到：
 * https://modelcontextprotocol.io/specification/
 *
 * @author Yeaury
 */
public interface ProtocolVersions {

	/**
	 * MCP协议版本 2024-11-05
	 *
	 * <p>这是MCP协议的初始版本，定义了：
	 * <ul>
	 *   <li>基本的JSON-RPC通信模式</li>
	 *   <li>工具（Tools）调用机制</li>
	 *   <li>资源（Resources）读取功能</li>
	 *   <li>提示词（Prompts）管理</li>
	 *   <li>日志和采样功能</li>
	 * </ul>
	 *
	 * <p>规范文档: https://modelcontextprotocol.io/specification/2024-11-05
	 */
	String MCP_2024_11_05 = "2024-11-05";

	/**
	 * MCP协议版本 2025-03-26
	 *
	 * <p>该版本在初始版本的基础上增加了：
	 * <ul>
	 *   <li>增强的能力协商机制</li>
	 *   <li>改进的错误处理</li>
	 *   <li>新的协议功能</li>
	 * </ul>
	 *
	 * <p>规范文档: https://modelcontextprotocol.io/specification/2025-03-26
	 */
	String MCP_2025_03_26 = "2025-03-26";

	/**
	 * MCP协议版本 2025-06-18
	 *
	 * <p>这是当前最新的MCP协议版本，包含：
	 * <ul>
	 *   <li>所有之前版本的功能</li>
	 *   <li>最新的协议改进和修复</li>
	 *   <li>推荐用于新的MCP实现</li>
	 * </ul>
	 *
	 * <p>规范文档: https://modelcontextprotocol.io/specification/2025-06-18
	 */
	String MCP_2025_06_18 = "2025-06-18";

}
