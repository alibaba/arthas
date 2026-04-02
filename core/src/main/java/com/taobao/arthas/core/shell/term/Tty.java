package com.taobao.arthas.core.shell.term;

import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * Shell TTY（终端）交互接口
 *
 * 提供与 Shell 终端进行交互的能力，包括获取终端类型、尺寸、
 * 处理输入输出、响应终端大小变化等功能。
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Tty {

    /**
     * 获取终端类型
     *
     * 返回声明的终端类型，例如 {@literal vt100}、{@literal xterm-256} 等。
     * 如果终端没有声明类型，则返回 null。
     *
     * @return 终端类型字符串，如 "vt100"、"xterm-256" 等；如果未声明则返回 null
     */
    String type();

    /**
     * 获取终端宽度（行数）
     *
     * 返回终端的当前宽度，即行数。如果无法确定宽度，则返回 -1。
     *
     * @return 终端宽度（行数），如果未知则返回 -1
     */
    int width();

    /**
     * 获取终端高度（列数）
     *
     * 返回终端的当前高度，即列数。如果无法确定高度，则返回 -1。
     *
     * @return 终端高度（列数），如果未知则返回 -1
     */
    int height();

    /**
     * 设置标准输入的数据处理器
     *
     * 在标准输入上设置一个流处理器，用于读取用户输入的数据。
     * 当用户在终端输入数据时，会调用此处理器。
     *
     * @param handler 标准输入处理器，用于处理用户输入的数据
     * @return 当前对象，支持链式调用
     */
    Tty stdinHandler(Handler<String> handler);

    /**
     * 向标准输出写入数据
     *
     * 将指定的数据字符串写入到终端的标准输出，显示给用户。
     *
     * @param data 要写入的数据字符串
     * @return 当前对象，支持链式调用
     */
    Tty write(String data);

    /**
     * 设置终端大小变化的处理器
     *
     * 设置一个处理器，当终端尺寸（宽度或高度）发生变化时会被调用。
     * 例如，当用户调整终端窗口大小时，此处理器将被触发。
     *
     * @param handler 大小变化处理器，当终端尺寸改变时会被调用
     * @return 当前对象，支持链式调用
     */
    Tty resizehandler(Handler<Void> handler);

}
