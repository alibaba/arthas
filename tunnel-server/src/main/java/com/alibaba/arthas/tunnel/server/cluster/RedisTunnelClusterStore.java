package com.alibaba.arthas.tunnel.server.cluster;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
 * 基于Redis的隧道集群存储实现
 * 用于在集群环境下存储和管理Agent的连接信息
 *
 * @author hengyunabc 2020-10-27
 *
 */
public class RedisTunnelClusterStore implements TunnelClusterStore {
    // 日志记录器
    private final static Logger logger = LoggerFactory.getLogger(RedisTunnelClusterStore.class);
    // 定义Jackson对象，用于JSON序列化和反序列化
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Redis中存储Agent信息的前缀
    private String prefix = "arthas-tunnel-agent-";

    // Spring提供的Redis模板类，用于执行Redis操作
    private StringRedisTemplate redisTemplate;

    /**
     * 根据Agent ID查找Agent集群信息
     *
     * @param agentId Agent的唯一标识
     * @return Agent的集群信息对象
     * @throws RuntimeException 当查找失败或找不到Agent时抛出异常
     */
    @Override
    public AgentClusterInfo findAgent(String agentId) {
        try {
            // 获取Redis字符串值的操作对象
            ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
            // 从Redis中获取Agent信息的JSON字符串
            String infoStr = opsForValue.get(prefix + agentId);
            // 如果Redis中不存在该Agent信息，抛出异常
            if (infoStr == null) {
                throw new IllegalArgumentException("can not find info for agentId: " + agentId);
            }
            // 将JSON字符串反序列化为AgentClusterInfo对象
            AgentClusterInfo info = MAPPER.readValue(infoStr, AgentClusterInfo.class);
            return info;
        } catch (Throwable e) {
            logger.error("try to read agentInfo error. agentId:{}", agentId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 从Redis中移除指定Agent的信息
     *
     * @param agentId 要移除的Agent的唯一标识
     */
    @Override
    public void removeAgent(String agentId) {
        // 获取Redis字符串值的操作对象
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
        // 从Redis中删除对应的Agent信息
        opsForValue.getOperations().delete(prefix + agentId);
    }

    /**
     * 向Redis中添加Agent信息，并设置过期时间
     *
     * @param agentId Agent的唯一标识
     * @param info Agent的集群信息对象
     * @param timeout 过期时间数值
     * @param timeUnit 过期时间单位
     * @throws RuntimeException 当添加失败时抛出异常
     */
    @Override
    public void addAgent(String agentId, AgentClusterInfo info, long timeout, TimeUnit timeUnit) {
        try {
            // 获取Redis字符串值的操作对象
            ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();
            // 将AgentClusterInfo对象序列化为JSON字符串
            String infoStr = MAPPER.writeValueAsString(info);
            // 将JSON字符串存入Redis，并设置过期时间
            opsForValue.set(prefix + agentId, infoStr, timeout, timeUnit);
        } catch (Throwable e) {
            logger.error("try to add agentInfo error. agentId:{}", agentId, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取Redis模板对象
     *
     * @return Spring的StringRedisTemplate对象
     */
    public StringRedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    /**
     * 设置Redis模板对象
     *
     * @param redisTemplate Spring的StringRedisTemplate对象
     */
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取Redis中存储的所有Agent ID集合
     *
     * @return 所有Agent ID的集合
     */
    @Override
    public Collection<String> allAgentIds() {
        // 获取Redis字符串值的操作对象
        ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

        // 获取前缀的长度，用于后续截取agentId
        int length = prefix.length();
        // 从Redis中查询所有以该前缀开头的key
        final Set<String> redisValues = opsForValue.getOperations().keys(prefix + "*");
        if (redisValues != null) {
            // 创建结果列表，初始容量为Redis返回的key集合大小
            final ArrayList<String> result = new ArrayList<>(redisValues.size());
            // 遍历所有key，截取掉前缀，得到纯agentId
            for (String value : redisValues) {
                result.add(value.substring(length));
            }
            return result;
        } else {
            // 如果Redis返回null，记录错误日志并返回空列表
            logger.error("try to get allAgentIds error. redis returned null.");
            return Collections.emptyList();
        }
    }

    /**
     * 根据应用名称查询该应用下所有Agent的信息
     *
     * @param appName 应用名称
     * @return Map集合，key为agentId，value为对应的Agent集群信息
     * @throws RuntimeException 当查询失败时抛出异常
     */
    @Override
    public Map<String, AgentClusterInfo> agentInfo(String appName) {
        try {
            // 获取Redis字符串值的操作对象
            ValueOperations<String, String> opsForValue = redisTemplate.opsForValue();

            // 构建包含应用名称的前缀，用于查询特定应用的所有Agent
            // 格式为：arthas-tunnel-agent-{appName}_
            String prefixWithAppName = prefix + appName + "_";

            // 查询所有匹配前缀的key
            ArrayList<String> keys = new ArrayList<>(opsForValue.getOperations().keys(prefixWithAppName + "*"));

            // 批量获取所有key对应的值
            List<String> values = opsForValue.getOperations().opsForValue().multiGet(keys);

            // 创建结果Map
            Map<String, AgentClusterInfo> result = new HashMap<>();

            // 获取值的迭代器，用于遍历
            Iterator<String> iterator = values.iterator();

            // 遍历所有key，将key-value转换为agentId-AgentClusterInfo的映射
            for (String key : keys) {
                // 获取对应key的JSON字符串值
                String infoStr = iterator.next();
                // 将JSON字符串反序列化为AgentClusterInfo对象
                AgentClusterInfo info = MAPPER.readValue(infoStr, AgentClusterInfo.class);
                // 从key中提取出agentId（去掉前缀）
                String agentId = key.substring(prefix.length());
                // 将agentId和对应的AgentClusterInfo放入结果Map中
                result.put(agentId, info);
            }

            return result;
        } catch (Throwable e) {
            logger.error("try to query agentInfo error. appName:{}", appName, e);
            throw new RuntimeException(e);
        }
    }

}
