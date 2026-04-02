package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import io.termd.core.function.Consumer;
import io.termd.core.util.Helper;

/**
 * 标准输入处理器包装类
 *
 * 该类实现了 io.termd.core.function.Consumer 接口，用于处理用户从标准输入（stdin）输入的数据。
 * 它将 termd 库的 int[] 类型的码点数组转换为 Arthas 内部的 String 类型的处理器。
 *
 * @author beiwei30 on 22/11/2016.
 */
public class StdinHandlerWrapper implements Consumer<int[]> {

    /**
     * 内部的处理器，用于处理用户从标准输入输入的字符串
     */
    private final Handler<String> handler;

    /**
     * 构造函数
     *
     * @param handler 内部处理器，用于处理标准输入的字符串数据
     */
    public StdinHandlerWrapper(Handler<String> handler) {
        this.handler = handler;
    }

    /**
     * 接收并处理标准输入事件
     *
     * 当用户从标准输入输入数据时，该方法会被调用。
     * 该方法接收 Unicode 码点数组，并将其转换为字符串后传递给内部处理器处理。
     *
     * @param codePoints Unicode 码点数组，表示用户输入的字符
     *                   例如：输入 "ABC" 会对应码点数组 [65, 66, 67]
     */
    @Override
    public void accept(int[] codePoints) {
        // 使用 Helper 工具类将码点数组转换为字符串，然后调用内部处理器处理
        handler.handle(Helper.fromCodePoints(codePoints));
    }
}
