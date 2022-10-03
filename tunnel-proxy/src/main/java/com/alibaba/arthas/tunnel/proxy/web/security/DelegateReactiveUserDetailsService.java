package com.alibaba.arthas.tunnel.proxy.web.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 代理 ReactiveUserDetailsService
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@RequiredArgsConstructor
@Slf4j
public class DelegateReactiveUserDetailsService implements ReactiveUserDetailsService {

    private final List<ReactiveUserDetailsService> delegates;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        for (ReactiveUserDetailsService delegate : delegates) {
            UserDetails exists = delegate.findByUsername(username).block();
            if (exists != null) {
                return Mono.just(exists);
            }
        }
        return Mono.empty();
    }
}
