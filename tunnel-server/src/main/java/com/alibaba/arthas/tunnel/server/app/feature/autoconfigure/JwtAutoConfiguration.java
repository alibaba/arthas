package com.alibaba.arthas.tunnel.server.app.feature.autoconfigure;

import com.alibaba.arthas.tunnel.server.app.feature.env.JwtProperties;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.config.JwtConfig;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenProvider;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenService;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenStore;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.token.AccessToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * JWT 自动装配
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
@Slf4j
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Configuration(proxyBeanMethods = false)
public class JwtAutoConfiguration {

    private final JwtProperties jwtProperties;

    @ConditionalOnMissingBean
    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtTokenStore jwtTokenStore) {
        JwtConfig jwtConfig = JwtConfig.builder()
                .header(jwtProperties.getHeader())
                .secret(jwtProperties.getSecret())
                .base64Secret(jwtProperties.getBase64Secret())
                .tokenValidityInSeconds(jwtProperties.getTokenValidityInSeconds())
                .tokenValidityInSecondsForRememberMe(jwtProperties.getTokenValidityInSecondsForRememberMe())
                .anonymousUrls(jwtProperties.getAnonymousUrls())
                .authenticatedUrls(jwtProperties.getAuthenticatedUrls())
                .permitAllUrls(jwtProperties.getPermitAllUrls())
                .build();
        return new JwtTokenProvider(jwtConfig, jwtTokenStore);
    }

    @ConditionalOnMissingBean
    @Bean
    public JwtTokenService jwtTokenService(AuthenticationManager authenticationManager,
                                           JwtTokenProvider jwtTokenProvider) {
        return new JwtTokenService(authenticationManager, jwtTokenProvider);
    }

    @Configuration(proxyBeanMethods = false)
    public static class InMemoryJwtAutoConfiguration {

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
}
