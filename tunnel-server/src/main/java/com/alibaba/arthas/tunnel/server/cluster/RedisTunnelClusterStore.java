package com.alibaba.arthas.tunnel.server.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author hengyunabc 2020-10-27
 *
 */
public class RedisTunnelClusterStore implements TunnelClusterStore {
    private final static Logger logger = LoggerFactory.getLogger(RedisTunnelClusterStore.class);
    // 定义jackson对象
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String prefix = "arthas-tunnel-agent-";

    private StringRedisTemplate redisTemplate;

    @Override
    public AgentClusterInfo findAgent(String agentId) {
        try {
            ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
            String infoStr = opsForValue.get(prefix + agentId);
            AgentClusterInfo info = MAPPER.readValue(infoStr, AgentClusterInfo.class);
            return info;
        } catch (Throwable e) {
            logger.error("try to read agentInfo error. agentId:{}", agentId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAgent(String agentId) {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        opsForValue.getOperations().delete(prefix + agentId);
    }

    @Override
    public void addAgent(String agentId, AgentClusterInfo info, long timeout, TimeUnit timeUnit) {
        try {
            ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
            String infoStr = MAPPER.writeValueAsString(info);
            opsForValue.set(prefix + agentId, infoStr, timeout, timeUnit);
        } catch (Throwable e) {
            logger.error("try to add agentInfo error. agentId:{}", agentId, e);
            throw new RuntimeException(e);
        }
    }

    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Collection<String> allAgentIds() {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        Set<String> result = new HashSet<String>();

        int length = prefix.length();
        for (String value : opsForValue.getOperations().keys(prefix + "*")) {
            result.add(value.substring(length));

        }
        return result;
    }

    @Override
    public Map<String, AgentClusterInfo> agentInfo(String appName) {
        try {

            ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

            Set<String> keys = new HashSet<String>();

            String prefixWithAppName = prefix + appName + "_";

            for (String value : opsForValue.getOperations().keys(prefixWithAppName + "*")) {
                keys.add(value);

            }

            List<String> values = opsForValue.getOperations().opsForValue().multiGet(keys);

            Map<String, AgentClusterInfo> result = new HashMap<>();

            Iterator<String> iterator = values.iterator();

            for (String key : keys) {
                String infoStr = iterator.next();
                AgentClusterInfo info = MAPPER.readValue(infoStr, AgentClusterInfo.class);
                String agentId = key.substring(prefix.length());
                result.put(agentId, info);
            }

            return result;
        } catch (Throwable e) {
            logger.error("try to query agentInfo error. appName:{}", appName, e);
            throw new RuntimeException(e);
        }
    }

}
