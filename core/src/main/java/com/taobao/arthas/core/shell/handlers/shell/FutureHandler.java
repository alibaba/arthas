package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * Future处理器
 *
 * 该类实现了Handler接口,用于处理事件并完成Future对象。
 * 当事件到达时,该处理器会调用Future.complete()方法来标记Future为已完成状态。
 *
 * @param <Void> 事件类型为Void,表示不需要传递具体的事件数据
 * @author beiwei30 on 23/11/2016.
 */
public class FutureHandler implements Handler<Void> {
    /**
     * 要被完成的Future对象
     * 当handle方法被调用时,该Future会被标记为完成状态
     */
    private Future future;

    /**
     * 构造函数
     *
     * @param future 需要被完成的Future对象
     */
    public FutureHandler(Future future) {
        this.future = future;
    }

    /**
     * 处理事件方法
     *
     * 当该方法被调用时,会通知关联的Future对象完成。
     * 这是一个回调方法,通常由事件循环或事件分发器在事件发生时调用。
     *
     * @param event 事件对象,类型为Void,表示不需要传递具体的事件数据
     */
    @Override
    public void handle(Void event) {
        // 标记Future为已完成状态
        future.complete();
    }
}
