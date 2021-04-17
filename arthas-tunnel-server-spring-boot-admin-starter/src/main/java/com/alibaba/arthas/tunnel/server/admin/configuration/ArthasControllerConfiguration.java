package com.alibaba.arthas.tunnel.server.admin.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Naah 2021-04-16
 *
 */
@Configuration
@ComponentScan(
        basePackages = {"com.alibaba.arthas.tunnel.server.admin.controller"}
)
public class ArthasControllerConfiguration {
}
