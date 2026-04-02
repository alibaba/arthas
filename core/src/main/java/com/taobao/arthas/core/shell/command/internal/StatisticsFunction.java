package com.taobao.arthas.core.shell.command.internal;

import io.termd.core.function.Function;

/**
 * 统计类Function接口
 * <p>
 * 该接口扩展了termd-core中的Function接口，专门用于具有统计功能的命令处理。
 * 实现此接口的函数不仅可以执行常规的输入输出转换，还能提供执行结果的统计信息。
 * 在Arthas中，该接口用于那些需要收集和返回执行统计数据的命令，如方法调用统计、耗时统计等。
 * </p>
 *
 * @author diecui1202 on 2017/10/24.
 */
public interface StatisticsFunction extends Function<String, String> {

    /**
     * 获取函数执行结果的统计信息
     * <p>
     * 该方法返回函数执行后的统计数据，通常包括调用次数、耗时、成功率等信息。
     * 与常规的apply方法不同，该方法返回的是聚合后的统计结果，而非单次处理的结果。
     * </p>
     *
     * @return 统计结果的字符串表示，格式由具体实现类决定
     */
    String result();
}
