package com.alibaba.arthas.tunnel.server.app.feature.autoconfigure;

import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.alibaba.arthas.tunnel.server.app.feature.env.SecurityProperties;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.handler.ForbiddenAccessDeniedHandler;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.handler.UnauthorizedEntryPoint;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.config.JwtSecurityConfigurer;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.filter.JwtAuthorizationFilter;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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

    private final SecurityProperties securityProperties;

    private final ArthasProperties arthasProperties;

    private final CorsFilter corsFilter;

    private final JwtTokenProvider jwtTokenProvider;

    private final UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder;

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf().disable()
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(new UnauthorizedEntryPoint())
                .accessDeniedHandler(new ForbiddenAccessDeniedHandler())
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .httpBasic()
                .and()
                .apply(securityConfigurationAdapter());

        httpSecurity.authorizeRequests()
                .antMatchers("/api/auth").permitAll()
                .antMatchers("/api/**").authenticated()
                .anyRequest().permitAll();

        /*if (CollectionUtils.isNotEmpty(securityProperties.getAnonymousUrls())) {
            List<String> anonymousUrls = securityProperties.getAnonymousUrls();
            String[] urls = anonymousUrls.toArray(new String[0]);
            httpSecurity.authorizeRequests().antMatchers(urls).anonymous();
        }*/

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

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
    }

    @Override
    protected UserDetailsService userDetailsService() {
        return userDetailsService;
    }

    private JwtSecurityConfigurer securityConfigurationAdapter() {
        JwtAuthorizationFilter filter = new JwtAuthorizationFilter(jwtTokenProvider);
        return new JwtSecurityConfigurer(filter);
    }
}
