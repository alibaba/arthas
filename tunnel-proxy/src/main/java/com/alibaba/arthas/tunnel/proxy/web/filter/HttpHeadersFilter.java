package com.alibaba.arthas.tunnel.proxy.web.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
/**
 * TODO
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
public interface HttpHeadersFilter {

    static HttpHeaders filter(List<HttpHeadersFilter> filters,
                              ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        return filter(filters, headers, exchange, Type.REQUEST);
    }

    static HttpHeaders filter(List<HttpHeadersFilter> filters,
                              HttpHeaders headers, ServerWebExchange exchange,
                              Type type) {
        if (filters != null) {
            return filters.stream()
                    .filter(headersFilter -> headersFilter.supports(type))
                    .reduce(headers,
                            (httpHeaders, filter) -> filter.filter(httpHeaders, exchange),
                            (httpHeaders1, httpHeaders2) -> {
                        httpHeaders1.addAll(httpHeaders2);
                        return httpHeaders1;
                    });
        }
        return headers;
    }

    HttpHeaders filter(HttpHeaders headers, ServerWebExchange exchange);

    default boolean supports(Type type) {
        return type.equals(Type.REQUEST);
    }

    enum Type {

        REQUEST,
        RESPONSE;
    }
}
