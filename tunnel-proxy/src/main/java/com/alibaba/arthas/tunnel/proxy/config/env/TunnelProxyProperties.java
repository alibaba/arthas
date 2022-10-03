package com.alibaba.arthas.tunnel.proxy.config.env;

import lombok.Data;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;

/**
 * Tunnel Server 代理配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@Data
@ConfigurationProperties(prefix = TunnelProxyProperties.PREFIX)
public class TunnelProxyProperties {

    public static final String PREFIX = "arthas.tunnel";

    private Set<SecurityProperties.User> users;
}
