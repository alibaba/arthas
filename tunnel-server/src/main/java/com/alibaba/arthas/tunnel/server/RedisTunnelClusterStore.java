package com.alibaba.arthas.tunnel.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * 
 * @author hengyunabc 2020-10-27
 *
 */
public class RedisTunnelClusterStore implements TunnelClusterStore {

    private String prefix = "arthas-tunnel-agent-";

    private StringRedisTemplate redisTemplate;

    @Override
    public String findHost(String agentId) {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        return opsForValue.get(prefix + agentId);
    }

    @Override
    public void removeAgent(String agentId) {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        opsForValue.getOperations().delete(prefix + agentId);
    }

    @Override
    public void addHost(String agentId, String host, long timeout, TimeUnit timeUnit) {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        opsForValue.set(prefix + agentId, host, timeout, timeUnit);
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
    public Collection<Pair<String, String>> agentInfo(String appName) {
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        Set<String> keys = new HashSet<String>();

        String prefixWithAppName = prefix + appName + "|";
        
        for (String value : opsForValue.getOperations().keys(prefixWithAppName + "*")) {
            keys.add(value);

        }

        List<String> values = opsForValue.getOperations().opsForValue().multiGet(keys);

        Collection<Pair<String, String>> result = new HashSet<>();

        Iterator<String> iterator = values.iterator();

        for (String key : keys) {
            String host = iterator.next();
            String agentId = key.substring(prefix.length());
            result.add(Pair.of(agentId, host));
        }

        return result;
    }

}
