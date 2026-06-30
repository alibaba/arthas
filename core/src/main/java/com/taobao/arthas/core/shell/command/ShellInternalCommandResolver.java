package com.taobao.arthas.core.shell.command;

/**
 * Shell 内部命令的 resolver 标记接口。
 * <p>
 * 实现该接口的 resolver 只用于 shell 自身控制命令，不参与普通命令查找，避免被 telnet/http/mcp 等执行链路命中。
 */
public interface ShellInternalCommandResolver extends CommandResolver {
}
