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
 * 
 * @author hengyunabc 2020-11-03
 *
 */
@Configuration
@AutoConfigureBefore(TunnelClusterStoreConfiguration.class)
public class EmbeddedRedisConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "arthas", name = { "embedded-redis.enabled" })
    public RedisServer embeddedRedisServer(ArthasProperties arthasProperties) {
        EmbeddedRedis embeddedRedis = arthasProperties.getEmbeddedRedis();

        RedisServerBuilder builder = RedisServer.builder().port(embeddedRedis.getPort()).bind(embeddedRedis.getHost());

        for (String setting : embeddedRedis.getSettings()) {
            builder.setting(setting);
        }
        RedisServer redisServer = builder.build();
        return redisServer;
    }
}
