package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.TimeFragmentVO;
import com.taobao.arthas.core.command.model.TimeTunnelModel;
import com.taobao.arthas.core.command.monitor200.TimeTunnelTable;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.text.ui.Element;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import static com.taobao.arthas.core.command.monitor200.TimeTunnelTable.*;
import static java.lang.String.format;

/**
 * TimeTunnel（时间隧道）命令的终端视图类
 * 用于记录和回放方法调用，支持以下功能：
 * 1. 列出所有记录的时间片段（tt -l）
 * 2. 查看指定索引的时间片段详情（tt -i <index>）
 * 3. 监控单个时间片段的表达式（tt -i <index> -w <expression>）
 * 4. 搜索并监控时间片段（tt -s <condition> -w <expression>）
 * 5. 重放时间片段（tt -i <index> -p）
 *
 * @author gongdewei 2020/4/27
 */
public class TimeTunnelView extends ResultView<TimeTunnelModel> {

    /**
     * 绘制TimeTunnel视图
     * 根据不同的命令模式显示不同类型的信息
     *
     * @param process 命令处理进程，用于输出结果
     * @param timeTunnelModel TimeTunnel模型对象，包含时间片段及相关数据
     */
    @Override
    public void draw(CommandProcess process, TimeTunnelModel timeTunnelModel) {
        // 标准化对象最大显示长度限制
        int sizeLimitValue = ObjectView.normalizeMaxObjectLength(timeTunnelModel.getSizeLimit());

        // 场景1: 显示时间片段列表（tt -l 或 tt -t）
        if (timeTunnelModel.getTimeFragmentList() != null) {
            // 绘制时间片段列表表格，显示索引、时间戳、类名、方法名、耗时等信息
            Element table = drawTimeTunnelTable(timeTunnelModel.getTimeFragmentList(), timeTunnelModel.getFirst());
            process.write(RenderUtil.render(table, process.width()));

        // 场景2: 显示单个时间片段的详细信息（tt -i <index>）
        } else if (timeTunnelModel.getTimeFragment() != null) {
            TimeFragmentVO tf = timeTunnelModel.getTimeFragment();
            TableElement table = TimeTunnelTable.createDefaultTable();
            // 绘制时间片段的基本信息（类名、方法名、耗时等）
            TimeTunnelTable.drawTimeTunnel(table, tf);
            // 绘制方法参数信息
            TimeTunnelTable.drawParameters(table, tf.getParams());
            // 绘制返回对象信息（如果有）
            TimeTunnelTable.drawReturnObj(table, tf, sizeLimitValue);
            // 绘制抛出的异常信息（如果有）
            TimeTunnelTable.drawThrowException(table, tf);
            process.write(RenderUtil.render(table, process.width()));

        // 场景3: 监控单个时间片段的表达式值（tt -i <index> -w '<expression>'）
        } else if (timeTunnelModel.getWatchValue() != null) {
            ObjectVO valueVO = timeTunnelModel.getWatchValue();
            // 判断是否需要展开显示对象（复杂对象需要展开）
            if (valueVO.needExpand()) {
                // 使用对象视图绘制复杂对象
                process.write(new ObjectView(sizeLimitValue, valueVO).draw()).write("\n");
            } else {
                // 简单对象直接转换为字符串输出
                process.write(StringUtils.objectToString(valueVO.getObject())).write("\n");
            }

        // 场景4: 搜索并监控表达式值（tt -s '<condition>' -w '<expression>'）
        } else if (timeTunnelModel.getWatchResults() != null) {
            TableElement table = TimeTunnelTable.createDefaultTable();
            // 绘制监控表格的表头
            TimeTunnelTable.drawWatchTableHeader(table);
            // 绘制所有符合条件的监控结果
            TimeTunnelTable.drawWatchResults(table, timeTunnelModel.getWatchResults(), sizeLimitValue);
            process.write(RenderUtil.render(table, process.width()));

        // 场景5: 重放时间片段（tt -i <index> -p）
        } else if (timeTunnelModel.getReplayResult() != null) {
            TimeFragmentVO replayResult = timeTunnelModel.getReplayResult();
            Integer replayNo = timeTunnelModel.getReplayNo();
            TableElement table = TimeTunnelTable.createDefaultTable();
            // 绘制重放操作的头部信息（类名、方法名、对象、索引）
            TimeTunnelTable.drawPlayHeader(replayResult.getClassName(), replayResult.getMethodName(), replayResult.getObject(), replayResult.getIndex(), table);
            // 绘制重放的参数信息
            TimeTunnelTable.drawParameters(table, replayResult.getParams());
            // 根据重放结果显示不同的内容
            if (replayResult.isReturn()) {
                // 正常返回：绘制返回结果和耗时
                TimeTunnelTable.drawPlayResult(table, replayResult.getReturnObj(), sizeLimitValue, replayResult.getCost());
            } else {
                // 异常情况：绘制异常信息
                TimeTunnelTable.drawPlayException(table, replayResult.getThrowExp());
            }
            // 输出重放成功的提示信息
            process.write(RenderUtil.render(table, process.width()))
                    .write(format("Time fragment[%d] successfully replayed %d times.", replayResult.getIndex(), replayNo))
                    .write("\n\n");
        }
    }

}
