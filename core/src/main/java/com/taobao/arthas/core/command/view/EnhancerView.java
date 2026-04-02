package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * 增强器视图
 * 用于渲染类增强操作（如watch、trace等）的执行结果
 * 显示增强操作的影响范围和统计信息
 *
 * Term view for EnhancerModel
 * @author gongdewei 2020/7/21
 */
public class EnhancerView extends ResultView<EnhancerModel> {
    /**
     * 绘制命令执行结果
     * 渲染类增强操作的影响信息，包括增强的类数量、方法数量等
     * 注意：忽略增强结果状态，通过后续输出来判断
     *
     * @param process 命令处理进程，用于输出结果
     * @param result 命令执行结果模型，包含增强操作的影响信息
     */
    @Override
    public void draw(CommandProcess process, EnhancerModel result) {
        // 忽略增强结果状态，通过后续输出来判断
        // 如果存在影响信息，则渲染增强操作的影响效果
        if (result.getEffect() != null) {
            process.write(ViewRenderUtil.renderEnhancerAffect(result.getEffect()));
        }
    }
}
