package com.alibaba.arthas.tunnel.server.app.feature.autoconfigure;

import com.alibaba.arthas.tunnel.server.app.feature.env.ArthasTunnelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ArthasTunnel 自动装配
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@EnableConfigurationProperties(ArthasTunnelProperties.class)
@RequiredArgsConstructor
@Slf4j
@Configuration(proxyBeanMethods = false)
public class ArthasTunnelAutoConfiguration {
}
