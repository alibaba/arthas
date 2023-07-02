package com.alibaba.arthas.tunnel.server.app.feature.web.security;

import com.alibaba.arthas.tunnel.server.app.feature.env.SecurityProperties;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.user.LoginUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 授权配置环境变更监听器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class SecurityEnvironmentChangeListener implements ApplicationListener<EnvironmentChangeEvent> {

    private final LoginUserDetailsService userDetailsService;

    private final TaskExecutor taskExecutor;

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        if (!checkSecurityUsersChanged(event)) {
            return;
        }

        taskExecutor.execute(userDetailsService::refreshUsers);
    }

    private boolean checkSecurityUsersChanged(EnvironmentChangeEvent event) {
        Set<String> keys = event.getKeys();
        for (String key : keys) {
            if (key.startsWith(SecurityProperties.PREFIX)) {
                return true;
            }
        }
        return false;
    }
}
