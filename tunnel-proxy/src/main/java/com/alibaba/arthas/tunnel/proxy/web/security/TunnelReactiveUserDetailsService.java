package com.alibaba.arthas.tunnel.proxy.web.security;

import com.alibaba.arthas.tunnel.proxy.config.env.TunnelProxyProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自定义 ReactiveUserDetailsService
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class TunnelReactiveUserDetailsService implements ReactiveUserDetailsService {

    private final Map<String, UserDetails> users = new ConcurrentHashMap<>();

    private final TunnelProxyProperties tunnelProxyProperties;

    private final SecurityUserHelper securityUserHelper;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        String key = getKey(username);
        UserDetails result = this.users.get(key);
        return (result != null) ? Mono.just(User.withUserDetails(result).build()) : Mono.empty();
    }

    @PostConstruct
    public void init() {
        refreshUsers();
    }

    public void refreshUsers() {
        Set<SecurityProperties.User> users = tunnelProxyProperties.getUsers();
        if (CollectionUtils.isEmpty(users)) {
            return;
        }

        Set<UserDetails> userDetails = users.stream().map(securityUserHelper::getUserDetails).collect(Collectors.toSet());
        resetUsers(userDetails);
    }

    private void resetUsers(@NonNull Collection<UserDetails> users) {
        this.users.clear();

        for (UserDetails user : users) {
            String password = user.getPassword();
            if (StringUtils.isBlank(password)) {
                log.warn("Update users found that username `{}` not set password", user.getUsername());
                continue;
            }
            log.info("Add user, username = {}, password = {}", user.getUsername(), password);
            this.users.put(getKey(user.getUsername()), user);
        }
    }

    private String getKey(String username) {
        return username.toLowerCase();
    }
}
