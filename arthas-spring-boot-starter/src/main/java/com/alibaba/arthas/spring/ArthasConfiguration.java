package com.alibaba.arthas.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taobao.arthas.agent.attach.ArthasAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hengyunabc 2020-06-22
 * @author Naah 2021-04-17
 *
 */
@ConditionalOnProperty(name = "spring.arthas.enabled", matchIfMissing = true)
@EnableConfigurationProperties({ ArthasProperties.class })
public class ArthasConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(ArthasConfiguration.class);

	@Autowired
	ConfigurableEnvironment environment;

	@ConfigurationProperties(prefix = "arthas")
	@ConditionalOnMissingBean
	@Bean
	public HashMap<String, String> arthasConfigMap() {
		return new HashMap<String, String>();
	}

	@ConditionalOnMissingBean
	@Bean
	public ArthasAgent arthasAgent(@Autowired Map<String, String> arthasConfigMap,
			@Autowired ArthasProperties arthasProperties) throws Throwable {
        arthasConfigMap = StringUtils.removeDashKey(arthasConfigMap);

        /**
         * @see org.springframework.boot.context.ContextIdApplicationContextInitializer#getApplicationId(ConfigurableEnvironment)
         */
        String appName = environment.getProperty("spring.application.name");
        if (arthasConfigMap.get("appName") == null && appName != null) {
            arthasConfigMap.put("appName", appName);
        }

        Map<String,String> metadata=new HashMap<String, String>();
        String serviceIp = InetAddress.getLocalHost().getHostAddress();
        String servicePort = environment.getProperty("server.port");

        if (serviceIp!=null && !"".equals(serviceIp)) {
            metadata.put("serviceIp",serviceIp);
        }

        if (servicePort!=null && !"".equals(servicePort)) {
            metadata.put("servicePort",servicePort);
        }

        // 给配置全加上前缀
		Map<String, String> mapWithPrefix = new HashMap<String, String>(arthasConfigMap.size());
		for (Entry<String, String> entry : arthasConfigMap.entrySet()) {
			mapWithPrefix.put("arthas." + entry.getKey(), entry.getValue());
		}

        mapWithPrefix.put("arthas.metadata",new ObjectMapper().writeValueAsString(metadata));

		final ArthasAgent arthasAgent = new ArthasAgent(mapWithPrefix, arthasProperties.getHome(),
				arthasProperties.isSlientInit(), null);

		arthasAgent.init();
		logger.info("Arthas agent start success.");
		return arthasAgent;

	}
}
