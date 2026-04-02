package com.taobao.arthas.core.util;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.command.ExitStatus;

/**
 * 命令处理工具类
 * <p>
 * 提供命令处理的通用工具方法，主要用于命令执行完成后的结束处理
 * </p>
 */
public class CommandUtils {

    /**
     * 检查退出状态并结束命令处理
     * <p>
     * 该方法用于命令执行完成后，根据退出状态码和消息来结束命令处理流程。
     * 如果退出状态不为null，则使用状态码和消息结束处理；
     * 如果退出状态为null，则使用默认的错误码-1和错误消息结束处理。
     * </p>
     *
     * @param process 命令处理进程实例，用于结束命令处理
     * @param status 命令执行的退出状态，包含状态码和消息
     */
    public static void end(CommandProcess process, ExitStatus status) {
        // 检查退出状态是否为null
        if (status != null) {
            // 使用退出状态中的状态码和消息结束命令处理
            process.end(status.getStatusCode(), status.getMessage());
        } else {
            // 退出状态为null时，使用默认错误码-1和错误消息
            process.end(-1, "process error, exit status is null");
        }
    }

}
