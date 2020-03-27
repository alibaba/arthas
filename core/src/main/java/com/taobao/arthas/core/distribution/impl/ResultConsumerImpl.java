package com.taobao.arthas.core.distribution.impl;

import com.taobao.arthas.core.command.result.ExecResult;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author gongdewei 2020/3/27
 */
public class ResultConsumerImpl implements ResultConsumer {
    private static final Logger logger = LogUtil.getArthasLogger();
    private BlockingQueue<ExecResult> resultQueue = new ArrayBlockingQueue<ExecResult>(500);
    private long lastAccessTime;
    private volatile boolean polling;
    private ReentrantLock lock = new ReentrantLock();
    private int resultSizeLimit = 32;
    private long pollTimeLimit = 10*1000;
    private String consumerId;

    public ResultConsumerImpl() {
        lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public void appendResult(ExecResult result) {
        while (!resultQueue.offer(result)) {
            ExecResult discardResult = resultQueue.poll();
        }
    }

    @Override
    public List<ExecResult> pollResults() {
        try {
            long accessTime = System.currentTimeMillis();
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                polling = true;
                long firstResultTime = 0;
                // sending delay: time elapsed after firstResultTime
                long sendingDelay = 0;
                // waiting time: time elapsed after access
                long waitingTime = 0;
                List<ExecResult> sendingResults = new ArrayList<ExecResult>(resultSizeLimit);

                while (sendingResults.size() < resultSizeLimit
                        && sendingDelay < 200
                        && waitingTime < pollTimeLimit) {
                    ExecResult aResult = resultQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (aResult != null) {
                        sendingResults.add(aResult);
                        //是否为第一次获取到数据
                        if (firstResultTime == 0){
                            firstResultTime = System.currentTimeMillis();
                        }
                    } else {
                        if (firstResultTime > 0){
                            //获取到部分数据后，队列已经取完，计算发送延时时间
                            sendingDelay = System.currentTimeMillis() - firstResultTime;
                        }
                        //计算总共等待时间，长轮询最大等待时间
                        waitingTime = System.currentTimeMillis() - accessTime;
                    }
                }

                //resultQueue.drainTo(sendingResults, resultSizeLimit-sendingResults.size());
                return sendingResults;
            }
        } catch (InterruptedException e) {
            //e.printStackTrace();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                polling = false;
                lastAccessTime = System.currentTimeMillis();
                lock.unlock();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public boolean isPolling() {
        return polling;
    }

    public int getResultSizeLimit() {
        return resultSizeLimit;
    }

    public void setResultSizeLimit(int resultSizeLimit) {
        this.resultSizeLimit = resultSizeLimit;
    }

    @Override
    public String getConsumerId() {
        return consumerId;
    }

    @Override
    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }
}
