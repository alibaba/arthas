package com.alibaba.arthas.spring.endpoints;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import com.taobao.arthas.agent.attach.ArthasAgent;

/**
 * Arthas监控端点类
 * <p>
 * 该类作为一个Spring Boot Actuator端点，用于暴露Arthas的运行状态和配置信息。
 * 通过访问/actuator/arthas端点，可以查看Arthas的配置和运行状态。
 * </p>
 *
 * @author hengyunabc 2020-06-24
 *
 */
// 声明这是一个Actuator端点，端点ID为"arthas"，访问路径为/actuator/arthas
@Endpoint(id = "arthas")
public class ArthasEndPoint {

	// Arthas代理实例，用于管理和控制Arthas的运行状态
	// required = false表示该Bean是可选的，如果容器中不存在也不报错
	@Autowired(required = false)
	private ArthasAgent arthasAgent;

	// Arthas配置映射表，存储所有的Arthas配置项
	// required = false表示该Bean是可选的
	@Autowired(required = false)
	private HashMap<String, String> arthasConfigMap;

	/**
	 * 端点读操作方法
	 * <p>
	 * 当通过HTTP GET请求访问/actuator/arthas端点时，该方法会被调用。
	 * 返回一个包含Arthas配置信息和错误信息的Map对象。
	 * </p>
	 * <p>
	 * 返回的信息包括：
	 * - arthasConfigMap: Arthas的所有配置项
	 * - errorMessage: 如果Arthas初始化或运行过程中出现错误，该字段包含错误信息
	 * </p>
	 *
	 * @return 包含Arthas状态信息的Map对象
	 */
	// 声明这是一个读操作，响应HTTP GET请求
	@ReadOperation
	public Map<String, Object> invoke() {
		// 创建结果Map，用于存储要返回的信息
		Map<String, Object> result = new HashMap<String, Object>();

		// 如果arthasConfigMap不为空，则将其添加到结果中
		// 这样用户可以通过端点查看当前Arthas的所有配置
		if (arthasConfigMap != null) {
			result.put("arthasConfigMap", arthasConfigMap);
		}

		// 从ArthasAgent获取错误信息（如果有）
		// 如果Arthas初始化或运行过程中出现了错误，这里可以获取到错误消息
		String errorMessage = arthasAgent.getErrorMessage();

		// 如果存在错误信息，则将其添加到结果中
		// 这样用户可以通过端点了解Arthas的运行状态
		if (errorMessage != null) {
			result.put("errorMessage", errorMessage);
		}

		// 返回包含所有状态信息的结果Map
		return result;
	}

}
