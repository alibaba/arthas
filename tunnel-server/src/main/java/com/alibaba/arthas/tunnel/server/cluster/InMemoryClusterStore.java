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
 * 基于内存的集群存储实现
 * <p>
 * 该类使用 Spring Cache 和 Caffeine 作为底层存储，实现 Tunnel 集群的数据存储功能。
 * 主要用于存储和管理连接到 Tunnel Server 的 Agent 信息。
 * </p>
 * <p>
 * 存储的数据结构：
 * - Key: Agent ID（格式为 appName_agentUniqueId）
 * - Value: AgentClusterInfo 对象，包含 Agent 的详细信息
 * </p>
 * <p>
 * 该实现适用于单机部署场景，集群环境下需要使用 Redis 等分布式存储。
 * </p>
 *
 * @author hengyunabc 2020-12-02
 *
 */
public class InMemoryClusterStore implements TunnelClusterStore {
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(InMemoryClusterStore.class);

    /**
     * Spring Cache 实例
     * 实际使用的是 CaffeineCache，提供高性能的内存缓存功能
     */
    private Cache cache;

    /**
     * 根据 Agent ID 查找 Agent 信息
     *
     * @param agentId Agent 的唯一标识
     * @return Agent 集群信息，如果不存在则返回 null
     */
    @Override
    public AgentClusterInfo findAgent(String agentId) {
        // 从缓存中获取 Agent 信息
        // ValueWrapper 是 Spring Cache 的包装类，用于处理缓存值的空值情况
        ValueWrapper valueWrapper = cache.get(agentId);
        if (valueWrapper == null) {
            // 缓存中不存在该 Agent，返回 null
            return null;
        }

        // 从包装器中提取实际的 Agent 信息对象
        AgentClusterInfo info = (AgentClusterInfo) valueWrapper.get();
        return info;
    }

    /**
     * 从集群存储中移除指定的 Agent
     * <p>
     * 当 Agent 断开连接时，需要从存储中移除其信息。
     * </p>
     *
     * @param agentId 要移除的 Agent 的唯一标识
     */
    @Override
    public void removeAgent(String agentId) {
        // 从缓存中驱逐指定的 Agent
        cache.evict(agentId);
    }

    /**
     * 向集群存储中添加一个新的 Agent
     * <p>
     * 当有新的 Agent 连接到 Tunnel Server 时，调用此方法存储其信息。
     * 参数中的 timeout 和 timeUnit 在当前实现中未使用，
     * 实际的过期时间由 Caffeine Cache 的配置决定。
     * </p>
     *
     * @param agentId Agent 的唯一标识
     * @param info Agent 的集群信息
     * @param timeout 超时时间（当前实现未使用）
     * @param timeUnit 时间单位（当前实现未使用）
     */
    @Override
    public void addAgent(String agentId, AgentClusterInfo info, long timeout, TimeUnit timeUnit) {
        // 将 Agent 信息存入缓存
        // Caffeine Cache 会根据配置自动管理过期
        cache.put(agentId, info);
    }

    /**
     * 获取集群中所有 Agent 的 ID 集合
     * <p>
     * 该方法返回当前存储中所有 Agent 的 ID，用于统计和遍历。
     * </p>
     *
     * @return 所有 Agent ID 的集合
     */
    @Override
    public Collection<String> allAgentIds() {
        // 将 Spring Cache 强制转换为 CaffeineCache
        CaffeineCache caffeineCache = (CaffeineCache) cache;

        // 获取底层的 Caffeine Cache 原生实例
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

        // 返回所有键（Agent ID）的集合
        return (Collection<String>) (Collection<?>) nativeCache.asMap().keySet();
    }

    /**
     * 根据应用名称获取该应用下所有 Agent 的信息
     * <p>
     * Agent ID 的格式为：appName_agentUniqueId
     * 该方法会遍历所有 Agent，筛选出指定应用的 Agent 信息。
     * </p>
     *
     * @param appName 应用名称
     * @return Agent ID 到 Agent 信息的映射表
     */
    @Override
    public Map<String, AgentClusterInfo> agentInfo(String appName) {
        // 将 Spring Cache 强制转换为 CaffeineCache
        CaffeineCache caffeineCache = (CaffeineCache) cache;

        // 获取底层的 Caffeine Cache 原生实例
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();

        // 获取底层的并发 Map，用于遍历
        ConcurrentMap<String, AgentClusterInfo> map = (ConcurrentMap<String, AgentClusterInfo>) (ConcurrentMap<?, ?>) nativeCache
                .asMap();

        // 创建结果 Map，用于存储匹配的 Agent 信息
        Map<String, AgentClusterInfo> result = new HashMap<String, AgentClusterInfo>();

        // 构造应用名称前缀，用于匹配 Agent ID
        // Agent ID 格式为：appName_agentUniqueId
        String prefix = appName + "_";

        // 遍历所有 Agent，筛选出属于指定应用的 Agent
        for (Entry<String, AgentClusterInfo> entry : map.entrySet()) {
            String agentId = entry.getKey();
            // 检查 Agent ID 是否以应用名称前缀开头
            if (agentId.startsWith(prefix)) {
                // 如果匹配，将该 Agent 信息添加到结果中
                result.put(agentId, entry.getValue());
            }
        }

        // 返回筛选结果
        return result;

    }

    /**
     * 获取缓存实例
     * <p>
     * 主要用于依赖注入和测试。
     * </p>
     *
     * @return Spring Cache 实例
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * 设置缓存实例
     * <p>
     * 主要用于依赖注入，由 Spring 容器自动调用。
     * </p>
     *
     * @param cache Spring Cache 实例
     */
    public void setCache(Cache cache) {
        this.cache = cache;
    }

}
