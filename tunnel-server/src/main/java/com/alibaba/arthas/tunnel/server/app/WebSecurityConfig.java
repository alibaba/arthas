package com.alibaba.arthas.tunnel.server.app;

import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.alibaba.arthas.tunnel.server.app.feature.env.ArthasTunnelProperties;
import com.alibaba.arthas.tunnel.server.app.feature.security.ArthasUserDetailsService;
import com.alibaba.arthas.tunnel.server.app.feature.security.SecurityUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 
 * @author hengyunabc 2021-08-11
 *
 */
@RequiredArgsConstructor
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final ArthasProperties arthasProperties;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.authorizeRequests().requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated().anyRequest()
        .permitAll().and().formLogin();
        // allow iframe
        if (arthasProperties.isEnableIframeSupport()) {
            httpSecurity.headers().frameOptions().disable();
        }
    }

    @Bean
    public UserDetailsService userDetailsService(ArthasTunnelProperties arthasTunnelProperties, SecurityUserHelper securityUserHelper) {
        return new ArthasUserDetailsService(arthasTunnelProperties, securityUserHelper);
    }
}