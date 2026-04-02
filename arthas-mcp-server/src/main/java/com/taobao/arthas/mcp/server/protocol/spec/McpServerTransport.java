/*
 * Copyright 2024-2024 the original author or authors.
 */

package com.taobao.arthas.mcp.server.protocol.spec;

import io.netty.channel.Channel;

/**
 * MCP服务端传输接口
 *
 * <p>该接口扩展了基本的McpTransport接口，为服务端传输提供了访问底层Netty Channel的能力。
 * 通过这个接口，服务端可以直接操作Netty通道，实现更底层的网络通信控制。
 *
 * <p>主要用途：
 * <ul>
 *   <li>访问和控制底层Netty通道</li>
 *   <li>实现自定义的通道管理逻辑</li>
 *   <li>支持服务端特有的传输功能</li>
 * </ul>
 *
 * @author Yeaury
 */
public interface McpServerTransport extends McpTransport {

	/**
	 * 获取底层的Netty通道对象
	 *
	 * <p>通过返回的Channel对象，调用者可以：
	 * <ul>
	 *   <li>获取通道状态信息</li>
	 *   <li>写入数据到通道</li>
	 *   <li>关闭通道</li>
	 *   <li>添加/移除处理器</li>
	 * </ul>
	 *
	 * @return 底层Netty Channel对象
	 */
	Channel getChannel();
}
