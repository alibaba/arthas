package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ResetModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 重置视图类
 *
 * 用于显示重置（reset）命令的执行结果。
重置命令用于清除之前通过Arthas对类进行的增强操作，使类恢复到原始状态。
该视图会显示受影响的类信息，包括重置的类数量和具体类名。
 *
 * @author gongdewei 2020/6/22
 */
public class ResetView extends ResultView<ResetModel> {

    /**
     * 绘制重置命令的执行结果到命令行界面
     *
     * 该方法将重置操作的影响信息输出到命令行。
使用ViewRenderUtil工具类来渲染增强器影响（Enhancer Affect）信息，
包括受影响的类数量、类名等详细信息。
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param result 重置模型对象，包含重置操作的影响信息
     */
    @Override
    public void draw(CommandProcess process, ResetModel result) {
        // 使用ViewRenderUtil渲染增强器影响信息并输出
        // 该信息包括重置的类数量、类名列表等
        process.write(ViewRenderUtil.renderEnhancerAffect(result.getAffect()));
    }

}
