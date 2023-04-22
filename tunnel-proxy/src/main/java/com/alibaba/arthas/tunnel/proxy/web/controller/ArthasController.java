package com.alibaba.arthas.tunnel.proxy.web.controller;

import com.alibaba.arthas.tunnel.proxy.config.env.TunnelProperties;
import com.alibaba.arthas.tunnel.proxy.domain.ArthasAgent;
import com.alibaba.arthas.tunnel.proxy.domain.ArthasAgentGroup;
import com.alibaba.arthas.tunnel.server.AgentInfo;
import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arthas 接口
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 1.0.0
 */
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
@RestController
public class ArthasController {

    private final TunnelServer tunnelServer;

    private final ArthasProperties arthasProperties;

    private final TunnelProperties tunnelProperties;

    private final ReactiveUserDetailsService userDetailsService;

    @GetMapping("/arthas/html/title")
    public String getHtmlTitle() {
        return tunnelProperties.getHtmlTitle();
    }

    @GetMapping("/arthas/server")
    public ArthasProperties.Server getTunnelServerInfo() {
        return arthasProperties.getServer();
    }

    @GetMapping(value = "/arthas/access/agents")
    public List<ArthasAgentGroup> getAgents(Principal principal) {
        Set<String> roles = getCurrentUserRole(principal.getName());
        if (roles.isEmpty()) {
            return Collections.emptyList();
        }
        boolean isSuperUser = isSuperAdmin(roles);

        Map<String, AgentInfo> agentInfoMap = tunnelServer.getAgentInfoMap();
        Map<String, List<ArthasAgent>> map = new HashMap<>(16);
        agentInfoMap.forEach((k, v) -> {
            String[] split = k.split(tunnelProperties.getAgentSpilt(), 2);
            String appName = split[0];
            if (split.length > 1) {
                if (isSuperUser || accessApp(roles, appName)) {
                    List<ArthasAgent> agents = map.computeIfAbsent(appName, k1 -> new ArrayList<>());
                    ArthasAgent agent = new ArthasAgent();
                    agent.setId(split[1]);
                    agent.setInfo(v);
                    agents.add(agent);
                }
            } else {
                log.warn("Ignore app `{}` due to might not set agent id", appName);
            }
        });
        List<ArthasAgentGroup> groups = new ArrayList<>();
        map.forEach((k, v) -> {
            ArthasAgentGroup group = new ArthasAgentGroup();
            group.setAgents(v);
            group.setService(k);
            groups.add(group);
        });
        return groups;
    }

    private Set<String> getCurrentUserRole(String userName) {
        if (Strings.isNullOrEmpty(userName)) {
            return Collections.emptySet();
        }

        UserDetails u = userDetailsService.findByUsername(userName).block();
        if (u == null) {
            return Collections.emptySet();
        }
        return u.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private boolean isSuperAdmin(Set<String> roles) {
        return accessApp(roles, "*");
    }

    private boolean accessApp(Set<String> roles, String appName) {
        for (String role : roles) {
            if (role.endsWith(appName)) {
                return true;
            }
        }
        return false;
    }
}
