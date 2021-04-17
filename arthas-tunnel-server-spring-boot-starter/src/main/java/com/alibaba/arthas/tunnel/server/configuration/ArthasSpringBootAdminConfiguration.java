package com.alibaba.arthas.tunnel.server.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;


/**
 *
 * @author naah 2021-04-17
 *
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(ArthasProperties.class)
public class ArthasSpringBootAdminConfiguration {
}
