package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MonitorModel;
import com.taobao.arthas.core.command.monitor200.MonitorData;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.text.DecimalFormat;

import static com.taobao.text.ui.Element.label;

/**
 * 方法监控视图类
 *
 * 用于以表格形式展示方法执行监控数据，包括时间戳、类名、方法名、
 * 总调用次数、成功次数、失败次数、平均响应时间和失败率等统计信息。
 *
 * @author gongdewei 2020/4/28
 */
public class MonitorView extends ResultView<MonitorModel> {

    /**
     * 绘制监控数据表格到命令行界面
     *
     * 该方法将方法监控数据以表格形式展示，表格包含以下列：
     * - timestamp: 时间戳
     * - class: 类名
     * - method: 方法名
     * - total: 总调用次数
     * - success: 成功次数
     * - fail: 失败次数
     * - avg-rt(ms): 平均响应时间（毫秒）
     * - fail-rate: 失败率（百分比）
     *
     * @param process 命令进程对象，用于与用户交互和输出内容
     * @param result 监控模型对象，包含需要显示的监控数据列表
     */
    @Override
    public void draw(CommandProcess process, MonitorModel result) {
        // 创建表格元素，设置8列的宽度比例
        // 第1列：时间戳（2单位宽）
        // 第2列：类名（3单位宽）
        // 第3列：方法名（3单位宽）
        // 第4-8列：统计数据（各1单位宽）
        TableElement table = new TableElement(2, 3, 3, 1, 1, 1, 1, 1).leftCellPadding(1).rightCellPadding(1);

        // 设置表头，使用粗体样式
        table.row(true, label("timestamp").style(Decoration.bold.bold()),
                label("class").style(Decoration.bold.bold()),
                label("method").style(Decoration.bold.bold()),
                label("total").style(Decoration.bold.bold()),
                label("success").style(Decoration.bold.bold()),
                label("fail").style(Decoration.bold.bold()),
                label("avg-rt(ms)").style(Decoration.bold.bold()),
                label("fail-rate").style(Decoration.bold.bold()));

        // 创建十进制格式化对象，保留两位小数
        final DecimalFormat df = new DecimalFormat("0.00");

        // 遍历监控数据列表，将每条数据添加到表格中
        for (MonitorData data : result.getMonitorDataList()) {
            table.row(
                    DateUtils.formatDateTime(data.getTimestamp()),  // 格式化时间戳
                    data.getClassName(),  // 类名
                    data.getMethodName(),  // 方法名
                    "" + data.getTotal(),  // 总调用次数
                    "" + data.getSuccess(),  // 成功次数
                    "" + data.getFailed(),  // 失败次数
                    df.format(div(data.getCost(), data.getTotal())),  // 平均响应时间
                    df.format(100.0d * div(data.getFailed(), data.getTotal())) + "%"  // 失败率
            );
        }

        // 渲染表格并输出到命令行
        process.write(RenderUtil.render(table, process.width()) + "\n");

    }

    /**
     * 安全除法运算
     *
     * 执行除法运算，当除数为0时返回0，避免抛出 ArithmeticException 异常。
     * 该方法用于计算平均响应时间和失败率等需要除法的场景。
     *
     * @param a 被除数
     * @param b 除数
     * @return 除法结果，当除数为0时返回0
     */
    private double div(double a, double b) {
        if (b == 0) {
            return 0;
        }
        return a / b;
    }
}
