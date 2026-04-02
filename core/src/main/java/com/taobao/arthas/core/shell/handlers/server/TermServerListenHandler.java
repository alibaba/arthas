package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;
import com.taobao.arthas.core.shell.term.TermServer;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 终端服务器监听处理器
 *
 * 该处理器用于等待多个终端服务器（TermServer）全部启动完成。
 * 当所有服务器都成功监听后，通知Shell服务器启动成功。
 * 如果任何一个服务器启动失败，则关闭所有已启动的服务器并通知失败。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class TermServerListenHandler implements Handler<Future<TermServer>> {
    /**
     * Shell服务器实例
     * 用于在所有终端服务器启动成功后进行初始化配置
     */
    private ShellServerImpl shellServer;

    /**
     * 监听完成后的回调处理器
     * 用于通知调用者所有终端服务器已启动完成
     */
    private Handler<Future<Void>> listenHandler;

    /**
     * 需要启动的终端服务器列表
     * 保存所有待启动的服务器引用，以便在失败时进行清理
     */
    private List<TermServer> toStart;

    /**
     * 剩余未完成监听的终端服务器计数器
     * 使用AtomicInteger确保多线程环境下的线程安全性
     */
    private AtomicInteger count;

    /**
     * 是否有服务器启动失败的标志
     * 使用AtomicBoolean确保多线程环境下的线程安全性
     */
    private AtomicBoolean failed;

    /**
     * 构造函数
     *
     * @param shellServer Shell服务器实例
     * @param listenHandler 监听完成后的回调处理器
     * @param toStart 需要启动的终端服务器列表
     */
    public TermServerListenHandler(ShellServerImpl shellServer, Handler<Future<Void>> listenHandler, List<TermServer> toStart) {
        this.shellServer = shellServer;
        this.listenHandler = listenHandler;
        this.toStart = toStart;
        // 初始化计数器为待启动服务器的数量
        this.count = new AtomicInteger(toStart.size());
        // 初始化失败标志为false
        this.failed = new AtomicBoolean();
    }

    /**
     * 处理单个终端服务器监听完成事件
     *
     * 每当一个终端服务器完成监听（成功或失败）时调用此方法。
     * 当所有服务器都完成监听后，根据是否有失败决定最终结果：
     * - 如果有失败：关闭所有已启动的服务器，并通知失败
     * - 如果全部成功：初始化Shell服务器，并通知成功
     *
     * @param ar 表示终端服务器监听结果的异步对象
     */
    @Override
    public void handle(Future<TermServer> ar) {
        // 检查当前服务器监听是否失败
        if (ar.failed()) {
            // 标记整体状态为失败
            failed.set(true);
        }

        // 原子性地将计数器减1，并检查是否所有服务器都已完成监听
        if (count.decrementAndGet() == 0) {
            // 检查是否有服务器启动失败
            if (failed.get()) {
                // 有服务器启动失败，通知监听处理器并传递失败原因
                listenHandler.handle(Future.<Void>failedFuture(ar.cause()));
                // 关闭所有已启动的终端服务器，进行资源清理
                for (TermServer termServer : toStart) {
                    termServer.close();
                }
            } else {
                // 所有服务器都启动成功，设置Shell服务器为未关闭状态
                shellServer.setClosed(false);
                // 启动Shell服务器的定时器
                shellServer.setTimer();
                // 通知监听处理器启动成功
                listenHandler.handle(Future.<Void>succeededFuture());
            }
        }
    }
}
