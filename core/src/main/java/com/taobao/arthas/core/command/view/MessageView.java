package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 消息视图类
 *
 * 用于渲染简单的文本消息，将消息内容输出到命令行界面。
 * 该类是 ResultView 的子类，专门处理 MessageModel 类型的结果对象。
 *
 * @author gongdewei 2020/4/2
 */
public class MessageView extends ResultView<MessageModel> {

    /**
     * 绘制消息内容到命令行界面
     *
     * 该方法将 MessageModel 中包含的消息内容输出到指定的命令进程。
     * 这是一个简单的文本输出操作，用于显示单条消息。
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param result 消息模型对象，包含需要显示的消息内容
     */
    @Override
    public void draw(CommandProcess process, MessageModel result) {
        writeln(process, result.getMessage());
    }
}
