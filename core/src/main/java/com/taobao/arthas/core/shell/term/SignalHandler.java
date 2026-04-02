package com.taobao.arthas.core.shell.term;

/**
 * 信号处理器接口
 *
 * 用于处理终端信号，如中断信号（Ctrl+C）和挂起信号（Ctrl+Z）
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface SignalHandler {
    /**
     * 传递信号到处理器
     *
     * @param key 信号对应的按键码（如Ctrl+C的按键码）
     * @return 如果信号被成功处理返回true，否则返回false
     */
    boolean deliver(int key);
}