package com.taobao.arthas.core.distribution.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
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
 * 结果消费者实现类
 *
 * 负责接收、存储和提供命令执行结果给消费者
 * 使用阻塞队列来缓存结果，支持批量轮询和健康检查
 *
 * @author gongdewei 2020/3/27
 */
public class ResultConsumerImpl implements ResultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ResultConsumerImpl.class);

    // 结果队列，用于缓存命令执行的结果
    private BlockingQueue<ResultModel> resultQueue;

    // 最后访问时间，用于健康检查
    private volatile long lastAccessTime;

    // 是否正在轮询标志
    private volatile boolean polling;

    // 重入锁，用于同步轮询操作
    private ReentrantLock lock = new ReentrantLock();

    // 批量结果大小限制，默认20条
    private int resultBatchSizeLimit = 20;

    // 结果队列大小限制
    private int resultQueueSize = DistributorOptions.resultQueueSize;

    // 轮询超时时间，默认2秒
    private long pollTimeLimit = 2 * 1000;

    // 消费者唯一标识
    private String consumerId;

    // 是否已关闭标志
    private boolean closed;

    // 正在发送的项目数量统计
    private long sendingItemCount;

    /**
     * 构造函数
     * 初始化最后访问时间和结果队列
     */
    public ResultConsumerImpl() {
        lastAccessTime = System.currentTimeMillis();
        resultQueue = new ArrayBlockingQueue<ResultModel>(resultQueueSize);
    }

    /**
     * 添加结果到队列
     *
     * 如果队列已满，会丢弃最旧的结果以腾出空间
     * 注意：此方法不会阻塞，因为某些Consumer可能已经断开连接，不会再读取数据
     *
     * @param result 要添加的结果模型
     * @return 如果没有丢弃结果返回true，否则返回false
     */
    @Override
    public boolean appendResult(ResultModel result) {
        //可能某些Consumer已经断开，不会再读取，这里不能堵塞！
        boolean discard = false;
        // 如果队列已满，循环移除最旧的元素直到有空间
        while (!resultQueue.offer(result)) {
            ResultModel discardResult = resultQueue.poll();
            discard = true;
        }
        return !discard;
    }

    /**
     * 轮询获取结果列表
     *
     * 使用长轮询模式批量获取结果，支持以下退出条件：
     * 1. 达到批量大小限制
     * 2. 发送延迟超过100ms
     * 3. 总等待时间超过pollTimeLimit
     * 4. 达到发送项目数量限制
     * 5. Consumer被关闭
     *
     * @return 结果列表，如果获取失败或被中断则返回空列表
     */
    @Override
    public List<ResultModel> pollResults() {
        try {
            // 更新最后访问时间
            lastAccessTime = System.currentTimeMillis();
            long accessTime = lastAccessTime;

            // 尝试获取锁，最多等待500ms
            if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
                // 设置轮询状态
                polling = true;
                sendingItemCount = 0;

                // 第一次获取到结果的时间点
                long firstResultTime = 0;
                // sending delay: time elapsed after firstResultTime
                // 发送延迟：从第一次获取结果后经过的时间
                long sendingDelay = 0;
                // waiting time: time elapsed after access
                // 等待时间：从访问开始经过的总时间
                long waitingTime = 0;

                // 创建结果列表，初始容量为批量大小限制
                List<ResultModel> sendingResults = new ArrayList<ResultModel>(resultBatchSizeLimit);

                // 循环获取结果，满足以下任一条件时退出：
                // 1. Consumer已关闭
                // 2. 结果数量达到批量限制
                // 3. 发送延迟超过100ms
                // 4. 等待时间超过轮询时间限制
                while (!closed
                        &&sendingResults.size() < resultBatchSizeLimit
                        && sendingDelay < 100
                        && waitingTime < pollTimeLimit) {
                    // 从队列中获取结果，每次等待100ms
                    ResultModel aResult = resultQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (aResult != null) {
                        // 获取到结果，添加到发送列表
                        sendingResults.add(aResult);

                        //是否为第一次获取到数据
                        if (firstResultTime == 0) {
                            firstResultTime = System.currentTimeMillis();
                        }

                        //判断是否需要立即发送出去
                        // 如果达到发送条件，提前退出循环
                        if (shouldFlush(sendingResults, aResult)) {
                            break;
                        }
                    } else {
                        // 未获取到结果
                        if (firstResultTime > 0) {
                            //获取到部分数据后，队列已经取完，计算发送延时时间
                            sendingDelay = System.currentTimeMillis() - firstResultTime;
                        }
                        //计算总共等待时间，长轮询最大等待时间
                        waitingTime = System.currentTimeMillis() - accessTime;
                    }
                }

                //resultQueue.drainTo(sendingResults, resultSizeLimit-sendingResults.size());
                // 记录调试日志
                if(logger.isDebugEnabled()) {
                    logger.debug("pollResults: {}, results: {}", sendingResults.size(), JSON.toJSONString(sendingResults));
                }
                return sendingResults;
            }
        } catch (InterruptedException e) {
            //e.printStackTrace();
            // 线程被中断，正常返回空列表
        } finally {
            // 确保锁被释放
            if (lock.isHeldByCurrentThread()) {
                lastAccessTime = System.currentTimeMillis();
                polling = false;
                lock.unlock();
            }
        }
        // 未获取到锁或发生异常，返回空列表
        return Collections.emptyList();
    }

    /**
     * 判断是否应该立即刷新发送结果
     *
     * 估算对象数量及大小，判断是否需要立即发送出去
     * 通过累加结果项数量，当达到阈值时触发立即发送
     *
     * @param sendingResults 当前待发送的结果列表
     * @param last 最新添加的结果模型
     * @return 如果应该立即刷新返回true，否则返回false
     */
    private boolean shouldFlush(List<ResultModel> sendingResults, ResultModel last) {
        //TODO 引入一个估算模型，每个model自统计对象数量
        // 累加当前结果的项目数量
        sendingItemCount += ResultConsumerHelper.getItemCount(last);
        // 当累计项目数量达到100时，触发立即发送
        return sendingItemCount >= 100;
    }

    /**
     * 检查消费者是否健康
     *
     * 满足以下任一条件即视为健康：
     * 1. 正在轮询
     * 2. 结果队列未满
     * 3. 最后访问时间在1秒以内
     *
     * @return 如果健康返回true，否则返回false
     */
    @Override
    public boolean isHealthy() {

        return isPolling()
                || resultQueue.size() < resultQueueSize
                || System.currentTimeMillis() - lastAccessTime < 1000;
    }

    /**
     * 获取最后访问时间
     *
     * @return 最后访问时间戳（毫秒）
     */
    @Override
    public long getLastAccessTime() {
        return lastAccessTime;
    }

    /**
     * 关闭消费者
     *
     * 设置关闭标志，停止接收新结果
     */
    @Override
    public void close(){
        this.closed = true;
    }

    /**
     * 检查消费者是否已关闭
     *
     * @return 如果已关闭返回true，否则返回false
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     * 检查是否正在轮询
     *
     * @return 如果正在轮询返回true，否则返回false
     */
    @Override
    public boolean isPolling() {
        return polling;
    }

    /**
     * 获取批量结果大小限制
     *
     * @return 批量结果大小限制
     */
    public int getResultBatchSizeLimit() {
        return resultBatchSizeLimit;
    }

    /**
     * 设置批量结果大小限制
     *
     * @param resultBatchSizeLimit 批量结果大小限制
     */
    public void setResultBatchSizeLimit(int resultBatchSizeLimit) {
        this.resultBatchSizeLimit = resultBatchSizeLimit;
    }

    /**
     * 获取消费者ID
     *
     * @return 消费者唯一标识
     */
    @Override
    public String getConsumerId() {
        return consumerId;
    }

    /**
     * 设置消费者ID
     *
     * @param consumerId 消费者唯一标识
     */
    @Override
    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

}
