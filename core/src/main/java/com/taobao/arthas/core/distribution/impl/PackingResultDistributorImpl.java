package com.taobao.arthas.core.distribution.impl;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.command.result.ExecResult;
import com.taobao.arthas.core.distribution.PackingResultDistributor;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PackingResultDistributorImpl implements PackingResultDistributor {
    private static final Logger logger = LogUtil.getArthasLogger();

    private BlockingQueue<ExecResult> resultQueue = new ArrayBlockingQueue<ExecResult>(500);
    private final Session session;

    public PackingResultDistributorImpl(Session session) {
        this.session = session;
    }

    @Override
    public void appendResult(ExecResult result) {
        if (!resultQueue.offer(result)){
            logger.warn("arthas", "result queue is full: {}, discard later result: {}", resultQueue.size(), JSON.toJSONString(result));
        }
    }

    @Override
    public List<ExecResult> getResults() {
        ArrayList<ExecResult> results = new ArrayList<ExecResult>(resultQueue.size());
        resultQueue.drainTo(results);
        return results;
    }

}
