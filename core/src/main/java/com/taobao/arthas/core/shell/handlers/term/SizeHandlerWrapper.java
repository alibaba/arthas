package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import io.termd.core.function.Consumer;
import io.termd.core.util.Vector;

/**
 * 终端尺寸变化处理器包装类
 *
 * 该类实现了 io.termd.core.function.Consumer 接口，用于处理终端尺寸变化事件。
 * 它将 termd 库的 Vector 类型的尺寸事件转换为 Arthas 内部的 Handler<Void> 类型。
 *
 * @author beiwei30 on 22/11/2016.
 */
public class SizeHandlerWrapper implements Consumer<Vector> {

    /**
     * 内部的处理器，用于处理终端尺寸变化事件
     * 当终端尺寸发生变化时，该处理器会被调用（参数为 null）
     */
    private final Handler<Void> handler;

    /**
     * 构造函数
     *
     * @param handler 内部处理器，用于处理终端尺寸变化事件
     */
    public SizeHandlerWrapper(Handler<Void> handler) {
        this.handler = handler;
    }

    /**
     * 接收并处理终端尺寸变化事件
     *
     * 当终端尺寸发生变化时，该方法会被调用。
     * 注意：虽然接收了 Vector 类型的 resize 参数，但实际上并不使用该参数，
     * 而是直接调用内部处理器的 handle 方法并传入 null。
     *
     * @param resize 终端尺寸变化信息，包含新的行数和列数（该参数在当前实现中未使用）
     */
    @Override
    public void accept(Vector resize) {
        // 调用内部处理器处理尺寸变化事件，传入 null 作为参数
        handler.handle(null);
    }
}
