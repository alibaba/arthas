package com.taobao.arthas.core.command.view;

import com.sun.management.VMOption;
import com.taobao.arthas.core.command.model.VMOptionModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * JVM虚拟机选项的终端视图类
 * 负责显示和修改JVM VM选项，包括查看所有选项、修改可写选项等
 *
 * @author gongdewei 2020/4/15
 */
public class VMOptionView extends ResultView<VMOptionModel> {

    /**
     * 绘制VMOption视图
     * 根据操作类型显示不同的内容：查看选项列表或显示修改结果
     *
     * @param process 命令处理进程，用于输出结果
     * @param result VMOption模型对象，包含VM选项或修改结果
     */
    @Override
    public void draw(CommandProcess process, VMOptionModel result) {
        // 场景1: 显示VM选项列表
        if (result.getVmOptions() != null) {
            // 将VM选项列表渲染为表格格式
            process.write(renderVMOptions(result.getVmOptions(), process.width()));
        // 场景2: 显示VM选项修改结果
        } else if (result.getChangeResult() != null) {
            // 渲染修改结果表格（包含修改前后的值对比）
            TableElement table = ViewRenderUtil.renderChangeResult(result.getChangeResult());
            process.write(RenderUtil.render(table, process.width()));
        }
    }

    /**
     * 渲染VM选项列表为表格格式
     * 表格包含四列：KEY（选项名）、VALUE（值）、ORIGIN（来源）、WRITEABLE（是否可写）
     *
     * @param diagnosticOptions VM选项列表
     * @param width 终端宽度，用于调整表格显示
     * @return 格式化后的表格字符串
     */
    private static String renderVMOptions(List<VMOption> diagnosticOptions, int width) {
        // 创建表格，包含4列
        TableElement table = new TableElement(1, 1, 1, 1).leftCellPadding(1).rightCellPadding(1);
        // 添加表头行：KEY、VALUE、ORIGIN、WRITEABLE
        table.row(true, label("KEY").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()),
                label("ORIGIN").style(Decoration.bold.bold()),
                label("WRITEABLE").style(Decoration.bold.bold()));

        // 遍历所有VM选项，添加到表格中
        for (VMOption option : diagnosticOptions) {
            // 每行包含：选项名、选项值、来源（如DEFAULT、CONFIG_FILE等）、是否可写（true/false）
            table.row(option.getName(), option.getValue(), "" + option.getOrigin(), "" + option.isWriteable());
        }

        // 渲染表格为字符串
        return RenderUtil.render(table, width);
    }
}
