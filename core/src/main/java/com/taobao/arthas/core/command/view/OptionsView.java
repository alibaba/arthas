package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.OptionVO;
import com.taobao.arthas.core.command.model.OptionsModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Collection;

import static com.taobao.text.ui.Element.label;

/**
 * 选项配置视图类
 *
 * 用于显示或修改Arthas的全局配置选项。
 * 支持两种操作模式：
 * 1. 显示所有配置选项及其详细信息
 * 2. 显示修改配置选项后的结果
 *
 * @author gongdewei 2020/4/15
 */
public class OptionsView extends ResultView<OptionsModel> {

    /**
     * 绘制选项配置信息到命令行界面
     *
     * 根据模型对象的内容，有两种输出模式：
     * 1. 如果存在配置选项列表，则以表格形式显示所有配置项
     * 2. 如果存在修改结果，则显示配置修改的执行结果
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param result 选项模型对象，包含配置选项列表或修改结果
     */
    @Override
    public void draw(CommandProcess process, OptionsModel result) {
        // 如果存在配置选项列表，则显示配置表格
        if (result.getOptions() != null) {
            process.write(RenderUtil.render(drawShowTable(result.getOptions()), process.width()));
        }
        // 如果存在修改结果，则显示修改操作的执行结果
        else if (result.getChangeResult() != null) {
            TableElement table = ViewRenderUtil.renderChangeResult(result.getChangeResult());
            process.write(RenderUtil.render(table, process.width()));
        }
    }

    /**
     * 绘制配置选项展示表格
     *
     * 创建一个包含6列的表格，显示配置选项的详细信息：
     * - LEVEL: 配置级别（如全局、会话等）
     * - TYPE: 配置类型（如字符串、数字、布尔等）
     * - NAME: 配置名称
     * - VALUE: 配置当前值
     * - SUMMARY: 配置简要说明
     * - DESCRIPTION: 配置详细描述
     *
     * @param options 配置选项集合
     * @return 渲染好的表格元素对象
     */
    private Element drawShowTable(Collection<OptionVO> options) {
        // 创建表格元素，设置6列的宽度比例
        // 第1列：级别（1单位宽）
        // 第2列：类型（1单位宽）
        // 第3列：名称（2单位宽）
        // 第4列：值（1单位宽）
        // 第5列：简要说明（3单位宽）
        // 第6列：详细描述（6单位宽）
        TableElement table = new TableElement(1, 1, 2, 1, 3, 6)
                .leftCellPadding(1).rightCellPadding(1);

        // 设置表头，使用粗体样式
        table.row(true, label("LEVEL").style(Decoration.bold.bold()),
                label("TYPE").style(Decoration.bold.bold()),
                label("NAME").style(Decoration.bold.bold()),
                label("VALUE").style(Decoration.bold.bold()),
                label("SUMMARY").style(Decoration.bold.bold()),
                label("DESCRIPTION").style(Decoration.bold.bold()));

        // 遍历配置选项集合，将每个配置项添加到表格中
        for (final OptionVO optionVO : options) {
            table.row("" + optionVO.getLevel(),  // 配置级别
                    optionVO.getType(),  // 配置类型
                    optionVO.getName(),  // 配置名称
                    optionVO.getValue(),  // 配置值
                    optionVO.getSummary(),  // 简要说明
                    optionVO.getDescription());  // 详细描述
        }
        return table;
    }

}
