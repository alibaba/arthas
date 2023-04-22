package com.alibaba.arthas.tunnel.proxy.config.autoconfigure;

import com.alibaba.arthas.tunnel.proxy.config.env.TunnelProperties;
import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Tunnel Server 代理自动配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@EnableConfigurationProperties(TunnelProperties.class)
@RequiredArgsConstructor
@Slf4j
@Configuration(proxyBeanMethods = false)
public class TunnelAutoConfiguration {

    private final TunnelProperties tunnelProperties;

    @Primary
    @Bean(initMethod = "start", destroyMethod = "stop")
    public TunnelServer tunnelServer(@Autowired(required = false) TunnelClusterStore tunnelClusterStore) {
        TunnelServer tunnelServer = new TunnelServer();
        tunnelServer.setPort(tunnelProperties.getServer().getPort());
        tunnelServer.setSsl(tunnelProperties.getServer().isSsl());
        tunnelServer.setPath(tunnelProperties.getServer().getPath());
        tunnelServer.setClientConnectHost(tunnelProperties.getServer().getClientConnectHost());
        if (tunnelClusterStore != null) {
            tunnelServer.setTunnelClusterStore(tunnelClusterStore);
        }
        return tunnelServer;
    }
}
