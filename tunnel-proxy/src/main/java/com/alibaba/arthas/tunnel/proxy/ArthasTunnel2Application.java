package com.alibaba.arthas.tunnel.server2;

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
public class ArthasTunnel2Application {

    public static void main(String[] args) {
        SpringApplication.run(ArthasTunnel2Application.class, args);
    }
}
