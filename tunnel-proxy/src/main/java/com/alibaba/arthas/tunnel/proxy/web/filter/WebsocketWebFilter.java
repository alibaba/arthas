package com.alibaba.arthas.tunnel.proxy.web.filter;

import com.alibaba.arthas.tunnel.server.AgentInfo;
import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Websocket 过滤器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class WebsocketWebFilter implements WebFilter {

    public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    private static final Pattern PATTERN = Pattern.compile("^/arthas/(.+)/ws$");

    private final WebSocketClient webSocketClient;

    private final WebSocketService webSocketService;

    private final TunnelServer tunnelServer;

    private final ArthasProperties arthasProperties;

    private volatile List<HttpHeadersFilter> headersFilters;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        URI uri = exchange.getRequest().getURI();
        AgentInfo agentInfo = findArthasAgent(uri);
        if (agentInfo == null) {
            return chain.filter(exchange);
        }

        HttpHeaders headers = exchange.getRequest().getHeaders();
        HttpHeaders filtered = HttpHeadersFilter.filter(getHeadersFilters(), exchange);

        List<String> protocols = headers.get(SEC_WEBSOCKET_PROTOCOL);
        if (protocols != null) {
            protocols = headers.get(SEC_WEBSOCKET_PROTOCOL).stream()
                    .flatMap(header -> Arrays.stream(StringUtils.commaDelimitedListToStringArray(header)))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        URI requestUrl = getForwardUrl(uri);
        return this.webSocketService.handleRequest(exchange,
                new WebsocketWebFilter.ProxyWebSocketHandler(
                        this.webSocketClient, requestUrl, filtered, protocols));
    }

    private AgentInfo findArthasAgent(URI uri) {
        String path = uri.getPath();
        Matcher matcher = PATTERN.matcher(path);
        if (!matcher.find()) {
            return null;
        }
        String serviceId = matcher.group(1);
        return tunnelServer.getAgentInfoMap().get(serviceId);
    }

    private URI getForwardUrl(URI uri) {
        ArthasProperties.Server server = arthasProperties.getServer();
        String query = UriUtils.encodeQuery(uri.getQuery(), CharsetUtil.UTF_8.name());
        String url = "ws://" + server.getClientConnectHost() + ":" + server.getPort() + "/ws";
        if (!Strings.isNullOrEmpty(url)) {
            url += "?" + query;
        }
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private List<HttpHeadersFilter> getHeadersFilters() {
        if (this.headersFilters == null) {
            this.headersFilters = Lists.newArrayList(
                    new ForwardedHeadersFilter(),
                    new XForwardedHeadersFilter(),
                    new RemoveHopByHopHeadersFilter());

            headersFilters.add((headers, exchange) -> {
                HttpHeaders filtered = new HttpHeaders();
                filtered.addAll(headers);
                filtered.remove(HttpHeaders.HOST);
                return filtered;
            });

            headersFilters.add((headers, exchange) -> {
                HttpHeaders filtered = new HttpHeaders();
                headers.entrySet().stream().filter(entry -> !entry.getKey().toLowerCase().startsWith("sec-websocket"))
                        .forEach(header -> filtered.addAll(header.getKey(), header.getValue()));
                return filtered;
            });
        }

        return this.headersFilters;
    }

    @RequiredArgsConstructor
    public static class ProxyWebSocketHandler implements WebSocketHandler {

        private final WebSocketClient webSocketClient;

        private final URI uri;

        private final HttpHeaders httpHeaders;

        private final List<String> subProtocols;

        @Override
        public Mono<Void> handle(WebSocketSession session) {
            return webSocketClient.execute(this.uri, this.httpHeaders, new WebSocketHandler() {

                @Override
                public Mono<Void> handle(WebSocketSession proxySession) {
                    Mono<Void> proxySessionSend =
                            proxySession.send(session.receive().doOnNext(WebSocketMessage::retain));
                    Mono<Void> serverSessionSend =
                            session.send(proxySession.receive().doOnNext(WebSocketMessage::retain));
                    return Mono.zip(proxySessionSend, serverSessionSend).then();
                }

                @Override
                public List<String> getSubProtocols() {
                    return WebsocketWebFilter.ProxyWebSocketHandler.this.subProtocols;
                }
            });
        }

        @Override
        public List<String> getSubProtocols() {
            return this.subProtocols;
        }
    }
}
