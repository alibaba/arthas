package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.CatModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Cat命令的结果视图类
 *
 * 负责将Cat命令（类似Unix的cat命令，用于查看文件内容）的结果渲染并输出到命令行界面。
 * 继承自ResultView基类，用于处理CatModel类型的结果数据。
 *
 * Result view for CatCommand
 * @author gongdewei 2020/5/11
 */
public class CatView extends ResultView<CatModel> {

    /**
     * 绘制Cat命令的执行结果
     *
     * 该方法将文件内容直接输出到命令行界面。
     * 获取结果中的文件内容，将其写入命令行进程，并添加换行符。
     *
     * @param process 命令处理进程对象，用于向命令行输出内容
     * @param result Cat命令的执行结果模型，包含要显示的文件内容
     */
    @Override
    public void draw(CommandProcess process, CatModel result) {
        // 将文件内容写入命令行，并添加换行符
        process.write(result.getContent()).write("\n");
    }

}
