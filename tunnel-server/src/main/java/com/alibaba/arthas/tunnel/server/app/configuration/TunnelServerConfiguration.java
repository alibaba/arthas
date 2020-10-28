package com.alibaba.arthas.tunnel.server.app.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.alibaba.arthas.tunnel.server.RedisTunnelClusterStore;
import com.alibaba.arthas.tunnel.server.TunnelClusterStore;
import com.alibaba.arthas.tunnel.server.TunnelServer;

/**
 * 
 * @author hengyunabc 2020-10-27
 *
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class TunnelServerConfiguration {

    @Autowired
    ArthasProperties arthasProperties;

    @Bean
//    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnClass(StringRedisTemplate.class)
    @ConditionalOnProperty("spring.redis.host")
    @ConditionalOnMissingBean
    public TunnelClusterStore tunnelClusterStore(@Autowired StringRedisTemplate redisTemplate) {
        RedisTunnelClusterStore redisTunnelClusterStore = new RedisTunnelClusterStore();
        redisTunnelClusterStore.setRedisTemplate(redisTemplate);
        return redisTunnelClusterStore;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public TunnelServer tunnelServer(@Autowired(required = false) TunnelClusterStore tunnelClusterStore) {
        TunnelServer tunnelServer = new TunnelServer();

        tunnelServer.setHost(arthasProperties.getServer().getHost());
        tunnelServer.setPort(arthasProperties.getServer().getPort());
        tunnelServer.setSsl(arthasProperties.getServer().isSsl());
        tunnelServer.setPath(arthasProperties.getServer().getPath());
        tunnelServer.setClientConnectHost(arthasProperties.getServer().getClientConnectHost());
        if (tunnelClusterStore != null) {
            tunnelServer.setTunnelClusterStore(tunnelClusterStore);
        }
        return tunnelServer;
    }

}
