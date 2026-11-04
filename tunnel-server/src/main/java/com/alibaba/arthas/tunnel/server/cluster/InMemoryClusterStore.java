package com.alibaba.arthas.tunnel.server.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.caffeine.CaffeineCache;

import com.alibaba.arthas.tunnel.server.AgentClusterInfo;

/**
 * 
 * @author hengyunabc 2020-12-02
 *
 */
public class InMemoryClusterStore implements TunnelClusterStore {
    private final static Logger logger = LoggerFactory.getLogger(InMemoryClusterStore.class);

    private Cache cache;

    @Override
    public AgentClusterInfo findAgent(String agentId) {

        ValueWrapper valueWrapper = cache.get(agentId);
        if (valueWrapper == null) {
            return null;
        }

        AgentClusterInfo info = (AgentClusterInfo) valueWrapper.get();
        return info;
    }

    @Override
    public void removeAgent(String agentId) {
        cache.evict(agentId);
    }

    @Override
    public void addAgent(String agentId, AgentClusterInfo info, long timeout, TimeUnit timeUnit) {
        cache.put(agentId, info);
    }

    @Override
    public Collection<String> allAgentIds() {
        CaffeineCache caffeineCache = (CaffeineCache) cache;
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
        return (Collection<String>) (Collection<?>) nativeCache.asMap().keySet();
    }

    @Override
    public Map<String, AgentClusterInfo> agentInfo(String appName) {
        CaffeineCache caffeineCache = (CaffeineCache) cache;
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

        ConcurrentMap<String, AgentClusterInfo> map = (ConcurrentMap<String, AgentClusterInfo>) (ConcurrentMap<?, ?>) nativeCache
                .asMap();

        Map<String, AgentClusterInfo> result = new HashMap<String, AgentClusterInfo>();

        String prefix = appName + "_";
        for (Entry<String, AgentClusterInfo> entry : map.entrySet()) {
            String agentId = entry.getKey();
            if (agentId.startsWith(prefix)) {
                result.put(agentId, entry.getValue());
            }
        }

        return result;

    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

}
