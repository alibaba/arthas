package com.alibaba.arthas.channel.server;

import com.alibaba.arthas.channel.server.autoconfigure.ChannelServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private ChannelServerProperties channelServerProperties;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        String username = channelServerProperties.getAuth().getUsername();
        if (StringUtils.hasText(username)) {
            auth.inMemoryAuthentication()
                    //.passwordEncoder(new BCryptPasswordEncoder())
                    .passwordEncoder(NoOpPasswordEncoder.getInstance()) // CHANGE IT for production
                    .withUser(username)
                    .password(channelServerProperties.getAuth().getPassword())
                    .roles("USER");
        }
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        String username = channelServerProperties.getAuth().getUsername();
        if (StringUtils.hasText(username)) {
            http.csrf().disable().authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .httpBasic();
        }
    }
}