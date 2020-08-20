package com.alibaba.arthas.channel.server.redis;

import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gongdewei 2020/8/10
 */
public class RedisAgentManageServiceImpl implements AgentManageService {

    private String prefix = "arthas:channel:agents:";

    private static final Logger logger = LoggerFactory.getLogger(RedisAgentManageServiceImpl.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public List<AgentVO> listAgents() {
        List<AgentVO> agentList = new ArrayList<AgentVO>();
        List<String> keys = new ArrayList<String>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(prefix + "*").count(1000).build();
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        try {
            Cursor<byte[]> cursor = connection.scan(scanOptions);
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                keys.add(key);
            }
        } finally {
            connection.close();
        }

        for (String key : keys) {
            String value = redisTemplate.opsForValue().get(key);
            AgentVO agentVO = null;
            try {
                agentVO = JSON.parseObject(value, AgentVO.class);
                agentList.add(agentVO);
            } catch (Exception e) {
                logger.error("parse agent data failure, agent key: {}", key, e);
            }
        }
        return agentList;
    }

    @Override
    public AgentVO findAgentById(String agentId) {
        String strValue = redisTemplate.opsForValue().get(prefix + agentId);
        AgentVO agentVO =  JSON.parseObject(strValue, AgentVO.class);
        return agentVO;
    }

    @Override
    public void addAgent(AgentVO agentVO) {
        if (StringUtils.isBlank(agentVO.getAgentId())) {
            throw new IllegalArgumentException("agent is empty");
        }
        String json = JSON.toJSONString(agentVO);
        redisTemplate.opsForValue().set(prefix+agentVO.getAgentId(), json);
    }

    @Override
    public void updateAgent(AgentVO agentVO) {
        this.addAgent(agentVO);
    }

    @Override
    public void removeAgentById(String agentId) {
        redisTemplate.delete(prefix+agentId);
    }

}
