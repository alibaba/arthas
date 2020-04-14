package com.taobao.arthas.core.distribution.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.InputStatusVO;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.shell.session.Session;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class SharingResultDistributorImpl implements SharingResultDistributor {
    private static final Logger logger = LoggerFactory.getLogger(SharingResultDistributorImpl.class);

    private List<ResultConsumer> consumers = new CopyOnWriteArrayList<ResultConsumer>();
    private BlockingQueue<ResultModel> resultQueue = new ArrayBlockingQueue<ResultModel>(500);
    private final Session session;
    private Thread distributorThread;
    private volatile boolean running;
    private AtomicInteger consumerNumGenerator = new AtomicInteger(0);

    private SharingResultConsumerImpl sharingResultConsumer = new SharingResultConsumerImpl();

    public SharingResultDistributorImpl(Session session) {
        this.session = session;
        this.running = true;
        distributorThread = new Thread(new DistributorTask(), "ResultDistributor");
        distributorThread.start();
    }

    @Override
    public void appendResult(ResultModel result) {
        //要避免阻塞影响业务线程执行
        while (!resultQueue.offer(result)) {
            ResultModel discardResult = resultQueue.poll();
            //logger.warn("result queue is full: {}, discard early result: {}", resultQueue.size(), JSON.toJSONString(discardResult));
        }
    }

    private void distribute() {
        while (running) {
            try {
                ResultModel result = resultQueue.poll(100, TimeUnit.MILLISECONDS);
                if (result != null) {
                    sharingResultConsumer.appendResult(result);
                    for (ResultConsumer consumer : consumers) {
                        consumer.appendResult(result);
                    }
                }
            } catch (Throwable e) {
                logger.warn("distribute result failed: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void addConsumer(ResultConsumer consumer) {
        int consumerNo = consumerNumGenerator.incrementAndGet();
        String consumerId = UUID.randomUUID().toString().replaceAll("-", "") + "_" + consumerNo;
        consumer.setConsumerId(consumerId);

        //将队列中的消息复制给新的消费者
        sharingResultConsumer.copyTo(consumer);

        consumers.add(consumer);
    }

    @Override
    public void removeConsumer(ResultConsumer consumer) {
        consumers.remove(consumer);
    }

    @Override
    public ResultConsumer getConsumer(String consumerId) {
        for (ResultConsumer consumer : consumers) {
            if (consumer.getConsumerId().equals(consumerId)) {
                return consumer;
            }
        }
        return null;
    }

    private class DistributorTask implements Runnable {
        @Override
        public void run() {
            distribute();
        }
    }

    private class SharingResultConsumerImpl implements ResultConsumer {
        private BlockingQueue<ResultModel> resultQueue = new ArrayBlockingQueue<ResultModel>(500);
        private ReentrantLock queueLock = new ReentrantLock();
        private InputStatusVO lastInputStatus;

        @Override
        public void appendResult(ResultModel result) {
            queueLock.lock();
            try {
                //输入状态不入历史指令队列，复制时在最后发送
                if (result instanceof InputStatusVO) {
                    lastInputStatus = (InputStatusVO) result;
                    return;
                }
                while (!resultQueue.offer(result)) {
                    ResultModel discardResult = resultQueue.poll();
                }
            } finally {
                if (queueLock.isHeldByCurrentThread()) {
                    queueLock.unlock();
                }
            }
        }

        public void copyTo(ResultConsumer consumer) {
            //复制时加锁，避免消息顺序错乱，这里堵塞只影响分发线程，不会影响到业务线程
            queueLock.lock();
            try {
                for (ResultModel result : resultQueue) {
                    consumer.appendResult(result);
                }
                //发送输入状态
                if (lastInputStatus != null) {
                    consumer.appendResult(lastInputStatus);
                }
            } finally {
                if (queueLock.isHeldByCurrentThread()) {
                    queueLock.unlock();
                }
            }
        }

        @Override
        public List<ResultModel> pollResults() {
            return null;
        }

        @Override
        public long getLastAccessTime() {
            return 0;
        }

        @Override
        public boolean isPolling() {
            return false;
        }

        @Override
        public String getConsumerId() {
            return "shared-consumer";
        }

        @Override
        public void setConsumerId(String consumerId) {
        }
    }
}
