package com.alibaba.arthas.tunnel.server.app.web;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.alibaba.arthas.tunnel.server.app.configuration.ArthasProperties;
import com.alibaba.arthas.tunnel.server.cluster.TunnelClusterStore;

/**
 * 
 * @author hengyunabc 2020-11-03
 *
 */
@Controller
public class DetailAPIController {

    private final static Logger logger = LoggerFactory.getLogger(DetailAPIController.class);

    @Autowired
    ArthasProperties arthasProperties;

    @Autowired(required = false)
    private TunnelClusterStore tunnelClusterStore;

    @RequestMapping("/api/tunnelApps")
    @ResponseBody
    public Set<String> tunnelApps(HttpServletRequest request, Model model) {
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }

        Set<String> result = new HashSet<String>();

        if (tunnelClusterStore != null) {
            Collection<String> agentIds = tunnelClusterStore.allAgentIds();

            for (String id : agentIds) {
                String appName = findAppNameFromAgentId(id);
                if (appName != null) {
                    result.add(appName);
                } else {
                    logger.warn("illegal agentId: " + id);
                }
            }

        }

        return result;
    }

    @RequestMapping("/api/tunnelAgentInfo")
    @ResponseBody
    public Map<String, AgentClusterInfo> tunnelAgentIds(@RequestParam(value = "app", required = true) String appName,
            HttpServletRequest request, Model model) {
        if (!arthasProperties.isEnableDetailPages()) {
            throw new IllegalAccessError("not allow");
        }

        if (tunnelClusterStore != null) {
            Map<String, AgentClusterInfo> agentInfos = tunnelClusterStore.agentInfo(appName);

            return agentInfos;
        }

        return Collections.emptyMap();
    }

    private static String findAppNameFromAgentId(String id) {
        int index = id.indexOf('_');
        if (index < 0 || index >= id.length()) {
            return null;
        }

        return id.substring(0, index);
    }
}
