package com.taobao.arthas.core.distribution.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.distribution.PackingResultDistributor;
import com.taobao.arthas.core.shell.session.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 打包结果分发器实现类
 * <p>
 * 该类实现了PackingResultDistributor接口，提供命令结果的打包和分发功能。
 * 主要用于将命令执行的结果缓存到内存队列中，以便后续批量获取和处理。
 * </p>
 * <p>
 * 核心特性：
 * 1. 使用阻塞队列（ArrayBlockingQueue）缓存命令结果，容量为500
 * 2. 当队列满时，新的结果会被丢弃并记录警告日志
 * 3. 支持批量获取所有缓存的结果
 * 4. 线程安全，可以在多线程环境中使用
 * </p>
 */
public class PackingResultDistributorImpl implements PackingResultDistributor {
    // 日志记录器，用于记录关键操作和异常信息
    private static final Logger logger = LoggerFactory.getLogger(PackingResultDistributorImpl.class);

    /**
     * 结果队列，用于缓存命令执行的结果
     * <p>
     * 使用有界阻塞队列，容量为500，用于在内存中缓存命令结果。
     * 当队列满时，offer操作会返回false，而不是阻塞等待。
     * </p>
     */
    private BlockingQueue<ResultModel> resultQueue = new ArrayBlockingQueue<ResultModel>(500);

    /**
     * Arthas会话对象
     * <p>
     * 保存当前会话的引用，用于后续可能需要访问会话信息的场景。
     * 虽然当前实现中未直接使用，但保留此字段以备未来扩展使用。
     * </p>
     */
    private final Session session;

    /**
     * 构造函数
     * <p>
     * 创建一个新的PackingResultDistributorImpl实例，并关联到指定的会话。
     * </p>
     *
     * @param session Arthas会话对象，不能为null
     */
    public PackingResultDistributorImpl(Session session) {
        this.session = session;
    }

    /**
     * 添加命令结果到队列
     * <p>
     * 将命令执行的结果添加到结果队列中。如果队列已满，则丢弃该结果并记录警告日志。
     * </p>
     * <p>
     * 使用offer方法而非put方法，避免在队列满时阻塞调用线程。
     * 当队列满时，offer返回false，此时会记录警告日志，包含当前队列大小和被丢弃的结果信息。
     * </p>
     *
     * @param result 要添加的命令结果，不能为null
     */
    @Override
    public void appendResult(ResultModel result) {
        // 尝试将结果添加到队列，如果队列已满则返回false
        if (!resultQueue.offer(result)) {
            // 队列已满，记录警告日志，包含队列当前大小和被丢弃的结果的JSON表示
            logger.warn("result queue is full: {}, discard later result: {}", resultQueue.size(), JSON.toJSONString(result));
        }
    }

    /**
     * 关闭分发器
     * <p>
     * 释放分发器占用的资源。当前实现为空方法，无需执行任何清理操作。
     * </p>
     * <p>
     * 注意：由于使用的是基于内存的ArrayBlockingQueue，且没有需要释放的外部资源，
     * 因此关闭操作不需要做任何处理。队列会在对象被垃圾回收时自动释放。
     * </p>
    */
    @Override
    public void close() {
        // 当前实现无需执行任何清理操作
    }

    /**
     * 获取所有缓存的结果
     * <p>
     * 从结果队列中获取所有已缓存的命令结果，并清空队列。
     * 使用drainTo方法一次性将所有元素从队列转移到列表中，效率较高。
     * </p>
     * <p>
     * 注意：此方法会清空队列，多次调用只会在第一次返回所有结果，后续调用返回空列表。
     * </p>
     *
     * @return 包含所有缓存结果的列表，如果队列中没有结果则返回空列表
     */
    @Override
    public List<ResultModel> getResults() {
        // 创建一个新的ArrayList，初始容量为队列当前大小
        ArrayList<ResultModel> results = new ArrayList<ResultModel>(resultQueue.size());
        // 将队列中的所有元素转移到列表中，并清空队列
        resultQueue.drainTo(results);
        return results;
    }

}
