package com.alibaba.arthas.channel.server.conf;

import java.util.concurrent.ScheduledExecutorService;

/**
 * ScheduledExecutorService config for channel server, avoiding bean injecting conflicts
 * @author gongdewei 2020/9/8
 */
public class ScheduledExecutorConfig {

    private ScheduledExecutorService executorService;

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }
}
