package com.alibaba.arthas.tunnel.proxy.config.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;
import reactor.netty.http.server.WebsocketServerSpec;

/**
 * WebSocket 自动配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class WebSocketAutoConfiguration {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.create();
    }

    @Bean
    public ReactorNettyWebSocketClient reactorNettyWebSocketClient(HttpClient httpClient) {
        return new ReactorNettyWebSocketClient(httpClient, WebsocketClientSpec.builder());
    }

    @Bean
    public ReactorNettyRequestUpgradeStrategy reactorNettyRequestUpgradeStrategy() {
        return new ReactorNettyRequestUpgradeStrategy(WebsocketServerSpec.builder());
    }

    @Bean
    public WebSocketService webSocketService(RequestUpgradeStrategy requestUpgradeStrategy) {
        return new HandshakeWebSocketService(requestUpgradeStrategy);
    }
}
