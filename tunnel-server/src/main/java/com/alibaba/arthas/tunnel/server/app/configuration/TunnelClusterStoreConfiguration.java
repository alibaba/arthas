package com.alibaba.arthas.tunnel.server.app.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import com.alibaba.arthas.tunnel.server.cluster.InMemoryClusterStore;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

/**
 * 
 * @author hengyunabc 2020-10-29
 *
 */
@AutoConfigureBefore(value = {TunnelServerConfiguration.class})
@EnableCaching
@Configuration
@Import(CacheAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine")
public class TunnelClusterStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TunnelClusterStore tunnelClusterStore(@Autowired CacheManager cacheManager) {
        Cache inMemoryClusterCache = cacheManager.getCache("inMemoryClusterCache");
        InMemoryClusterStore inMemoryClusterStore = new InMemoryClusterStore();
        inMemoryClusterStore.setCache(inMemoryClusterCache);
        return inMemoryClusterStore;
    }
}
