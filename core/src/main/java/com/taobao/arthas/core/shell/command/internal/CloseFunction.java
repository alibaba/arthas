package com.taobao.arthas.core.shell.command.internal;

import io.termd.core.function.Function;

/**
 * 可关闭函数接口
 *
 * 该接口扩展了Function接口，增加了一个close()方法。
 * 它用于定义那些在执行完成后需要释放资源的函数。
 * 典型的使用场景是在命令行交互中，某些函数需要在完成后关闭或清理相关资源。
 *
 * @author diecui1202 on 2017/11/2.
 */
public interface CloseFunction extends Function<String, String> {

    /**
     * 关闭函数并释放相关资源
     *
     * 当函数不再使用时，应该调用此方法来清理资源。
     * 实现类应该在此方法中执行必要的清理操作，如关闭文件、释放锁等。
     */
    void close();
}
