package com.taobao.arthas.mcp.server.tool;

import java.util.Collections;
import java.util.Map;

/**
 * 工具上下文类
 *
 * 该类用于封装工具执行时的上下文信息，提供不可变的环境数据访问。
 * 上下文信息以键值对的形式存储，用于在工具执行过程中传递必要的参数和配置信息。
 *
 * @author arthas
 */
public final class ToolContext {

	/**
	 * 上下文数据映射
	 * 存储工具执行所需的上下文信息，键为参数名，值为参数对象
	 * 使用不可变Map确保上下文数据在创建后无法被修改
	 */
	private final Map<String, Object> context;

	/**
	 * 构造函数
	 *
	 * 创建工具上下文对象，并将传入的上下文Map转换为不可变Map，
	 * 以防止上下文数据在运行时被意外修改，确保线程安全和数据一致性。
	 *
	 * @param context 上下文数据映射，包含工具执行所需的所有参数和配置信息
	 */
	public ToolContext(Map<String, Object> context) {
		// 使用Collections.unmodifiableMap包装原始Map，创建不可修改的视图
		// 这样可以确保外部引用无法修改内部的上下文数据
		this.context = Collections.unmodifiableMap(context);
	}

	/**
	 * 获取上下文数据
	 *
	 * 返回不可变的上下文Map对象，调用者可以使用该Map访问工具执行时所需的参数和配置信息，
	 * 但无法修改其中的内容。
	 *
	 * @return 不可修改的上下文数据映射，包含工具执行所需的所有信息
	 */
	public Map<String, Object> getContext() {
		return this.context;
	}

}
