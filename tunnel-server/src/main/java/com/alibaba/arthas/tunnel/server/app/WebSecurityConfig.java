package com.alibaba.arthas.tunnel.server.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;

/**
 * 
 * @author hengyunabc 2021-08-11
 *
 */
@Configuration
public class WebSecurityConfig {

    @Autowired
    ArthasProperties arthasProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeHttpRequests((authz) -> authz
                .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated()
                .anyRequest().permitAll())
            .formLogin((form) -> form.permitAll());
        
        // allow iframe
        if (arthasProperties.isEnableIframeSupport()) {
            httpSecurity.headers((headers) -> headers.frameOptions((frame) -> frame.disable()));
        }
        
        return httpSecurity.build();
    }
}
