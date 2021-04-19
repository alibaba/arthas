package com.alibaba.arthas.tunnel.server.configuration;

import com.alibaba.arthas.tunnel.server.cluster.InMemoryClusterStore;
import com.alibaba.arthas.tunnel.server.cluster.RedisTunnelClusterStore;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;
import com.alibaba.arthas.tunnel.server.configuration.TunnelClusterStoreConfiguration.RedisTunnelClusterStoreConfiguration;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import redis.embedded.RedisServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 *
 * @author hengyunabc 2020-10-29
 * @author Naah 2021-04-17
 *
 */
@Configuration
@AutoConfigureAfter(value = { RedisAutoConfiguration.class, CacheAutoConfiguration.class })
@Import(RedisTunnelClusterStoreConfiguration.class)
public class TunnelClusterStoreConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TunnelClusterStoreConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public TunnelClusterStore tunnelClusterStore(CacheManager cacheManager, CacheProperties cacheProperties, @Autowired(required = false) RedisServer c, ApplicationContext context) {
        if ("org.springframework.data.redis.cache.RedisCacheManager".equals(cacheManager.getClass().getName())) {
            try {
                Class<?> redisConnectionFactoryClass = Class.forName("org.springframework.data.redis.connection.RedisConnectionFactory");
                Object bean = context.getBean(redisConnectionFactoryClass);
                Object connection = redisConnectionFactoryClass.getMethod("getConnection").invoke(bean);
                Class<?> redisConnectionClass = Class.forName("org.springframework.data.redis.connection.RedisConnection");
                redisConnectionClass.getMethod("close").invoke(connection);
            } catch (Exception e) {
                logger.warn("redis connection is not available，cache: Caffine");
                CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
                String defaultCacheName = "inMemoryClusterCache";
                String defaultCacheSpecification = "maximumSize=3000,expireAfterAccess=3600s";
                if (Objects.nonNull(cacheProperties)) {
                    List<String> cacheNames = Optional.ofNullable(cacheProperties.getCacheNames()).orElse(new ArrayList<>(1));
                    cacheNames.add("inMemoryClusterCache");
                    caffeineCacheManager.setCacheNames(cacheNames);

                    CacheProperties.Caffeine caffeine = cacheProperties.getCaffeine();
                    if (Objects.nonNull(caffeine) && StringUtils.isNotBlank(caffeine.getSpec())) {
                        caffeineCacheManager.setCacheSpecification(Strings.join(Lists.newArrayList(caffeine.getSpec(), defaultCacheName), ','));
                    } else {
                        caffeineCacheManager.setCacheSpecification(defaultCacheSpecification);
                    }
                } else {
                    caffeineCacheManager.setCacheNames(Lists.newArrayList(defaultCacheName));
                    caffeineCacheManager.setCacheSpecification(defaultCacheSpecification);
                }
                cacheManager = caffeineCacheManager;
            }
        }
        Cache inMemoryClusterCache = cacheManager.getCache("inMemoryClusterCache");
        InMemoryClusterStore inMemoryClusterStore = new InMemoryClusterStore();
        inMemoryClusterStore.setCache(inMemoryClusterCache);
        return inMemoryClusterStore;
    }

    static class RedisTunnelClusterStoreConfiguration {
        @Bean
        @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
        @ConditionalOnProperty("spring.redis.host")
        @ConditionalOnMissingBean
        public TunnelClusterStore tunnelClusterStore(@Autowired ApplicationContext context) throws ClassNotFoundException {
            RedisTunnelClusterStore redisTunnelClusterStore = new RedisTunnelClusterStore();
            redisTunnelClusterStore.setRedisTemplate(context.getBean(Class.forName("org.springframework.data.redis.core.StringRedisTemplate")));
            return redisTunnelClusterStore;
        }
    }

}
