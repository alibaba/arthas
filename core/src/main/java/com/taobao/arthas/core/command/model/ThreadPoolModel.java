package com.taobao.arthas.core.command.model;

import java.util.Collection;

/**
 * @author HJ
 * @date 2021-07-09
 **/
public class ThreadPoolModel extends ResultModel {

    private Collection<ThreadPoolVO> threadPools;

    @Override
    public String getType() {
        return "threadpool";
    }

    public Collection<ThreadPoolVO> getThreadPools() {
        return threadPools;
    }

    public void setThreadPools(Collection<ThreadPoolVO> threadPools) {
        this.threadPools = threadPools;
    }
}
