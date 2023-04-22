package com.alibaba.arthas.tunnel.proxy.config.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存自动配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@ComponentScan("com.alibaba.arthas.tunnel.server.app.configuration")
@EnableCaching
@Slf4j
@Configuration(proxyBeanMethods = false)
public class CacheAutoConfiguration {

}
