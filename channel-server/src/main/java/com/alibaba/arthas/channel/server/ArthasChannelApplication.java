package com.alibaba.arthas.channel.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;

/**
 * @author gongdewei 2020/8/10
 */
@SpringBootApplication(exclude = RedisAutoConfiguration.class)
public class ArthasChannelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArthasChannelApplication.class, args);
    }

}
