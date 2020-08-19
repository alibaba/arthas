package com.alibaba.arthas.channel.server;

import com.alibaba.arthas.channel.proto.AgentStatus;
import com.alibaba.arthas.channel.server.model.AgentVO;
import com.alibaba.arthas.channel.server.service.AgentManageService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@EnableScheduling
@SpringBootApplication(exclude = RedisAutoConfiguration.class)
public class ArthasChannelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArthasChannelApplication.class, args);
    }


}
