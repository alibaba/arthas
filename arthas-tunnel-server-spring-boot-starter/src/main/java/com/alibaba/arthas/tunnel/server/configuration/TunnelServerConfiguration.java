package com.alibaba.arthas.tunnel.server.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

import java.util.Objects;

/**
 *
 * @author hengyunabc 2020-10-27
 * @author Naah 2021-04-17
 *
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class TunnelServerConfiguration {

    @Autowired
    ArthasProperties arthasProperties;

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public TunnelServer tunnelServer(TunnelClusterStore tunnelClusterStore) {
        TunnelServer tunnelServer = new TunnelServer();
        if(Objects.isNull(arthasProperties.getServer())){
            ArthasProperties.Server server = new ArthasProperties.Server();
            server.setHost("0.0.0.0");
            server.setPort(7777);
            arthasProperties.setServer(server);
        }
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
