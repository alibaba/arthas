package com.taobao.arthas.core.shell.term;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import io.termd.core.function.Function;

/**
 * 终端接口
 *
 * 定义了终端的基本操作，包括读写、信号处理、命令行读取等功能
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Term extends Tty {

    /**
     * 设置终端大小调整处理器
     *
     * @param handler 大小调整事件处理器
     * @return 返回当前实例，支持链式调用
     */
    @Override
    Term resizehandler(Handler<Void> handler);

    /**
     * 设置标准输入处理器
     *
     * @param handler 标准输入处理器
     * @return 返回当前实例，支持链式调用
     */
    @Override
    Term stdinHandler(Handler<String> handler);

    /**
     * 设置标准输出处理器
     *
     * @param handler 标准输出处理器，用于处理输出的转换
     * @return 返回当前实例，支持链式调用
     */
    Term stdoutHandler(Function<String, String> handler);

    /**
     * 向终端写入数据
     *
     * @param data 要写入的数据
     * @return 返回当前实例，支持链式调用
     */
    @Override
    Term write(String data);

    /**
     * 获取终端最后一次接收输入的时间
     *
     * @return 最后一次接收输入的时间戳
     */
    long lastAccessedTime();

    /**
     * 在终端中回显文本，必要时进行转义处理
     *
     * @param text 要回显的文本
     * @return 返回当前实例，支持链式调用
     */
    Term echo(String text);

    /**
     * 将终端与会话关联
     *
     * @param session 要设置的会话对象
     * @return 返回当前实例，支持链式调用
     */
    Term setSession(Session session);

    /**
     * 设置中断信号处理器（如Ctrl+C）
     *
     * @param handler 中断信号处理器
     * @return 返回当前实例，支持链式调用
     */
    Term interruptHandler(SignalHandler handler);

    /**
     * 设置挂起信号处理器（如Ctrl+Z）
     *
     * @param handler 挂起信号处理器
     * @return 返回当前实例，支持链式调用
     */
    Term suspendHandler(SignalHandler handler);

    /**
     * 提示用户输入一行文本
     *
     * @param prompt 显示的提示符
     * @param lineHandler 处理输入行的处理器
     */
    void readline(String prompt, Handler<String> lineHandler);

    /**
     * 提示用户输入一行文本，并提供自动完成处理器
     *
     * @param prompt 显示的提示符
     * @param lineHandler 处理输入行的处理器
     * @param completionHandler 自动完成处理器
     */
    void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler);

    /**
     * 设置终端关闭时的处理器
     *
     * @param handler 关闭事件处理器
     * @return 返回当前实例，支持链式调用
     */
    Term closeHandler(Handler<Void> handler);

    /**
     * 关闭终端连接
     */
    void close();
}
