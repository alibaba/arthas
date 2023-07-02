package com.alibaba.arthas.tunnel.server.app.feature.autoconfigure;

import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.alibaba.arthas.tunnel.server.app.feature.env.SecurityProperties;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.filter.JwtAuthorizationFilter;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.handler.ForbiddenAccessDeniedHandler;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.handler.UnauthorizedEntryPoint;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.config.JwtSecurityConfigurer;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenProvider;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.user.LoginUserDetailsService;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.user.SecurityUserHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.CorsFilter;

/**
 * Web 自定义授权配置
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
@Slf4j
@Configuration(proxyBeanMethods = false)
public class WebSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final ArthasProperties arthasProperties;

    private final CorsFilter corsFilter;

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(new UnauthorizedEntryPoint())
                .accessDeniedHandler(new ForbiddenAccessDeniedHandler())
                .and()
                .authorizeRequests()
                .antMatchers("/api/auth/*").permitAll()
                .anyRequest().authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .apply(securityConfigurationAdapter());

        // allow iframe
        if (arthasProperties.isEnableIframeSupport()) {
            httpSecurity.headers().frameOptions().disable();
        }
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public UserDetailsService userDetailsService(SecurityProperties securityProperties,
                                                 SecurityUserHelper securityUserHelper) {
        return new LoginUserDetailsService(securityProperties, securityUserHelper);
    }

    private JwtSecurityConfigurer securityConfigurationAdapter() {
        JwtAuthorizationFilter filter = new JwtAuthorizationFilter(authenticationManager,
                jwtTokenProvider, antPathMatcher);
        return new JwtSecurityConfigurer(filter);
    }
}
