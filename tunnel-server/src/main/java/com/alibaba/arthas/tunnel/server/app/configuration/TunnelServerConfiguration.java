package com.alibaba.arthas.tunnel.server.app.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

/**
 * 隧道服务器配置类
 * 负责创建和配置TunnelServer实例
 * 在Redis自动配置之后执行，确保可以使用Redis相关的Bean
 *
 * @author hengyunabc 2020-10-27
 *
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class TunnelServerConfiguration {

    // 自动注入Arthas配置属性
    @Autowired
    ArthasProperties arthasProperties;

    /**
     * 创建TunnelServer Bean
     * 使用配置文件中的属性初始化服务器
     *
     * @param tunnelClusterStore 隧道集群存储对象，可选参数
     * @return 配置好的TunnelServer实例
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public TunnelServer tunnelServer(@Autowired(required = false) TunnelClusterStore tunnelClusterStore) {
        // 创建TunnelServer实例
        TunnelServer tunnelServer = new TunnelServer();

        // 从配置文件中读取并设置服务器监听的主机地址
        tunnelServer.setHost(arthasProperties.getServer().getHost());
        // 从配置文件中读取并设置服务器监听的端口号
        tunnelServer.setPort(arthasProperties.getServer().getPort());
        // 从配置文件中读取并设置是否启用SSL
        tunnelServer.setSsl(arthasProperties.getServer().isSsl());
        // 从配置文件中读取并设置WebSocket访问路径
        tunnelServer.setPath(arthasProperties.getServer().getPath());
        // 从配置文件中读取并设置客户端连接的主机地址（集群模式使用）
        tunnelServer.setClientConnectHost(arthasProperties.getServer().getClientConnectHost());
        // 如果存在集群存储对象，则设置到TunnelServer中
        if (tunnelClusterStore != null) {
            tunnelServer.setTunnelClusterStore(tunnelClusterStore);
        }
        // 返回配置好的TunnelServer实例
        return tunnelServer;
    }

}
