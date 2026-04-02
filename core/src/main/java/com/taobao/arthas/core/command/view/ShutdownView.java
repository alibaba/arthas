package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ShutdownModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 关闭命令的视图
 * 用于显示 Arthas 服务器关闭时的消息
 *
 * @author gongdewei 2020/6/22
 */
public class ShutdownView extends ResultView<ShutdownModel> {

    /**
     * 渲染关闭消息
     * 显示服务器关闭的相关信息
     *
     * @param process 命令处理进程，用于写入输出
     * @param result 关闭命令的结果模型，包含关闭消息
     */
    @Override
    public void draw(CommandProcess process, ShutdownModel result) {
        // 输出关闭消息并换行
        // 消息内容通常包括 "Arthas server is going to shutdown. Bye." 等
        process.write(result.getMessage()).write("\n");
    }
}
