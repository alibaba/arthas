package com.alibaba.arthas.tunnel.proxy.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 认证用户帮助类
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class SecurityUserHelper {

    private static final String NOOP_PASSWORD_PREFIX = "{noop}";

    private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");

    private final ObjectProvider<PasswordEncoder> passwordEncoder;

    public UserDetails getUserDetails(SecurityProperties.User user) {
        List<String> roles = user.getRoles();
        return User.withUsername(user.getName())
                .password(getOrDeducePassword(user, passwordEncoder.getIfAvailable()))
                .roles(StringUtils.toStringArray(roles)).build();
    }

    private String getOrDeducePassword(SecurityProperties.User user,
                                   PasswordEncoder encoder) {
        String password = user.getPassword();
        if (user.isPasswordGenerated()) {
            log.info("Using generated security password: {}", user.getPassword());
        }

        if (encoder != null || PASSWORD_ALGORITHM_PATTERN.matcher(password).matches()) {
            return password;
        }
        return NOOP_PASSWORD_PREFIX + password;
    }
}
