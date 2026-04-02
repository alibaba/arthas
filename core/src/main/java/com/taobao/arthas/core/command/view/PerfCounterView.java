package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.PerfCounterModel;
import com.taobao.arthas.core.command.model.PerfCounterVO;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * 性能计数器视图类
 *
 * 用于显示JVM的性能计数器（Performance Counter）信息。
 * 支持两种显示模式：
 * 1. 简略模式：仅显示计数器名称和值
 * 2. 详细模式：显示计数器名称、变化性、单位和值
 *
 * @author gongdewei 2020/4/27
 */
public class PerfCounterView extends ResultView<PerfCounterModel> {

    /**
     * 绘制性能计数器信息到命令行界面
     *
     * 根据是否需要详细信息，创建不同格式的表格：
     * - 详细模式（details=true）：显示4列（名称、变化性、单位、值）
     * - 简略模式（details=false）：显示2列（名称、值）
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param result 性能计数器模型对象，包含计数器列表和显示模式标志
     */
    @Override
    public void draw(CommandProcess process, PerfCounterModel result) {
        // 获取性能计数器列表
        List<PerfCounterVO> perfCounters = result.getPerfCounters();
        // 判断是否需要显示详细信息
        boolean details = result.isDetails();

        TableElement table;

        // 根据显示模式创建不同结构的表格
        if (details) {
            // 详细模式：创建4列表格（名称、变化性、单位、值）
            table = new TableElement(3, 1, 1, 10).leftCellPadding(1).rightCellPadding(1);
            table.row(true, label("Name").style(Decoration.bold.bold()),
                    label("Variability").style(Decoration.bold.bold()),
                    label("Units").style(Decoration.bold.bold()), label("Value").style(Decoration.bold.bold()));
        } else {
            // 简略模式：创建2列表格（名称、值）
            table = new TableElement(4, 6).leftCellPadding(1).rightCellPadding(1);
            table.row(true, label("Name").style(Decoration.bold.bold()),
                    label("Value").style(Decoration.bold.bold()));
        }

        // 遍历性能计数器列表，根据显示模式添加数据行
        for (PerfCounterVO counter : perfCounters) {
            if (details) {
                // 详细模式：显示名称、变化性、单位和值
                table.row(counter.getName(), counter.getVariability(),
                        counter.getUnits(), String.valueOf(counter.getValue()));
            } else {
                // 简略模式：仅显示名称和值
                table.row(counter.getName(), String.valueOf(counter.getValue()));
            }
        }

        // 渲染表格并输出到命令行
        process.write(RenderUtil.render(table, process.width()));
    }
}
