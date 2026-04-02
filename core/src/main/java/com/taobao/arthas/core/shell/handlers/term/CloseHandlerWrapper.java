package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import io.termd.core.function.Consumer;

/**
 * 关闭处理器包装类
 *
 * 该类实现了Consumer接口，用于包装内部的核心Handler处理器。
 * 它充当适配器的角色，将Consumer接口的accept方法调用转换为
 * Handler接口的handle方法调用，使得不同类型的处理器能够协同工作。
 *
 * 主要用于终端关闭事件的处理，将标准的事件回调适配到Arthas的处理器体系中。
 *
 * @author beiwei30 on 22/11/2016.
 */
public class CloseHandlerWrapper implements Consumer<Void> {
    /**
     * 内部的核心处理器，负责实际的处理逻辑
     * 使用final修饰确保该引用在构造后不可变，保证线程安全性
     */
    private final Handler<Void> handler;

    /**
     * 构造函数
     *
     * 创建一个关闭处理器包装器，将传入的Handler包装成Consumer接口的实现。
     *
     * @param handler 内部的核心处理器，负责实际处理关闭事件
     */
    public CloseHandlerWrapper(Handler<Void> handler) {
        this.handler = handler;
    }

    /**
     * 接受并处理关闭事件
     *
     * 该方法是Consumer接口的实现，当关闭事件发生时被调用。
     * 它将调用委托给内部的Handler处理器执行实际的处理逻辑。
     *
     * @param v 关闭事件的参数，此处为Void类型表示无具体参数
     */
    @Override
    public void accept(Void v) {
        // 将处理逻辑委托给内部的Handler处理器
        handler.handle(v);
    }
}
