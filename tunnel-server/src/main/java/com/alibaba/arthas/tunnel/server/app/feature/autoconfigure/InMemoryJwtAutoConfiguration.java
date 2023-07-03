package com.alibaba.arthas.tunnel.server.app.feature.autoconfigure;

import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenStore;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.token.AccessToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
@Configuration(proxyBeanMethods = false)
public class InMemoryJwtAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public JwtTokenStore tokenStore() {
        return new JwtTokenStore() {

            @Override
            public boolean validateAccessToken(AccessToken token) {
                return true;
            }

            @Override
            public void storeAccessToken(AccessToken token) {

            }

            @Override
            public void removeAccessToken(AccessToken token) {

            }
        };
    }
}