package com.taobao.arthas.core.shell.handlers;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.future.Future;

/**
 * 空操作处理器(No-Operation Handler)
 *
 * 该类实现了Handler接口,但不执行任何实际的业务逻辑。
 * 主要用途:
 * 1. 作为默认的处理器实现,避免空指针异常
 * 2. 用于日志记录和错误处理,特别是Future失败时的错误日志
 * 3. 作为占位符,在不需要具体处理逻辑时使用
 *
 * 该处理器会检查事件是否为失败的Future,如果是则记录错误日志,
 * 这对于调试和问题追踪非常有用。
 *
 * @param <E> 事件类型参数,可以是任何类型
 * @author beiwei30 on 22/11/2016.
 */
public class NoOpHandler<E> implements Handler<E> {

    /**
     * 日志记录器
     * 用于记录Future失败时的错误信息
     */
    private static final Logger logger = LoggerFactory.getLogger(NoOpHandler.class);

    /**
     * 处理事件方法
     *
     * 该方法不执行任何实际的操作,但如果事件是一个失败的Future,
     * 则会记录错误日志,包括失败的原因。
     *
     * 这种设计使得NoOpHandler既能保持"空操作"的特性,
     * 又能提供有价值的调试信息。
     *
     * @param event 需要处理的事件对象,可以是任何类型
     */
    @Override
    public void handle(E event) {
        // 检查事件是否为Future实例
        if (event instanceof Future && ((Future) event).failed()) {
            // 如果Future失败,记录错误日志和失败原因
            logger.error("Error listening term server:", ((Future) event).cause());
        }
    }
}
