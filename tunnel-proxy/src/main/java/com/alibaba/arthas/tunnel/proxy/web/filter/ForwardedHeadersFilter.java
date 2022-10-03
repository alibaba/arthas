package com.alibaba.arthas.tunnel.proxy.web.filter;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
public class ForwardedHeadersFilter implements HttpHeadersFilter, Ordered {

    public static final String FORWARDED_HEADER = "Forwarded";

    @Override
    public HttpHeaders filter(HttpHeaders headers, ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders updated = new HttpHeaders();

        headers.entrySet().stream().filter(entry -> !entry.getKey().toLowerCase().equalsIgnoreCase(FORWARDED_HEADER))
                .forEach(entry -> updated.addAll(entry.getKey(), entry.getValue()));

        List<Forwarded> forwardeds = parse(headers.get(FORWARDED_HEADER));

        for (Forwarded f : forwardeds) {
            updated.add(FORWARDED_HEADER, f.toHeaderValue());
        }

        URI uri = request.getURI();
        String host = headers.getFirst(HttpHeaders.HOST);
        Forwarded forwarded = new Forwarded().put("host", host).put("proto", uri.getScheme());

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            String forValue = remoteAddress.isUnresolved() ? remoteAddress.getHostName()
                    : remoteAddress.getAddress().getHostAddress();
            int port = remoteAddress.getPort();
            if (port >= 0) {
                forValue = forValue + ":" + port;
            }
            forwarded.put("for", forValue);
        }
        updated.add(FORWARDED_HEADER, forwarded.toHeaderValue());
        return updated;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Forwarded parse(String value) {
        String[] pairs = StringUtils.tokenizeToStringArray(value, ";");
        LinkedCaseInsensitiveMap<String> result = splitIntoCaseInsensitiveMap(pairs);
        if (result == null) {
            return null;
        }
        return new Forwarded(result);
    }

    private List<Forwarded> parse(List<String> values) {
        ArrayList<Forwarded> forwardeds = new ArrayList<>();
        if (CollectionUtils.isEmpty(values)) {
            return forwardeds;
        }

        for (String value : values) {
            Forwarded forwarded = parse(value);
            forwardeds.add(forwarded);
        }
        return forwardeds;
    }

    private LinkedCaseInsensitiveMap<String> splitIntoCaseInsensitiveMap(String[] pairs) {
        if (ObjectUtils.isEmpty(pairs)) {
            return null;
        }

        LinkedCaseInsensitiveMap<String> result = new LinkedCaseInsensitiveMap<>();
        for (String element : pairs) {
            String[] splittedElement = StringUtils.split(element, "=");
            if (splittedElement == null) {
                continue;
            }
            result.put(splittedElement[0].trim(), splittedElement[1].trim());
        }
        return result;
    }

    public static class Forwarded {

        private static final char EQUALS = '=';

        private static final char SEMICOLON = ';';

        private final Map<String, String> values;

        public Forwarded() {
            this.values = new HashMap<>();
        }

        public Forwarded(Map<String, String> values) {
            this.values = values;
        }

        public Forwarded put(String key, String value) {
            this.values.put(key, quoteIfNeeded(value));
            return this;
        }

        private String quoteIfNeeded(String s) {
            if (s != null && s.contains(":")) {
                return "\"" + s + "\"";
            }
            return s;
        }

        @Override
        public String toString() {
            return "Forwarded{" + "values=" + this.values + '}';
        }

        public String toHeaderValue() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : this.values.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(SEMICOLON);
                }
                builder.append(entry.getKey()).append(EQUALS).append(entry.getValue());
            }
            return builder.toString();
        }
    }
}
