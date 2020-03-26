package com.taobao.arthas.core.distribution.impl;

import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.command.result.ExecResult;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class SharingResultDistributorImpl implements SharingResultDistributor {
    private static final Logger logger = LogUtil.getArthasLogger();

    private List<ResultConsumer> consumers = new CopyOnWriteArrayList<ResultConsumer>();
    private BlockingQueue<ExecResult> resultQueue = new ArrayBlockingQueue<ExecResult>(500);
    private final Session session;
    private Thread distributorThread;
    private volatile boolean running;

    public SharingResultDistributorImpl(Session session) {
        this.session = session;
        this.running = true;
        distributorThread = new Thread(new DistributorTask(), "ResultDistributor");
        distributorThread.start();
    }

    @Override
    public void appendResult(ExecResult result) {
        while (!resultQueue.offer(result)) {
            ExecResult discardResult = resultQueue.poll();
            //logger.warn("arthas", "result queue is full: {}, discard early result: {}", resultQueue.size(), JSON.toJSONString(discardResult));
        }
    }

    private void distribute() {
        while (running) {
            try {
                ExecResult result = resultQueue.poll(100, TimeUnit.MILLISECONDS);
                if (result != null) {
                    for (ResultConsumer consumer : consumers) {
                        consumer.appendResult(result);
                    }
                }
            } catch (Throwable e) {
                logger.warn("arthas", "distribute result failed", e);
            }
        }
    }

    @Override
    public void addConsumer(ResultConsumer consumer) {
        consumers.add(consumer);
    }

    @Override
    public void removeConsumer(ResultConsumer consumer) {
        consumers.remove(consumer);
    }

    private class DistributorTask implements Runnable {
        @Override
        public void run() {
            distribute();
        }
    }
}
