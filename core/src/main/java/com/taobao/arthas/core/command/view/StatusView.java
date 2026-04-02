package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.StatusModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 状态信息的终端视图类
 * 负责显示命令执行状态或简单消息信息
 *
 * @author gongdewei 2020/3/27
 */
public class StatusView extends ResultView<StatusModel> {

    /**
     * 绘制状态信息视图
     * 将状态消息输出到终端
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 状态模型对象，包含要显示的消息内容
     */
    @Override
    public void draw(CommandProcess process, StatusModel result) {
        // 如果存在消息内容，则输出到终端
        if (result.getMessage() != null) {
            writeln(process, result.getMessage());
        }
    }

}
