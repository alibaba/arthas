package com.taobao.arthas.core.shell.command.internal;

import io.termd.core.function.Function;

/**
 * 统计类Function的接口
 *
 * @author diecui1202 on 2017/10/24.
 */
public interface StatisticsFunction extends Function<String, String> {

    String result();
}
