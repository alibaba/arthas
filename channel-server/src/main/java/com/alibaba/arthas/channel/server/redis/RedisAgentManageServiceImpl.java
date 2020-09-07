package com.alibaba.arthas.channel.server.redis;

import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author gongdewei 2020/8/10
 */
public class RedisAgentManageServiceImpl implements AgentManageService {

    private String prefix = "arthas:channel:agents:";

    private static final Logger logger = LoggerFactory.getLogger(RedisAgentManageServiceImpl.class);

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<List<AgentVO>> listAgents() {
        ScanOptions scanOptions = ScanOptions.scanOptions().match(prefix + "*").count(1000).build();
        return redisTemplate.scan(scanOptions)
                .collectList()
                .flatMap((Function<List<String>, Mono<List<String>>>) keys -> redisTemplate.opsForValue().multiGet(keys))
                .flatMap((Function<List<String>, Mono<List<AgentVO>>>) jsons -> {
                    List<AgentVO> agentList = new ArrayList<AgentVO>();
                    for (String json : jsons) {
                        AgentVO agentVO = null;
                        try {
                            agentVO = JSON.parseObject(json, AgentVO.class);
                            agentList.add(agentVO);
                        } catch (Exception e) {
                            logger.error("parse agent data failure, agent json: {}", json, e);
                        }
                    }
                    return Mono.just(agentList);
                });
    }

    @Override
    public Mono<Optional<AgentVO>> findAgentById(String agentId) {
        return redisTemplate.opsForValue()
                .get(prefix + agentId)
                .map(json -> Optional.of(JSON.parseObject(json, AgentVO.class)))
                .switchIfEmpty(Mono.just(Optional.empty()));
    }

    @Override
    public void addAgent(AgentVO agentVO) {
        if (StringUtils.isBlank(agentVO.getAgentId())) {
            throw new IllegalArgumentException("agent is empty");
        }
        String json = JSON.toJSONString(agentVO);
        redisTemplate.opsForValue().set(prefix+agentVO.getAgentId(), json).subscribe();
    }

    @Override
    public void updateAgent(AgentVO agentVO) {
        this.addAgent(agentVO);
    }

    @Override
    public void removeAgentById(String agentId) {
        redisTemplate.delete(prefix+agentId).subscribe();
    }

}
