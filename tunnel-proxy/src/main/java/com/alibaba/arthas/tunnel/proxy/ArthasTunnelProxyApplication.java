package com.alibaba.arthas.tunnel.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * 启动入口
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@EnableWebFlux
@EnableDiscoveryClient
@SpringBootApplication
public class ArthasTunnelProxyApplication {

    public static void main(String[] args) {

        SpringApplication.run(ArthasTunnelProxyApplication.class, args);
    }
}
