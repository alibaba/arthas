package com.alibaba.arthas.tunnel.proxy.config.autoconfigure;

import com.alibaba.arthas.tunnel.proxy.web.security.DelegateReactiveUserDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

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

    @Primary
    @Bean
    public ReactiveUserDetailsService delegateReactiveUserDetailsService(List<ReactiveUserDetailsService> services) {
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
                        "/favicon.ico*",
                        "/actuator/**",
                        "/actuator",
                        "/*.css",
                        "/*.png",
                        "/*.js",
                        "/*.jpg",
                        "/*.ico",
                        "/static/**")
                .permitAll()
                .pathMatchers("/**")
                .authenticated()
                .and()
                .httpBasic()
                .disable()
                .formLogin()
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
}
