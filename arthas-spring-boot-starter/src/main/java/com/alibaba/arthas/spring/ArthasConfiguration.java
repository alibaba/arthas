package com.alibaba.arthas.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

import com.taobao.arthas.agent.attach.ArthasAgent;

/**
 * Arthas Spring Boot 自动配置类
 * <p>
 * 该类负责在Spring Boot应用中自动配置和启动Arthas诊断代理。
 * 通过读取配置文件中的arthas相关配置，初始化并启动Arthas Agent。
 * </p>
 *
 * @author hengyunabc 2020-06-22
 *
 */
// 当配置项 spring.arthas.enabled 存在且不为false时，或者该配置项不存在时（matchIfMissing = true），该配置类才会生效
@ConditionalOnProperty(name = "spring.arthas.enabled", matchIfMissing = true)
// 启用 ArthasProperties 配置属性类，使其能够自动绑定配置文件中的属性
@EnableConfigurationProperties({ ArthasProperties.class })
public class ArthasConfiguration {
	// 日志记录器，用于记录配置过程中的关键信息
	private static final Logger logger = LoggerFactory.getLogger(ArthasConfiguration.class);

	// 自动注入Spring的可配置环境，用于读取配置信息
	@Autowired
	ConfigurableEnvironment environment;

	/**
	 * 创建Arthas配置映射Bean
	 * <p>
	 * 该方法的作用包括：
	 * 1. 提取所有以 arthas.* 开头的配置项，再统一转换为Arthas配置
	 * 2. 避免某些配置在新版本里支持，但在ArthasProperties里没有配置的情况。
	 * </p>
	 * <p>
	 * 通过使用HashMap存储配置，可以灵活地支持任意配置项，
	 * 即使在ArthasProperties类中没有定义相应的属性字段。
	 * </p>
	 *
	 * @return 包含所有arthas配置项的HashMap
	 */
	// 将配置文件中前缀为"arthas"的属性自动绑定到这个HashMap中
	@ConfigurationProperties(prefix = "arthas")
	// 当容器中不存在名为"arthasConfigMap"的Bean时，才创建这个Bean
	@ConditionalOnMissingBean(name="arthasConfigMap")
	// 将该方法返回的对象注册为Spring容器中的Bean
	@Bean
	public HashMap<String, String> arthasConfigMap() {
		// 返回一个新的HashMap实例，用于存储arthas的配置项
		return new HashMap<String, String>();
	}

	/**
	 * 创建并初始化ArthasAgent Bean
	 * <p>
	 * 该方法负责创建ArthasAgent实例并完成初始化工作。
	 * 主要步骤包括：
	 * 1. 处理配置项中的连字符键名
	 * 2. 补全默认配置值
	 * 3. 设置应用名称
	 * 4. 为所有配置项添加"arthas."前缀
	 * 5. 初始化并启动Arthas Agent
	 * </p>
	 *
	 * @param arthasConfigMap arthas配置映射表，包含所有arthas相关的配置项
	 * @param arthasProperties arthas属性配置对象，包含类型安全的配置属性
	 * @return 初始化完成的ArthasAgent实例
	 * @throws Throwable 初始化过程中可能抛出的异常
	 */
	// 当容器中不存在ArthasAgent类型的Bean时，才创建这个Bean
	@ConditionalOnMissingBean
	@Bean
	public ArthasAgent arthasAgent(@Autowired @Qualifier("arthasConfigMap") Map<String, String> arthasConfigMap,
			@Autowired ArthasProperties arthasProperties) throws Throwable {
		// 移除配置Map中所有包含连字符的键名，统一转换为驼峰命名
        arthasConfigMap = StringUtils.removeDashKey(arthasConfigMap);

        // 更新配置Map，为未配置的属性设置默认值（例如disabledCommands默认为"stop"）
        ArthasProperties.updateArthasConfigMapDefaultValue(arthasConfigMap);

        /**
         * 获取Spring应用的名称
         * @see org.springframework.boot.context.ContextIdApplicationContextInitializer#getApplicationId(ConfigurableEnvironment)
         */
        // 从Spring环境配置中获取应用名称
        String appName = environment.getProperty("spring.application.name");

        // 如果配置Map中没有设置appName，且Spring配置中有应用名称，则使用Spring的应用名称
        if (arthasConfigMap.get("appName") == null && appName != null) {
            arthasConfigMap.put("appName", appName);
        }

		// 给配置全加上前缀"arthas."，因为ArthasAgent期望接收带前缀的完整配置键名
		Map<String, String> mapWithPrefix = new HashMap<String, String>(arthasConfigMap.size());

		// 遍历原始配置Map，为每个键名添加"arthas."前缀
		for (Entry<String, String> entry : arthasConfigMap.entrySet()) {
			// 将键名加上"arthas."前缀后存入新的Map中
			mapWithPrefix.put("arthas." + entry.getKey(), entry.getValue());
		}

		// 创建ArthasAgent实例，传入配置Map、Arthas安装目录路径、是否静默初始化标志和类加载器
		// slientInit: 当为true时，初始化失败不会抛出异常
		final ArthasAgent arthasAgent = new ArthasAgent(mapWithPrefix, arthasProperties.getHome(),
				arthasProperties.isSlientInit(), null);

		// 初始化Arthas Agent，启动诊断服务
		arthasAgent.init();

		// 记录Arthas Agent启动成功的日志
		logger.info("Arthas agent start success.");

		// 返回初始化完成的ArthasAgent实例
		return arthasAgent;

	}
}
