package com.alibaba.arthas.tunnel.proxy.config.autoconfigure;

import com.alibaba.arthas.tunnel.proxy.config.env.TunnelProxyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Tunnel Server 代理自动配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@EnableConfigurationProperties(TunnelProxyProperties.class)
@Slf4j
@Configuration
public class TunnelProxyAutoConfiguration  {

}
