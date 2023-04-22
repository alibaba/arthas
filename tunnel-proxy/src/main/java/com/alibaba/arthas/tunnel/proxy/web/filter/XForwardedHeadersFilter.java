package com.alibaba.arthas.tunnel.proxy.web.filter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * x-forwarded 请求头过滤器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@Data
@ConfigurationProperties("spring.cloud.gateway.x-forwarded")
public class XForwardedHeadersFilter implements HttpHeadersFilter, Ordered {

    /** Default http port. */
    public static final int HTTP_PORT = 80;

    /** Default https port. */
    public static final int HTTPS_PORT = 443;

    /** Http url scheme. */
    public static final String HTTP_SCHEME = "http";

    /** Https url scheme. */
    public static final String HTTPS_SCHEME = "https";

    /** X-Forwarded-For Header. */
    public static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";

    /** X-Forwarded-Host Header. */
    public static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";

    /** X-Forwarded-Port Header. */
    public static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    /** X-Forwarded-Proto Header. */
    public static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";

    /** X-Forwarded-Prefix Header. */
    public static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";

    /** The order of the XForwardedHeadersFilter. */
    private int order = 0;

    /** If the XForwardedHeadersFilter is enabled. */
    private boolean enabled = true;

    /** If X-Forwarded-For is enabled. */
    private boolean forEnabled = true;

    /** If X-Forwarded-Host is enabled. */
    private boolean hostEnabled = true;

    /** If X-Forwarded-Port is enabled. */
    private boolean portEnabled = true;

    /** If X-Forwarded-Proto is enabled. */
    private boolean protoEnabled = true;

    /** If X-Forwarded-Prefix is enabled. */
    private boolean prefixEnabled = true;

    /** If appending X-Forwarded-For as a list is enabled. */
    private boolean forAppend = true;

    /** If appending X-Forwarded-Host as a list is enabled. */
    private boolean hostAppend = true;

    /** If appending X-Forwarded-Port as a list is enabled. */
    private boolean portAppend = true;

    /** If appending X-Forwarded-Proto as a list is enabled. */
    private boolean protoAppend = true;

    /** If appending X-Forwarded-Prefix as a list is enabled. */
    private boolean prefixAppend = true;

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public HttpHeaders filter(HttpHeaders headers, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders updated = new HttpHeaders();

        headers.forEach(updated::addAll);
        if (isForEnabled() && request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            String remoteAddr = request.getRemoteAddress().getAddress().getHostAddress();
            write(updated, X_FORWARDED_FOR_HEADER, remoteAddr, isForAppend());
        }

        String proto = request.getURI().getScheme();
        if (isProtoEnabled()) {
            write(updated, X_FORWARDED_PROTO_HEADER, proto, isProtoAppend());
        }

        if (isPortEnabled()) {
            String port = String.valueOf(request.getURI().getPort());
            if (request.getURI().getPort() < 0) {
                port = String.valueOf(getDefaultPort(proto));
            }
            write(updated, X_FORWARDED_PORT_HEADER, port, isPortAppend());
        }

        if (isHostEnabled()) {
            String host = toHostHeader(request);
            write(updated, X_FORWARDED_HOST_HEADER, host, isHostAppend());
        }

        return updated;
    }

    private void write(HttpHeaders headers, String name, String value, boolean append) {
        if (append) {
            headers.add(name, value);
            List<String> values = headers.get(name);
            String delimitedValue = StringUtils.collectionToCommaDelimitedString(values);
            headers.set(name, delimitedValue);
        }
        else {
            headers.set(name, value);
        }
    }

    private int getDefaultPort(String scheme) {
        return HTTPS_SCHEME.equals(scheme) ? HTTPS_PORT : HTTP_PORT;
    }

    private String toHostHeader(ServerHttpRequest request) {
        int port = request.getURI().getPort();
        String host = request.getURI().getHost();
        String scheme = request.getURI().getScheme();
        if (port < 0 || (port == HTTP_PORT && HTTP_SCHEME.equals(scheme))
                || (port == HTTPS_PORT && HTTPS_SCHEME.equals(scheme))) {
            return host;
        }
        return host + ":" + port;
    }
}
