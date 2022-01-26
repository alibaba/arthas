package com.alibaba.arthas.tunnel.server.app.configuration;

import com.alibaba.arthas.tunnel.server.cluster.RedisTunnelClusterStore;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author caizhaoke
 * @Date 2022-01-26
 * @Description
 */
@Configuration
@AutoConfigureAfter(EmbeddedRedisConfiguration.class)
public class TunnelClusterRedisStoreConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty("spring.redis.host")
    @ConditionalOnClass(StringRedisTemplate.class)
    public TunnelClusterStore tunnelClusterStore(@Autowired StringRedisTemplate redisTemplate) {
        RedisTunnelClusterStore redisTunnelClusterStore = new RedisTunnelClusterStore();
        redisTunnelClusterStore.setRedisTemplate(redisTemplate);
        return redisTunnelClusterStore;
    }
}
