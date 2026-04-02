package com.alibaba.arthas.tunnel.server.endpoint;

import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;

/**
 * Arthas 终端端点自动配置类
 *
 * <p>这是一个 Spring Boot 自动配置类，用于自动配置和创建 ArthasEndpoint Bean。</p>
 * <p>该类负责：</p>
 * <ul>
 *   <li>启用 ArthasProperties 配置属性</li>
 *   <li>创建 ArthasEndpoint 实例</li>
 *   <li>仅在端点可用且不存在同名 Bean 时才创建</li>
 * </ul>
 *
 * @author hengyunabc 2019-08-29
 */
@EnableConfigurationProperties(ArthasProperties.class)
@Configuration
public class ArthasEndPointAutoconfiguration {

    /**
     * 创建 ArthasEndpoint Bean
     *
     * <p>该方法创建并返回一个新的 ArthasEndpoint 实例，用于暴露 Arthas 的监控和管理端点。</p>
     * <p>创建条件：</p>
     * <ul>
     *   <li>容器中不存在同名的 Bean（@ConditionalOnMissingBean）</li>
     *   <li>端点在当前环境中可用（@ConditionalOnAvailableEndpoint）</li>
     * </ul>
     *
     * @return 新创建的 ArthasEndpoint 实例
     */
    @ConditionalOnMissingBean
    @Bean
    @ConditionalOnAvailableEndpoint
    public ArthasEndpoint arthasEndPoint() {
        return new ArthasEndpoint();
    }
}
