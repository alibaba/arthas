package com.alibaba.arthas.tunnel.server.feature.autoconfigure;

import com.alibaba.arthas.tunnel.server.feature.security.DelegateReactiveUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

/**
 * Security 自动配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@AutoConfigureAfter(ReactiveUserDetailsServiceAutoConfiguration.class)
@EnableWebFluxSecurity
@Slf4j
@Configuration(proxyBeanMethods = false)
public class SecurityAutoConfiguration {

    private final ServerAuthenticationFailureHandler failureHandler = new FormAuthenticationFailureHandler("/login?error");

    @Primary
    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService(List<ReactiveUserDetailsService> services) {
        return new DelegateReactiveUserDetailsService(services);
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(ReactiveUserDetailsService userDetailsService,
                                                                       ObjectProvider<PasswordEncoder> passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        if (passwordEncoder.getIfAvailable() != null) {
            authenticationManager.setPasswordEncoder(passwordEncoder.getIfAvailable());
        }
        return authenticationManager;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange()
                .pathMatchers(
                        "/login*",
                        "/logout**",
                        "/favicon.ico",
                        "/actuator/**",
                        "/actuator",
                        "*.css",
                        "*.png",
                        "*.js",
                        "*.jpg",
                        "*.ico",
                        "*.otf",
                        "*.eot",
                        "*.svg",
                        "*.ttf",
                        "*.woff",
                        "/static/**",
                        "/public/**")
                .permitAll()
                .pathMatchers("/**")
                .authenticated()
                .and()
                .httpBasic()
                .disable()
                .formLogin()
                .loginPage("/login")
                .requiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST,"/login"))
                .authenticationFailureHandler(failureHandler)
                .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"))
                .and()
                .logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(logoutSuccessHandler("/"))
                .and()
                .build();
    }

    private ServerLogoutSuccessHandler logoutSuccessHandler(String uri) {
        RedirectServerLogoutSuccessHandler successHandler =
                new RedirectServerLogoutSuccessHandler();
        successHandler.setLogoutSuccessUrl(URI.create(uri));
        return successHandler;
    }

    private static class FormAuthenticationFailureHandler implements ServerAuthenticationFailureHandler {

        private final URI location;

        private ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

        public FormAuthenticationFailureHandler(String location) {
            Assert.notNull(location, "location cannot be null");
            this.location = URI.create(location);
        }

        @Override
        public Mono<Void> onAuthenticationFailure(WebFilterExchange exchange, AuthenticationException exception) {
            exchange.getExchange().getSession().block().getAttributes().put(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
            return this.redirectStrategy.sendRedirect(exchange.getExchange(), this.location);
        }
    }
}
