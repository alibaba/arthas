package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.impl.TermImpl;
import io.termd.core.function.Consumer;

/**
 * 请求处理器
 * <p>
 * 这个类实现了Consumer接口，用于处理终端的输入请求。
 * 当用户在终端中输入一行文本并提交时，这个处理器会被调用。
 * 它的主要职责是：
 * 1. 更新终端的读取状态
 * 2. 将输入的文本行委托给行处理器进行处理
 * </p>
 *
 * @author beiwei30 on 23/11/2016.
 */
public class RequestHandler implements Consumer<String> {
    /**
     * 终端实现对象
     * 用于管理终端的状态和操作
     */
    private TermImpl term;

    /**
     * 行处理器
     * 用于实际处理用户输入的文本行内容
     */
    private final Handler<String> lineHandler;

    /**
     * 构造函数
     *
     * @param term 终端实现对象，用于管理终端状态
     * @param lineHandler 行处理器，用于处理用户输入的文本行
     */
    public RequestHandler(TermImpl term, Handler<String> lineHandler) {
        this.term = term;
        this.lineHandler = lineHandler;
    }

    /**
     * 接受并处理用户输入的文本行
     * <p>
     * 这个方法在用户提交输入时被调用，它执行以下操作：
     * 1. 将终端的读取状态设置为false，表示已完成当前行的读取
     * 2. 将输入的文本行委托给行处理器进行后续处理
     * </p>
     *
     * @param line 用户输入的文本行
     */
    @Override
    public void accept(String line) {
        // 更新终端状态，标记不再处于读取行模式
        term.setInReadline(false);

        // 将输入的行委托给行处理器进行处理
        lineHandler.handle(line);
    }
}
