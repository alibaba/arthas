package com.taobao.arthas.core.shell.command;

import java.util.List;

/**
 * 命令解析器接口，用于Shell发现和获取可用的命令
 * 任何实现了此接口的类都可以作为命令的提供者
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface CommandResolver {
    /**
     * 获取当前可用的所有命令列表
     * Shell通过此方法来发现有哪些命令可以执行
     *
     * @return 当前可用的命令列表
     */
    List<Command> commands();
}
