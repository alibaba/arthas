package com.alibaba.arthas.tunnel.server.app.feature.web.security.user;

import com.alibaba.arthas.tunnel.server.app.feature.env.SecurityProperties;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 自定义 UserDetailsService
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@RequiredArgsConstructor
@Slf4j
public class LoginUserDetailsService implements UserDetailsService {

    private final Map<String, UserDetails> users = new ConcurrentHashMap<>();

    private final SecurityProperties securityProperties;

    private final SecurityUserHelper securityUserHelper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String key = getKey(username);
        return this.users.get(key);
    }

    @PostConstruct
    public void init() {
        refreshUsers();
    }

    public void refreshUsers() {
        Set<org.springframework.boot.autoconfigure.security.SecurityProperties.User> users =
                securityProperties.getUsers();
        if (CollectionUtils.isEmpty(users)) {
            return;
        }

        Set<UserDetails> userDetails = users.stream()
                .map(securityUserHelper::getUserDetails)
                .collect(Collectors.toSet());
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
