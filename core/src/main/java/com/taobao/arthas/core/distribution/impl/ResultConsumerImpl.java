package com.taobao.arthas.core.distribution.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.DistributorOptions;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.ResultConsumerHelper;

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
    private static final Logger logger = LoggerFactory.getLogger(ResultConsumerImpl.class);
    private BlockingQueue<ResultModel> resultQueue;
    private volatile long lastAccessTime;
    private volatile boolean polling;
    private ReentrantLock lock = new ReentrantLock();
    private int resultBatchSizeLimit = 20;
    private int resultQueueSize = DistributorOptions.resultQueueSize;
    private long pollTimeLimit = 2 * 1000;
    private String consumerId;
    private boolean closed;
    private long sendingItemCount;

    public ResultConsumerImpl() {
        lastAccessTime = System.currentTimeMillis();
        resultQueue = new ArrayBlockingQueue<ResultModel>(resultQueueSize);
    }

    @Override
    public boolean appendResult(ResultModel result) {
        //可能某些Consumer已经断开，不会再读取，这里不能堵塞！
        boolean discard = false;
        while (!resultQueue.offer(result)) {
            ResultModel discardResult = resultQueue.poll();
            discard = true;
        }
        return !discard;
    }

    @Override
    public List<ResultModel> pollResults() {
        try {
            lastAccessTime = System.currentTimeMillis();
            long accessTime = lastAccessTime;
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                polling = true;
                sendingItemCount = 0;
                long firstResultTime = 0;
                // sending delay: time elapsed after firstResultTime
                long sendingDelay = 0;
                // waiting time: time elapsed after access
                long waitingTime = 0;
                List<ResultModel> sendingResults = new ArrayList<ResultModel>(resultBatchSizeLimit);

                while (!closed
                        &&sendingResults.size() < resultBatchSizeLimit
                        && sendingDelay < 100
                        && waitingTime < pollTimeLimit) {
                    ResultModel aResult = resultQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (aResult != null) {
                        sendingResults.add(aResult);
                        //是否为第一次获取到数据
                        if (firstResultTime == 0) {
                            firstResultTime = System.currentTimeMillis();
                        }
                        //判断是否需要立即发送出去
                        if (shouldFlush(sendingResults, aResult)) {
                            break;
                        }
                    } else {
                        if (firstResultTime > 0) {
                            //获取到部分数据后，队列已经取完，计算发送延时时间
                            sendingDelay = System.currentTimeMillis() - firstResultTime;
                        }
                        //计算总共等待时间，长轮询最大等待时间
                        waitingTime = System.currentTimeMillis() - accessTime;
                    }
                }

                //resultQueue.drainTo(sendingResults, resultSizeLimit-sendingResults.size());
                if(logger.isDebugEnabled()) {
                    logger.debug("pollResults: {}, results: {}", sendingResults.size(), JSON.toJSONString(sendingResults));
                }
                return sendingResults;
            }
        } catch (InterruptedException e) {
            //e.printStackTrace();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lastAccessTime = System.currentTimeMillis();
                polling = false;
                lock.unlock();
            }
        }
        return Collections.emptyList();
    }

    /**
     * 估算对象数量及大小，判断是否需要立即发送出去
     * @param sendingResults
     * @param last
     * @return
     */
    private boolean shouldFlush(List<ResultModel> sendingResults, ResultModel last) {
        //TODO 引入一个估算模型，每个model自统计对象数量
        sendingItemCount += ResultConsumerHelper.getItemCount(last);
        return sendingItemCount >= 100;
    }

    @Override
    public boolean isHealthy() {

        return isPolling()
                || resultQueue.size() < resultQueueSize
                || System.currentTimeMillis() - lastAccessTime < 1000;
    }

    @Override
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    @Override
    public void close(){
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean isPolling() {
        return polling;
    }

    public int getResultBatchSizeLimit() {
        return resultBatchSizeLimit;
    }

    public void setResultBatchSizeLimit(int resultBatchSizeLimit) {
        this.resultBatchSizeLimit = resultBatchSizeLimit;
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
