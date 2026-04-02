package com.alibaba.arthas.tunnel.server.app.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.arthas.tunnel.server.app.configuration.TunnelClusterStoreConfiguration.RedisTunnelClusterStoreConfiguration;
import com.alibaba.arthas.tunnel.server.cluster.InMemoryClusterStore;
import com.alibaba.arthas.tunnel.server.cluster.RedisTunnelClusterStore;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

/**
 * Tunnel集群存储配置类
 * <p>
 * 该类负责配置Tunnel服务器的集群存储实现，支持两种存储方式：
 * 1. 内存存储（基于Caffeine缓存）：适用于单机部署场景
 * 2. Redis存储：适用于分布式集群部署场景，实现多实例间的数据共享
 * </p>
 *
 * @author hengyunabc 2020-10-29
 *
 */
@Configuration
// 在Redis自动配置和缓存自动配置之后加载，确保相关的Bean已经准备就绪
@AutoConfigureAfter(value = { RedisAutoConfiguration.class, CacheAutoConfiguration.class })
// 导入Redis存储配置内部类
@Import(RedisTunnelClusterStoreConfiguration.class)
public class TunnelClusterStoreConfiguration {

    /**
     * 创建基于内存的Tunnel集群存储Bean
     * <p>
     * 当满足以下条件时创建此Bean：
     * 1. 容器中不存在TunnelClusterStore类型的Bean
     * 2. 配置属性spring.cache.type的值为"caffeine"
     * </p>
     *
     * @param cacheManager Spring缓存管理器，自动注入
     * @return 基于内存的Tunnel集群存储实例
     */
    @Bean
    // 当容器中不存在TunnelClusterStore类型的Bean时才创建
    @ConditionalOnMissingBean
    // 当配置属性spring.cache.type的值为"caffeine"时才创建
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine")
    public TunnelClusterStore tunnelClusterStore(@Autowired CacheManager cacheManager) {
        // 从缓存管理器获取名为"inMemoryClusterCache"的缓存实例
        Cache inMemoryClusterCache = cacheManager.getCache("inMemoryClusterCache");
        // 创建内存集群存储实例
        InMemoryClusterStore inMemoryClusterStore = new InMemoryClusterStore();
        // 设置缓存到存储实例中
        inMemoryClusterStore.setCache(inMemoryClusterCache);
        return inMemoryClusterStore;
    }

    /**
     * Redis集群存储配置内部类
     * <p>
     * 该内部类负责配置基于Redis的Tunnel集群存储实现，
     * 适用于分布式部署场景，多个Tunnel服务器实例可以通过Redis共享集群状态信息
     * </p>
     */
    static class RedisTunnelClusterStoreConfiguration {
        /**
         * 创建基于Redis的Tunnel集群存储Bean
         * <p>
         * 当满足以下条件时创建此Bean：
         * 1. 类路径中存在StringRedisTemplate类
         * 2. 配置了spring.redis.host属性
         * 3. 容器中不存在TunnelClusterStore类型的Bean
         * </p>
         *
         * @param redisTemplate Redis字符串模板，自动注入
         * @return 基于Redis的Tunnel集群存储实例
         */
        @Bean
        // 当类路径中存在StringRedisTemplate类时才创建（即项目中有Redis相关依赖）
        // @ConditionalOnBean(StringRedisTemplate.class)
        @ConditionalOnClass(StringRedisTemplate.class)
        // 当配置了spring.redis.host属性时才创建（即配置了Redis连接信息）
        @ConditionalOnProperty("spring.redis.host")
        // 当容器中不存在TunnelClusterStore类型的Bean时才创建
        @ConditionalOnMissingBean
        public TunnelClusterStore tunnelClusterStore(@Autowired StringRedisTemplate redisTemplate) {
            // 创建Redis隧道集群存储实例
            RedisTunnelClusterStore redisTunnelClusterStore = new RedisTunnelClusterStore();
            // 设置Redis模板到存储实例中，用于与Redis进行交互
            redisTunnelClusterStore.setRedisTemplate(redisTemplate);
            return redisTunnelClusterStore;
        }
    }

}
