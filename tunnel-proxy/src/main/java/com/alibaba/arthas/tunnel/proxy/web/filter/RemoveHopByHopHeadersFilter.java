package com.alibaba.arthas.tunnel.proxy.web.filter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@Data
@ConfigurationProperties("spring.cloud.gateway.filter.remove-hop-by-hop")
public class RemoveHopByHopHeadersFilter implements HttpHeadersFilter, Ordered {

    public static final Set<String> HEADERS_REMOVED_ON_REQUEST = new HashSet<>(
            Arrays.asList("connection", "keep-alive", "transfer-encoding", "te", "trailer", "proxy-authorization",
                    "proxy-authenticate", "x-application-context", "upgrade"));

    private Set<String> httpHeaders = HEADERS_REMOVED_ON_REQUEST;

    @Override
    public HttpHeaders filter(HttpHeaders headers, ServerWebExchange exchange) {
        HttpHeaders filtered = new HttpHeaders();

        headers.entrySet().stream()
                .filter(entry -> !this.httpHeaders.contains(entry.getKey().toLowerCase()))
                .forEach(entry -> filtered.addAll(entry.getKey(), entry.getValue()));

        return filtered;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public boolean supports(Type type) {
        return type.equals(Type.REQUEST) || type.equals(Type.RESPONSE);
    }
}
