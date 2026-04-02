package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 路径追踪建议监听器
 * 用于在方法调用路径追踪时接收和处理通知
 *
 * @author ralf0131 2017-01-05 13:59.
 */
public class PathTraceAdviceListener extends AbstractTraceAdviceListener {

    /**
     * 构造函数
     * 创建一个路径追踪建议监听器实例
     *
     * @param command 追踪命令对象，包含追踪配置信息
     * @param process 命令处理进程，用于输出结果和与用户交互
     */
    public PathTraceAdviceListener(TraceCommand command, CommandProcess process) {
        // 调用父类构造函数，初始化追踪建议监听器
        super(command, process);
    }
}
