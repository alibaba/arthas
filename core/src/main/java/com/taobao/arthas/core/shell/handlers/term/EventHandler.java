package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.term.impl.TermImpl;
import io.termd.core.function.BiConsumer;
import io.termd.core.tty.TtyEvent;

/**
 * 终端事件处理器
 *
 * 该类实现了BiConsumer接口，用于处理来自TTY（Teletype）层的事件。
 * TTY事件是终端底层的特殊信号，包括中断、文件结束和挂起等操作。
 *
 * 支持的事件类型：
 * 1. INTR (Interrupt): 中断信号，通常由Ctrl+C触发，用于中断当前正在执行的命令
 * 2. EOF (End of File): 文件结束信号，通常由Ctrl+D触发，表示输入结束或退出
 * 3. SUSP (Suspend): 挂起信号，通常由Ctrl+Z触发，用于挂起当前进程
 *
 * 该处理器将这些底层事件分发到终端实现类的相应处理方法中。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class EventHandler implements BiConsumer<TtyEvent, Integer> {
    /**
     * 终端实现类的引用
     * 用于将事件分发到相应的处理方法
     */
    private TermImpl term;

    /**
     * 构造函数
     *
     * 创建一个终端事件处理器，将其绑定到指定的终端实例。
     *
     * @param term 终端实现类的实例，用于处理具体的事件
     */
    public EventHandler(TermImpl term) {
        this.term = term;
    }

    /**
     * 接收并处理终端事件
     *
     * 该方法在TTY事件发生时被调用，根据事件类型将处理分发给
     * 终端实现类的相应方法。每个事件都携带一个键值参数，
     * 表示触发该事件的按键或信号值。
     *
     * @param event 事件类型枚举，可以是INTR、EOF或SUSP
     * @param key 事件相关的键值，通常表示触发事件的按键码
     */
    @Override
    public void accept(TtyEvent event, Integer key) {
        // 根据事件类型进行分发处理
        switch (event) {
            case INTR:
                // 处理中断信号（Interrupt，通常是Ctrl+C）
                // 用于中断当前正在执行的命令或操作
                term.handleIntr(key);
                break;
            case EOF:
                // 处理文件结束信号（End of File，通常是Ctrl+D）
                // 表示输入结束或请求退出当前会话
                term.handleEof(key);
                break;
            case SUSP:
                // 处理挂起信号（Suspend，通常是Ctrl+Z）
                // 用于挂起当前正在执行的进程
                term.handleSusp(key);
                break;
        }
    }
}
