package com.alibaba.arthas.tunnel.proxy.config.autoconfigure;

import com.alibaba.arthas.tunnel.proxy.config.env.TunnelProxyProperties;
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
@EnableConfigurationProperties(TunnelProxyProperties.class)
@RequiredArgsConstructor
@Slf4j
@Configuration
public class TunnelProxyAutoConfiguration  {

    private final TunnelProxyProperties tunnelProxyProperties;

    @Primary
    @Bean(initMethod = "start", destroyMethod = "stop")
    public TunnelServer tunnelServer(@Autowired(required = false) TunnelClusterStore tunnelClusterStore) {
        TunnelServer tunnelServer = new TunnelServer();

        tunnelServer.setHost(tunnelProxyProperties.getServer().getHost());
        tunnelServer.setPort(tunnelProxyProperties.getServer().getPort());
        tunnelServer.setSsl(tunnelProxyProperties.getServer().isSsl());
        tunnelServer.setPath(tunnelProxyProperties.getServer().getPath());
        tunnelServer.setClientConnectHost(tunnelProxyProperties.getServer().getClientConnectHost());
        if (tunnelClusterStore != null) {
            tunnelServer.setTunnelClusterStore(tunnelClusterStore);
        }
        return tunnelServer;
    }
}
