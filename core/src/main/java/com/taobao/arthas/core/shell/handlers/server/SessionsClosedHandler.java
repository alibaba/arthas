package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多会话关闭处理器
 *
 * 当需要等待多个会话全部关闭完成时，该处理器用于跟踪剩余未关闭的会话数量。
 * 只有当所有会话都关闭完成后，才会调用完成处理器。
 * 这种模式通常用于Shell服务器关闭时的优雅退出场景。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class SessionsClosedHandler implements Handler<Future<Void>> {
    /**
     * 剩余未关闭会话的计数器
     * 使用AtomicInteger确保在多线程环境下的线程安全性
     */
    private final AtomicInteger count;

    /**
     * 所有会话关闭完成后的回调处理器
     * 当计数器减到0时，会调用此处理器
     */
    private final Handler<Future<Void>> completionHandler;

    /**
     * 构造函数
     *
     * @param count 初始会话数量的计数器
     * @param completionHandler 所有会话关闭完成后的回调处理器
     */
    public SessionsClosedHandler(AtomicInteger count, Handler<Future<Void>> completionHandler) {
        this.count = count;
        this.completionHandler = completionHandler;
    }

    /**
     * 处理单个会话关闭事件
     *
     * 每当一个会话关闭时，计数器减1。
     * 当计数器减到0时，表示所有会话都已关闭，此时调用完成处理器。
     *
     * @param event 表示会话关闭结果的异步对象
     */
    @Override
    public void handle(Future<Void> event) {
        // 原子性地将计数器减1，并检查是否所有会话都已关闭
        if (count.decrementAndGet() == 0) {
            // 所有会话都已关闭，调用完成处理器并返回成功的Future
            completionHandler.handle(Future.<Void>succeededFuture());
        }
    }
}
