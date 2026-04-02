package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.VersionModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 版本信息视图类
 * 用于展示 Arthas 的版本信息
 *
 * @author gongdewei 2020/3/27
 */
public class VersionView extends ResultView<VersionModel> {

    /**
     * 绘制版本信息到命令行
     *
     * @param process 命令进程对象，用于输出信息
     * @param result 版本模型对象，包含版本信息
     */
    @Override
    public void draw(CommandProcess process, VersionModel result) {
        // 将版本信息写入命令行输出
        writeln(process, result.getVersion());
    }

}
