package com.alibaba.arthas.channel.server.configuration;

import com.alibaba.arthas.channel.server.grpc.ArthasServiceGrpcImpl;
import com.alibaba.arthas.channel.server.grpc.ChannelServer;
import com.alibaba.arthas.channel.server.message.MessageExchangeService;
import com.alibaba.arthas.channel.server.message.impl.MessageExchangeServiceImpl;
import com.alibaba.arthas.channel.server.redis.RedisAgentManageServiceImpl;
import com.alibaba.arthas.channel.server.redis.RedisMessageExchangeServiceImpl;
import com.alibaba.arthas.channel.server.service.AgentBizSerivce;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import com.alibaba.arthas.channel.server.service.ApiActionDelegateService;
import com.alibaba.arthas.channel.server.service.impl.AgentBizServiceImpl;
import com.alibaba.arthas.channel.server.service.impl.AgentManageServiceImpl;
import com.alibaba.arthas.channel.server.service.impl.ApiActionDelegateServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * @author gongdewei 2020/8/14
 */
@Configuration
public class ChannelServerConfiguration {

    @Bean
    public ScheduledExecutorService executorService() {
        // 设置较大的corePoolSize，避免长时间运行的task阻塞调度队列 (https://developer.aliyun.com/article/5897 "1.2 线程数量控制")
        int corePoolSize = 20;
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(corePoolSize, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r, "Arthas-channel-server-execute");
                t.setDaemon(true);
                return t;
            }
        });

        //ScheduledThreadPoolExecutor为无界队列，设置MaximumPoolSize无效
//        if (executorService instanceof ThreadPoolExecutor) {
//            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;
//            threadPoolExecutor.setMaximumPoolSize(50);
//        }
        return executorService;
    }

    @Bean
    public ApiActionDelegateService apiActionDelegateService() {
        return new ApiActionDelegateServiceImpl();
    }

    @Bean
    public AgentBizSerivce agentBizSerivce() {
        return new AgentBizServiceImpl();
    }

    @Bean
    public ArthasServiceGrpcImpl arthasServiceGrpc() {
        return new ArthasServiceGrpcImpl();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(value = "channel.server.backend.enabled", havingValue = "true", matchIfMissing = false)
    public ChannelServer channelServer(@Value("${channel.server.backend.port}") int port) {
        ChannelServer channelServer = new ChannelServer();
        channelServer.setPort(port);
        return channelServer;
    }


    @Profile("memory")
    @Configuration
    static class StandaloneConfiguration {

        @Bean
        public AgentManageService agentManageService() {
            return new AgentManageServiceImpl();
        }

        @Bean
        public MessageExchangeService messageExchangeService() {
            return new MessageExchangeServiceImpl();
        }
    }


    @Profile("redis")
    @Configuration
    static class RedisConfiguration {
        @Bean
        public AgentManageService agentManageService() {
            return new RedisAgentManageServiceImpl();
        }

        @Bean
        public MessageExchangeService messageExchangeService() {
            return new RedisMessageExchangeServiceImpl();
        }

        @Bean
        public RedisTemplate<String, byte[]> redisTemplate(RedisConnectionFactory redisConnectionFactory)
                throws UnknownHostException {
            RedisTemplate<String, byte[]> template = new RedisTemplate<String, byte[]>();
            template.setKeySerializer(RedisSerializer.string());
            template.setValueSerializer(RedisSerializer.byteArray());
            template.setHashKeySerializer(RedisSerializer.string());
            template.setHashValueSerializer(RedisSerializer.byteArray());
            template.setConnectionFactory(redisConnectionFactory);
            return template;
        }

        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory)
                throws UnknownHostException {
            StringRedisTemplate template = new StringRedisTemplate();
            template.setConnectionFactory(redisConnectionFactory);
            return template;
        }
    }


}
