package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.JFRModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Java Flight Recorder（JFR）命令视图类
 *
 * <p>负责渲染和显示 JFR 相关命令的输出结果。</p>
 * <p>JFR 是 JDK 提供的性能监控和诊断工具，可以记录 JVM 运行时的各种事件。</p>
 *
 * @author longxu 2022/7/25
 */
public class JFRView extends ResultView<JFRModel> {

    /**
     * 绘制 JFR 命令的输出结果
     *
     * <p>将 JFR 模型中的输出内容写入命令进程，完成结果的显示。</p>
     *
     * @param process 命令处理进程，用于输出渲染结果
     * @param result JFR 模型数据，包含 JFR 命令的输出信息
     */
    @Override
    public void draw(CommandProcess process, JFRModel result) {
        // 将 JFR 输出结果写入进程
        writeln(process, result.getJfrOutput());
    }
}
