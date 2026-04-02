package com.alibaba.arthas.tunnel.server.app.configuration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties.EmbeddedRedis;

import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;

/**
 * 嵌入式 Redis 配置类
 * <p>
 * 该配置类用于在 Tunnel Server 中启动一个嵌入式 Redis 服务器。
 * 当没有配置外部 Redis 时，可以使用嵌入式 Redis 作为数据存储。
 * 主要用于存储集群中的 Agent 连接信息。
 * </p>
 * <p>
 * 配置属性：
 * - arthas.embedded-redis.enabled: 是否启用嵌入式 Redis（默认为 false）
 * - arthas.embedded-redis.port: Redis 服务端口（默认为 6379）
 * - arthas.embedded-redis.host: Redis 绑定地址（默认为 localhost）
 * - arthas.embedded-redis.settings: Redis 配置参数列表
 * </p>
 *
 * @author hengyunabc 2020-11-03
 *
 */
@Configuration
@AutoConfigureBefore(TunnelClusterStoreConfiguration.class)
public class EmbeddedRedisConfiguration {

    /**
     * 创建嵌入式 Redis 服务器 Bean
     * <p>
     * 该方法会根据配置属性创建并启动一个嵌入式 Redis 服务器。
     * Spring 容器会在初始化时调用 start() 方法启动 Redis，
     * 在容器销毁时调用 stop() 方法关闭 Redis。
     * </p>
     *
     * @param arthasProperties Arthas 配置属性，包含嵌入式 Redis 的配置信息
     * @return Redis 服务器实例
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "arthas", name = { "embedded-redis.enabled" })
    public RedisServer embeddedRedisServer(ArthasProperties arthasProperties) {
        // 从配置属性中获取嵌入式 Redis 的配置
        EmbeddedRedis embeddedRedis = arthasProperties.getEmbeddedRedis();

        // 创建 Redis 服务器构建器
        // 设置端口和绑定地址
        RedisServerBuilder builder = RedisServer.builder().port(embeddedRedis.getPort()).bind(embeddedRedis.getHost());

        // 应用所有额外的 Redis 配置参数
        // 这些参数可以用于设置 Redis 的各种选项，如最大内存、持久化策略等
        for (String setting : embeddedRedis.getSettings()) {
            builder.setting(setting);
        }

        // 构建并返回 Redis 服务器实例
        RedisServer redisServer = builder.build();
        return redisServer;
    }
}
