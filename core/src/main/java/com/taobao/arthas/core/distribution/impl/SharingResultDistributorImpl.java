package com.taobao.arthas.core.distribution.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.model.InputStatusModel;
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.DistributorOptions;
import com.taobao.arthas.core.distribution.ResultConsumer;
import com.taobao.arthas.core.distribution.SharingResultDistributor;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.Job;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 共享结果分发器实现类
 *
 * 负责将命令执行结果分发给多个消费者
 * 使用单独的分发线程从结果队列中取出数据，并分发给所有注册的消费者
 * 支持消费者动态添加和移除，以及健康检查机制
 */
public class SharingResultDistributorImpl implements SharingResultDistributor {
    private static final Logger logger = LoggerFactory.getLogger(SharingResultDistributorImpl.class);

    // 消费者列表，使用写时复制列表保证线程安全
    private List<ResultConsumer> consumers = new CopyOnWriteArrayList<ResultConsumer>();

    // 待分发结果队列，缓存等待分发的结果
    private BlockingQueue<ResultModel> pendingResultQueue = new ArrayBlockingQueue<ResultModel>(10);

    // 关联的会话对象
    private final Session session;

    // 分发线程
    private Thread distributorThread;

    // 运行状态标志
    private volatile boolean running;

    // 消费者编号生成器，用于生成唯一的消费者ID
    private AtomicInteger consumerNumGenerator = new AtomicInteger(0);

    // 共享结果消费者，用于缓存历史结果，新消费者加入时可以复制这些结果
    private SharingResultConsumerImpl sharingResultConsumer = new SharingResultConsumerImpl();

    // 标记是否已经因为不健康而中断过，避免重复中断导致死循环
    private volatile boolean interruptedForUnhealthy = false;

    /**
     * 构造函数
     *
     * 创建分发器并启动分发线程
     *
     * @param session 关联的会话对象
     */
    public SharingResultDistributorImpl(Session session) {
        this.session = session;
        this.running = true;
        // 创建并启动分发线程
        distributorThread = new Thread(new DistributorTask(), "ResultDistributor");
        distributorThread.setDaemon(true);  // 设置为守护线程，避免阻止 JVM 退出
        distributorThread.start();
    }

    /**
     * 添加结果到待分发队列
     *
     * 将结果添加到待分发队列，如果队列已满则丢弃最旧的结果
     * 注意：此方法设计为非阻塞，避免影响业务线程执行
     *
     * @param result 要分发的结果模型
     */
    @Override
    public void appendResult(ResultModel result) {
        //要避免阻塞影响业务线程执行
        try {
            // 尝试将结果添加到队列，最多等待100ms
            if (!pendingResultQueue.offer(result, 100, TimeUnit.MILLISECONDS)) {
                // 队列已满，移除最旧的结果
                ResultModel discardResult = pendingResultQueue.poll();
                // 正常情况走不到这里，除非distribute 循环堵塞或异常终止
                // 输出队列满，终止当前执行的命令
                interruptJob("result queue is full: "+ pendingResultQueue.size());
            }
        } catch (InterruptedException e) {
            //ignore 线程被中断，忽略异常
        }
    }

    /**
     * 中断当前正在执行的前台任务
     *
     * 当发生严重错误（如队列满、所有消费者不健康）时，中断当前任务
     *
     * @param message 中断原因的描述信息
     */
    private void interruptJob(String message) {
        // 获取当前前台任务
        Job job = session.getForegroundJob();
        if (job != null) {
            // 记录警告日志
            logger.warn(message+", current job was interrupted.", job.id());
            // 中断任务
            job.interrupt();
            // 向队列添加中断消息，以便消费者知晓
            pendingResultQueue.offer(new MessageModel(message+", current job was interrupted."));
        }
    }

