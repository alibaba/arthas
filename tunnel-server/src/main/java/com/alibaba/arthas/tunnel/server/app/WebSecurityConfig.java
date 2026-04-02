package com.alibaba.arthas.tunnel.server.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;

/**
 * Web安全配置类
 * <p>
 * 该类负责配置Spring Security的Web安全策略，控制HTTP请求的访问权限。
 * 主要功能包括：
 * 1. 配置Actuator端点的访问控制（需要认证）
 * 2. 配置其他请求的访问控制（允许所有）
 * 3. 配置表单登录
 * 4. 配置iframe支持（可选）
 * </p>
 *
 * @author hengyunabc 2021-08-11
 *
 */
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    // Arthas配置属性，包含应用程序的各种配置选项
    @Autowired
    ArthasProperties arthasProperties;

    /**
     * 配置HTTP安全策略
     * <p>
     * 该方法定义了HTTP请求的安全规则，包括访问控制和认证方式。
     * 主要配置内容：
     * 1. Actuator端点需要认证才能访问
     * 2. 其他所有请求允许匿名访问
     * 3. 启用表单登录
     * 4. 可选：禁用iframe的X-Frame-Options响应头，允许在iframe中嵌入页面
     * </p>
     *
     * @param httpSecurity HTTP安全配置对象，用于构建安全规则
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        // 配置请求授权规则
        // 1. 对于Actuator端点请求，需要认证
        // 2. 对于其他所有请求，允许所有人访问
        // 3. 启用表单登录作为认证方式
        httpSecurity.authorizeRequests().requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated().anyRequest()
        .permitAll().and().formLogin();

        // allow iframe
        // 如果配置启用了iframe支持，则禁用X-Frame-Options响应头
        // 这允许应用程序在iframe中被其他页面嵌入，但可能会带来安全风险（点击劫持攻击）
        if (arthasProperties.isEnableIframeSupport()) {
            // 禁用frameOptions，移除X-Frame-Options响应头
            httpSecurity.headers().frameOptions().disable();
        }
    }
}
