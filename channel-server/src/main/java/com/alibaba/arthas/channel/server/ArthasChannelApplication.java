package com.alibaba.arthas.channel.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ArthasChannelApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArthasChannelApplication.class, args);
    }


}
