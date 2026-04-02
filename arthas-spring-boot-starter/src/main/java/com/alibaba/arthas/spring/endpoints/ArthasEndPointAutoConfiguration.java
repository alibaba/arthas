package com.alibaba.arthas.spring.endpoints;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Arthas端点自动配置类
 * <p>
 * 该类负责自动配置ArthasEndPoint Bean，将其注册到Spring容器中。
 * 只有在满足以下条件时才会创建端点：
 * 1. spring.arthas.enabled配置项为true或不存在（默认启用）
 * 2. 容器中不存在ArthasEndPoint类型的Bean
 * 3. ArthasEndPoint端点可用（取决于Actuator的配置）
 * </p>
 *
 * @author hengyunabc 2020-06-24
 *
 */
// 当配置项 spring.arthas.enabled 存在且不为false时，或者该配置项不存在时（matchIfMissing = true），该配置类才会生效
@ConditionalOnProperty(name = "spring.arthas.enabled", matchIfMissing = true)
public class ArthasEndPointAutoConfiguration {

	/**
	 * 创建ArthasEndPoint Bean
	 * <p>
	 * 该方法创建一个ArthasEndPoint实例并注册到Spring容器中。
	 * 端点可以通过/actuator/arthas访问，用于查看Arthas的配置和状态信息。
	 * </p>
	 * <p>
	 * 只有在以下条件都满足时才会创建Bean：
	 * 1. 容器中不存在ArthasEndPoint类型的Bean（避免重复创建）
	 * 2. ArthasEndPoint端点在当前环境中可用（由Actuator的配置决定）
	 * </p>
	 *
	 * @return 新创建的ArthasEndPoint实例
	 */
	// 将该方法返回的对象注册为Spring容器中的Bean
	@Bean
	// 当容器中不存在ArthasEndPoint类型的Bean时，才创建这个Bean
	@ConditionalOnMissingBean
	// 当ArthasEndPoint端点可用时才创建Bean（取决于Actuator的配置和暴露策略）
	@ConditionalOnAvailableEndpoint
	public ArthasEndPoint arthasEndPoint() {
		// 创建并返回一个新的ArthasEndPoint实例
		// Spring会自动注入所需的依赖（ArthasAgent和arthasConfigMap）
		return new ArthasEndPoint();
	}
}
