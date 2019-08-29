package com.alibaba.arthas.tunnel.server.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.arthas.tunnel.server.TunnelServer;

@Configuration
public class TunnelServerConfiguration {

    @Autowired
    ArthasProperties arthasProperties;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public TunnelServer tunnelServer() {
        TunnelServer tunnelServer = new TunnelServer();

        tunnelServer.setHost(arthasProperties.getServer().getHost());
        tunnelServer.setPort(arthasProperties.getServer().getPort());
        tunnelServer.setSsl(arthasProperties.getServer().isSsl());
        return tunnelServer;
    }

}
