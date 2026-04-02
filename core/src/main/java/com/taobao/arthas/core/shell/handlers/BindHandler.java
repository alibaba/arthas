package com.taobao.arthas.core.shell.handlers;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 绑定处理器
 *
 * 该类用于处理终端服务器绑定的异步操作结果。当终端服务器尝试绑定时，
 * 如果绑定失败，该处理器会记录错误日志并更新绑定状态。
 *
 * @author ralf0131 2017-04-24 18:23.
 */
public class BindHandler implements Handler<Future<Void>> {

    /**
     * 日志记录器，用于记录绑定过程中的错误信息
     */
    private static final Logger logger = LoggerFactory.getLogger(BindHandler.class);

    /**
     * 绑定状态的原子引用
     * 使用AtomicBoolean保证多线程环境下的线程安全性
     * true表示已绑定或正在绑定，false表示未绑定
     */
    private AtomicBoolean isBindRef;

    /**
     * 构造函数
     *
     * @param isBindRef 绑定状态的原子引用，用于在绑定失败时更新状态
     */
    public BindHandler(AtomicBoolean isBindRef) {
        this.isBindRef = isBindRef;
    }

    /**
     * 处理绑定事件的结果
     *
     * 该方法在终端服务器绑定操作完成后被调用。如果绑定失败，
     * 会记录错误日志并将绑定状态从true更新为false。
     *
     * @param event 绑定操作的异步结果对象，包含操作状态和可能的异常信息
     */
    @Override
    public void handle(Future<Void> event) {
        // 检查绑定操作是否失败
        if (event.failed()) {
            // 记录绑定失败的错误日志，包括异常堆栈信息
            logger.error("Error listening term server:", event.cause());

            // 使用原子操作将绑定状态从true更新为false
            // compareAndSet确保只有在当前值为true时才会更新为false
            // 这样可以避免在已经解绑的情况下重复操作
            isBindRef.compareAndSet(true, false);
        }
    }
}
