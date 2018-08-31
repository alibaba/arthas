package com.taobao.arthas.core.shell.command.internal;

import io.termd.core.function.Function;

/**
 * @author diecui1202 on 2017/11/2.
 */
public interface CloseFunction extends Function<String, String> {

    void close();
}