    /**
     * 分发结果到所有消费者
     *
     * 从待分发队列中取出结果，分发给所有注册的消费者
     * 同时进行健康检查，当所有消费者都不健康时中断任务
     * 此方法在独立的分发线程中运行
     */
    private void distribute() {
        while (running) {
            try {
                // 从队列中获取结果，最多等待100ms
                ResultModel result = pendingResultQueue.poll(100, TimeUnit.MILLISECONDS);
                if (result != null) {
                    // 将结果添加到共享消费者，用于缓存历史结果
                    sharingResultConsumer.appendResult(result);

                    // 如果没有 consumer，跳过健康检查
                    if (consumers.isEmpty()) {
                        continue;
                    }

                    //判断是否有至少一个consumer是健康的
                    int healthCount = 0;
                    // 遍历所有消费者，分发结果并统计健康数量
                    for (int i = 0; i < consumers.size(); i++) {
                        ResultConsumer consumer = consumers.get(i);
                        // 统计健康消费者数量
                        if(consumer.isHealthy()){
                            healthCount += 1;
                        }
                        // 将结果分发给消费者
                        consumer.appendResult(result);
                    }

                    //所有consumer都不是健康状态，终止当前执行的命令
                    //使用标志位避免重复中断导致死循环
                    if (healthCount == 0 && !interruptedForUnhealthy) {
                        interruptedForUnhealthy = true;
                        interruptJob("all consumers are unhealthy");
                    }
                } else {
                    // 队列为空时，检查是否有健康的consumer，如果有则重置标志位
                    if (interruptedForUnhealthy) {
                        for (int i = 0; i < consumers.size(); i++) {
                            if (consumers.get(i).isHealthy()) {
                                // 发现健康的消费者，重置中断标志
                                interruptedForUnhealthy = false;
                                break;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // 线程被中断，正常退出
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable e) {
                // 捕获所有异常，避免分发线程意外终止
                logger.warn("distribute result failed: " + e.getMessage(), e);
            }
        }
        logger.debug("ResultDistributor thread exited");
    }

    /**
     * 关闭分发器
     *
     * 停止分发线程，关闭所有消费者，清理资源
     */
    @Override
    public void close() {
        // 设置运行标志为false，停止分发循环
        this.running = false;

        // 中断线程，使其尽快退出 poll 阻塞
        if (distributorThread != null) {
            distributorThread.interrupt();
        }

        // 清理 consumers，关闭所有消费者
        for (ResultConsumer consumer : consumers) {
            try {
                consumer.close();
            } catch (Exception e) {
                logger.warn("close consumer failed: " + e.getMessage(), e);
            }
        }
        // 清空消费者列表
        consumers.clear();

        // 清空待处理队列
        pendingResultQueue.clear();
    }

    /**
     * 添加消费者
     *
     * 生成唯一的消费者ID，并将历史结果复制给新消费者
     *
     * @param consumer 要添加的消费者
     */
    @Override
    public void addConsumer(ResultConsumer consumer) {
        // 生成递增的消费者编号
        int consumerNo = consumerNumGenerator.incrementAndGet();
        // 生成唯一的消费者ID：UUID_编号
        String consumerId = UUID.randomUUID().toString().replaceAll("-", "") + "_" + consumerNo;
        consumer.setConsumerId(consumerId);

        //将队列中的消息复制给新的消费者
        // 新消费者可以看到之前的历史结果
        sharingResultConsumer.copyTo(consumer);

        // 将消费者添加到列表
        consumers.add(consumer);
    }

    /**
     * 移除消费者
     *
     * 从消费者列表中移除指定消费者，并关闭它
     *
     * @param consumer 要移除的消费者
     */
    @Override
    public void removeConsumer(ResultConsumer consumer) {
        // 从列表中移除
        consumers.remove(consumer);
        // 关闭消费者
        consumer.close();
    }

    /**
     * 获取所有消费者列表
     *
     * @return 所有注册的消费者列表
     */
    @Override
    public List<ResultConsumer> getConsumers() {
        return consumers;
    }

    /**
     * 根据ID获取消费者
     *
     * @param consumerId 消费者ID
     * @return 找到的消费者，如果不存在则返回null
     */
    @Override
    public ResultConsumer getConsumer(String consumerId) {
        // 遍历消费者列表，查找匹配ID的消费者
        for (int i = 0; i < consumers.size(); i++) {
            ResultConsumer consumer = consumers.get(i);
            if (consumer.getConsumerId().equals(consumerId)) {
                return consumer;
            }
        }
        // 未找到匹配的消费者
        return null;
    }

    /**
     * 分发任务
     *
     * Runnable实现，用于在独立线程中执行分发逻辑
     */
    private class DistributorTask implements Runnable {
        /**
         * 执行分发任务
         */
        @Override
        public void run() {
            distribute();
        }
    }

    /**
     * 共享结果消费者实现类
     *
     * 这是一个特殊的消费者，用于缓存历史结果
     * 新加入的消费者可以从这里复制历史结果
     */
    private static class SharingResultConsumerImpl implements ResultConsumer {
        // 结果队列，用于缓存历史结果
        private BlockingQueue<ResultModel> resultQueue = new ArrayBlockingQueue<ResultModel>(DistributorOptions.resultQueueSize);

        // 队列操作锁，保证复制时的线程安全
        private ReentrantLock queueLock = new ReentrantLock();

        // 最后的输入状态，特殊处理，不放入历史队列
        private InputStatusModel lastInputStatus;

        /**
         * 添加结果到队列
         *
         * 输入状态模型特殊处理，不入历史队列，而是单独保存
         * 其他结果模型加入历史队列，队列满时丢弃最旧的结果
         *
         * @param result 要添加的结果
         * @return 总是返回true
         */
        @Override
        public boolean appendResult(ResultModel result) {
            // 获取队列锁
            queueLock.lock();
            try {
                //输入状态不入历史指令队列，复制时在最后发送
                if (result instanceof InputStatusModel) {
                    // 保存最后的输入状态
                    lastInputStatus = (InputStatusModel) result;
                    return true;
                }

                // 队列满时，循环移除最旧的结果
                while (!resultQueue.offer(result)) {
                    ResultModel discardResult = resultQueue.poll();
                }
            } finally {
                // 确保锁被释放
                if (queueLock.isHeldByCurrentThread()) {
                    queueLock.unlock();
                }
            }
            return true;
        }

        /**
         * 将历史结果复制给新消费者
         *
         * 复制时加锁，避免消息顺序错乱
         * 注意：这里阻塞只影响分发线程，不会影响业务线程
         *
         * @param consumer 目标消费者
         */
        public void copyTo(ResultConsumer consumer) {
            //复制时加锁，避免消息顺序错乱，这里堵塞只影响分发线程，不会影响到业务线程
            queueLock.lock();
            try {
                // 遍历历史队列，将所有结果复制给新消费者
                for (ResultModel result : resultQueue) {
                    consumer.appendResult(result);
                }

                //发送输入状态（最后发送）
                if (lastInputStatus != null) {
                    consumer.appendResult(lastInputStatus);
                }
            } finally {
                // 确保锁被释放
                if (queueLock.isHeldByCurrentThread()) {
                    queueLock.unlock();
                }
            }
        }

        /**
         * 轮询结果（不实现）
         *
         * 共享消费者不支持轮询操作
         *
         * @return null
         */
        @Override
        public List<ResultModel> pollResults() {
            return null;
        }

        /**
         * 获取最后访问时间（不实现）
         *
         * 共享消费者不需要访问时间
         *
         * @return 0
         */
        @Override
        public long getLastAccessTime() {
            return 0;
        }

        /**
         * 关闭消费者（不实现）
         *
         * 共享消费者不需要关闭操作
         */
        @Override
        public void close() {

        }

        /**
         * 检查是否已关闭
         *
         * 共享消费者永远不会关闭
         *
         * @return false
         */
        @Override
        public boolean isClosed() {
            return false;
        }

        /**
         * 检查是否正在轮询
         *
         * 共享消费者不支持轮询
         *
         * @return false
         */
        @Override
        public boolean isPolling() {
            return false;
        }

        /**
         * 获取消费者ID
         *
         * 共享消费者的固定ID
         *
         * @return "shared-consumer"
         */
        @Override
        public String getConsumerId() {
            return "shared-consumer";
        }

        /**
         * 设置消费者ID（不实现）
         *
         * 共享消费者的ID是固定的
         *
         * @param consumerId 消费者ID（忽略）
         */
        @Override
        public void setConsumerId(String consumerId) {
        }

        /**
         * 检查是否健康
         *
         * 共享消费者永远是健康的
         *
         * @return true
         */
        @Override
        public boolean isHealthy() {
            return true;
        }
    }
}
