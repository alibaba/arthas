package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.CommandVO;
import com.taobao.arthas.core.command.model.HelpModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.usage.StyledUsageFormatter;
import com.taobao.middleware.cli.CLI;
import com.taobao.text.Color;
import com.taobao.text.Decoration;
import com.taobao.text.Style;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.LabelElement;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;

import static com.taobao.text.ui.Element.label;
import static com.taobao.text.ui.Element.row;

/**
 * 帮助信息视图类
 *
 * <p>负责渲染和显示 Arthas 命令的帮助信息，包括：</p>
 * <ul>
 *   <li>主帮助信息：显示所有可用命令的列表和简要描述</li>
 *   <li>命令详细帮助：显示单个命令的详细使用说明</li>
 * </ul>
 *
 * @author gongdewei 2020/4/3
 */
public class HelpView extends ResultView<HelpModel> {

    /**
     * 绘制帮助信息
     *
     * <p>根据结果类型渲染不同的帮助信息：</p>
     * <ul>
     *   <li>如果包含命令列表，显示主帮助页面</li>
     *   <li>如果包含单个命令详情，显示该命令的详细帮助</li>
     * </ul>
     *
     * @param process 命令处理进程，用于输出渲染结果
     * @param result 帮助模型数据，包含命令列表或单个命令详情
     */
    @Override
    public void draw(CommandProcess process, HelpModel result) {
        if (result.getCommands() != null) {
            // 渲染主帮助信息（所有命令列表）
            String message = RenderUtil.render(mainHelp(result.getCommands()), process.width());
            process.write(message);
        } else if (result.getDetailCommand() != null) {
            // 渲染单个命令的详细帮助信息
            process.write(commandHelp(result.getDetailCommand().cli(), process.width()));
        }
    }

    /**
     * 生成主帮助页面的表格元素
     *
     * <p>创建一个包含所有命令的表格，每个命令显示名称和简要描述：</p>
     * <ul>
     *   <li>NAME 列：命令名称，使用绿色显示</li>
     *   <li>DESCRIPTION 列：命令的简要描述</li>
     * </ul>
     *
     * @param commands 命令值对象列表，包含所有可用命令的信息
     * @return 渲染好的表格元素
     */
    private static Element mainHelp(List<CommandVO> commands) {
        // 创建表格元素，设置左右内边距为1个字符
        TableElement table = new TableElement().leftCellPadding(1).rightCellPadding(1);
        // 添加表头行：NAME（加粗显示）和 DESCRIPTION
        table.row(new LabelElement("NAME").style(Style.style(Decoration.bold)), new LabelElement("DESCRIPTION"));
        // 遍历命令列表，添加每个命令的行
        for (CommandVO commandVO : commands) {
            // 命令名称使用绿色显示，后面跟着命令描述
            table.add(row().add(label(commandVO.getName()).style(Style.style(Color.green))).add(label(commandVO.getSummary())));
        }
        return table;
    }

    /**
     * 生成单个命令的详细帮助信息
     *
     * <p>使用样式化的使用格式化器来生成命令的详细帮助文档，
     * 包括命令用法、参数说明、选项等信息。</p>
     *
     * @param command CLI 命令对象，包含命令的定义和元数据
     * @param width 输出宽度（字符数），用于格式化显示
     * @return 格式化后的命令详细帮助字符串
     */
    private static String commandHelp(CLI command, int width) {
        // 使用样式化的使用格式化器生成帮助信息
        return StyledUsageFormatter.styledUsage(command, width);
    }
}
