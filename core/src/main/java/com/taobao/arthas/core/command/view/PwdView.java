package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 当前工作目录视图类
 *
 * 用于显示Arthas当前的工作目录。
 * 该功能类似于Unix/Linux系统中的pwd命令，用于打印当前工作目录。
 *
 * @author gongdewei 2020/5/11
 */
public class PwdView extends ResultView<PwdModel> {

    /**
     * 绘制当前工作目录到命令行界面
     *
     * 该方法将当前工作目录的路径输出到命令行界面。
     * 输出格式为：目录路径后跟一个换行符。
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param result 工作目录模型对象，包含当前工作目录的路径信息
     */
    @Override
    public void draw(CommandProcess process, PwdModel result) {
        // 将工作目录路径写入命令行，并添加换行符
        process.write(result.getWorkingDir()).write("\n");
    }
}
