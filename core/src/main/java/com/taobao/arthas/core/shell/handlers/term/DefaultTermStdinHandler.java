package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.term.impl.TermImpl;
import io.termd.core.function.Consumer;

/**
 * 默认终端标准输入处理器
 *
 * 该类实现了Consumer接口，专门用于处理来自终端标准输入的数据。
 * 当用户在终端输入字符时，这些字符会被编码成Unicode码点数组，
 * 然后由该处理器进行接收和处理。
 *
 * 处理流程包括两个步骤：
 * 1. 将输入的字符回显到终端，让用户看到自己输入的内容
 * 2. 将输入事件加入到readline队列中，等待后续的命令解析和处理
 *
 * @author beiwei30 on 23/11/2016.
 */
public class DefaultTermStdinHandler implements Consumer<int[]> {
    /**
     * 终端实现类的引用
     * 用于执行回显操作和访问readline处理器
     */
    private TermImpl term;

    /**
     * 构造函数
     *
     * 创建一个默认的标准输入处理器，将其绑定到指定的终端实例。
     *
     * @param term 终端实现类的实例，用于执行终端相关操作
     */
    public DefaultTermStdinHandler(TermImpl term) {
        this.term = term;
    }

    /**
     * 接收并处理标准输入数据
     *
     * 该方法在用户输入字符时被调用，接收Unicode码点数组形式的输入数据。
     * 处理流程包括回显输入到终端，并将事件加入到readline处理队列。
     *
     * @param codePoints Unicode码点数组，表示用户输入的字符
     *                    每个整数代表一个Unicode字符的码点值
     */
    @Override
    public void accept(int[] codePoints) {
        // 将用户输入的字符回显到终端屏幕
        // 这样用户可以看到自己输入的内容
        term.echo(codePoints);

        // 将输入事件加入到readline的事件队列中
        // readline会处理这些输入，包括命令解析、历史记录等功能
        term.getReadline().queueEvent(codePoints);
    }
}
