package com.alibaba.arthas.channel.server.service;

import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.server.conf.ScheduledExecutorConfig;
import com.alibaba.arthas.channel.server.model.AgentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AgentCleaner {

    private static final Logger logger = LoggerFactory.getLogger(AgentCleaner.class);
    private int removingTimeout = 1800*1000;
    private int downTimeout = 30000;
    private int outOfServiceTimeout = 15000;
    private int cleanIntervalMills = 5000;

    @Autowired
    private AgentManageService agentManageService;
    private ScheduledExecutorConfig scheduledExecutorConfig;
    private ScheduledFuture<?> scheduledFuture;

    public AgentCleaner(ScheduledExecutorConfig scheduledExecutorConfig) {
        this.scheduledExecutorConfig = scheduledExecutorConfig;
    }

    public void start() {

        if (downTimeout < outOfServiceTimeout) {
            throw new IllegalArgumentException(" agent down timeout must not be less than out of service timeout");
        }

        if (removingTimeout > 0 && removingTimeout < downTimeout) {
            throw new IllegalArgumentException(" agent removing timeout must not be less than down timeout or -1");
        }

        scheduledFuture = scheduledExecutorConfig.getExecutorService().scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                doClean();
            }
        }, cleanIntervalMills, cleanIntervalMills, TimeUnit.MILLISECONDS);

        logger.info("Agent cleaner is started.");
    }

    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            logger.info("Agent cleaner is stopped.");
        }
    }

    public void doClean() {
        long now = System.currentTimeMillis();
        Mono<List<AgentVO>> agentsMono = agentManageService.listAgents();
        agentsMono.doOnSuccess(agents -> {
            for (AgentVO agent : agents) {
                long heartbeatDelay = now - agent.getHeartbeatTime();
                if (heartbeatDelay > removingTimeout && removingTimeout > 0) {
                    logger.info("clean up dead agent: {}, heartbeat delay: {}", agent.getAgentId(), heartbeatDelay);
                    agentManageService.removeAgentById(agent.getAgentId());
                } else if (heartbeatDelay > downTimeout) {
                    if (!AgentStatus.DOWN.name().equals(agent.getAgentStatus())) {
                        logger.info("Mark agent status as DOWN, agentId: {}, heartbeat delay: {}", agent.getAgentId(), heartbeatDelay);
                        agent.setAgentStatus(AgentStatus.DOWN.name());
                        agentManageService.updateAgent(agent);
                    }
                } else if (heartbeatDelay > outOfServiceTimeout) {
                    if (!AgentStatus.OUT_OF_SERVICE.name().equals(agent.getAgentStatus())) {
                        logger.info("Mark agent status as OUT_OF_SERVICE, agentId: {}, heartbeat delay: {}", agent.getAgentId(), heartbeatDelay);
                        agent.setAgentStatus(AgentStatus.OUT_OF_SERVICE.name());
                        agentManageService.updateAgent(agent);
                    }
                }
            }
        }).doOnError(throwable -> {
            logger.error("clean agent error", throwable);
        }).subscribe();
    }

    public int getRemovingTimeout() {
        return removingTimeout;
    }

    public void setRemovingTimeout(int removingTimeout) {
        this.removingTimeout = removingTimeout;
    }

    public int getDownTimeout() {
        return downTimeout;
    }

    public void setDownTimeout(int downTimeout) {
        if (downTimeout < 5000) {
            throw new IllegalArgumentException("agent down timeout must be not be less than 5000 mills");
        }
        this.downTimeout = downTimeout;
    }

    public int getOutOfServiceTimeout() {
        return outOfServiceTimeout;
    }

    public void setOutOfServiceTimeout(int outOfServiceTimeout) {
        if (outOfServiceTimeout < 5000) {
            throw new IllegalArgumentException("agent out of service timeout must not be less than 5000 mills");
        }
        this.outOfServiceTimeout = outOfServiceTimeout;
    }

    public int getCleanIntervalMills() {
        return cleanIntervalMills;
    }

    public void setCleanIntervalMills(int cleanIntervalMills) {
        if (cleanIntervalMills < 2000) {
            throw new IllegalArgumentException("agent clean interval must not be less than 2000 mills");
        }
        this.cleanIntervalMills = cleanIntervalMills;
    }
}
