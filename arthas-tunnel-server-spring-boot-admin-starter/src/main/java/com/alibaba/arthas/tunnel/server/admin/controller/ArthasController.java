package com.alibaba.arthas.tunnel.server.admin.controller;

import com.alibaba.arthas.tunnel.server.AgentInfo;
import com.alibaba.arthas.tunnel.server.TunnelServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.services.ApplicationRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.arthas.tunnel.server.admin.arthas.ArthasAgent;
import java.util.*;

/**
 *
 * @author naah 2021-04-16
 *
 */
@RequestMapping("/api/arthas")
@RestController
public class ArthasController {

    private static final Logger logger = LoggerFactory.getLogger(ArthasController.class);
    public static final HashMap<String, String> NULL_METADATA = new HashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private TunnelServer tunnelServer;

    @Autowired
    private ApplicationRegistry applicationRegistry;


    @RequestMapping(value = "/clients", method = RequestMethod.GET)
    public MultiValueMap<String, ArthasAgent> getClients() {
        MultiValueMap<String, ArthasAgent> result = new LinkedMultiValueMap<>();

//        ipAndPort : ArthasAgent
        Map<String, ArthasAgent> agentMap = new HashMap<>();
        List<ArthasAgent> chaosAgentList = new LinkedList<>();

        tunnelServer.getAgentInfoMap().entrySet().stream().map(entry -> {
            AgentInfo value = entry.getValue();
            HashMap<String, String> metadata = null;
            if (StringUtils.isNotBlank(value.getMetadata())) {
                try {
                    metadata = objectMapper.readValue(value.getMetadata(), new TypeReference<HashMap<String, String>>() {
                    });
                } catch (Exception e) {
                    metadata = NULL_METADATA;
                    logger.error("tunnel metadata isn't a json, metadata:{}", value.getMetadata(), e);
                }
            } else {
                metadata = NULL_METADATA;
            }

            return new ArthasAgent(value.getAppName(),
                    entry.getKey(),
                    metadata.get("serviceIp"),
                    metadata.get("servicePort"));
        }).forEach(agent -> {
            if (StringUtils.isNotBlank(agent.getServiceIp()) && StringUtils.isNotBlank(agent.getServicePort())) {
                agentMap.put(String.format("%s:%s", agent.getServiceIp(), agent.getServicePort()), agent);
            } else {
                chaosAgentList.add(agent);
            }
        });


//        IpAndPort : appName
        Map<String, String> adminIpAndPortAppNameMap = new HashMap<>();

//        IpAndPort : Instance
        Map<String, Instance> adminIpAndPortInstanceMap = new HashMap<>();

        Set<String> appNameSet = new HashSet<>();

        applicationRegistry.getApplications().toStream().forEach(app -> app.getInstances().forEach(ins -> {
            String appName = app.getName();
            appNameSet.add(appName);
            String ipAndPort = ins.getRegistration().getServiceUrl().replace("http://", "");
            ArthasAgent arthasAgent = agentMap.get(ipAndPort);
            if (Objects.nonNull(arthasAgent)) {
                arthasAgent.setServiceId(ins.getId().getValue());
                result.computeIfAbsent(appName, k -> new LinkedList<>()).add(arthasAgent);
                agentMap.remove(ipAndPort);
            } else {
                adminIpAndPortAppNameMap.put(ipAndPort, appName);
                adminIpAndPortInstanceMap.put(ipAndPort, ins);
            }
        }));

        agentMap.entrySet().forEach(entry -> {
            String ipAndPort = entry.getKey();
            ArthasAgent agent = entry.getValue();
            String appName = adminIpAndPortAppNameMap.get(ipAndPort);
            if (StringUtils.isNotBlank(appName)) {
                agent.setServiceId(adminIpAndPortInstanceMap.get(ipAndPort).getId().getValue());
                result.computeIfAbsent(appName, k -> new LinkedList<>()).add(agent);
            } else {
                washRemainAgent(result, appNameSet, agent);
            }
        });

        chaosAgentList.forEach(agent -> washRemainAgent(result, appNameSet, agent));

        return result;
    }

    private void washRemainAgent(MultiValueMap<String, ArthasAgent> result, Set<String> appNameSet, ArthasAgent agent) {
        String agentAppName = Optional.ofNullable(agent.getAppName()).orElse(agent.getAgentId().split("_")[0]);
        if (StringUtils.isNotBlank(agentAppName) && appNameSet.contains(agentAppName)) {
            result.computeIfAbsent(agentAppName, k -> new LinkedList<>()).add(agent);
        }else{
            result.computeIfAbsent("None", k -> new LinkedList<>()).add(agent);
        }
    }
}