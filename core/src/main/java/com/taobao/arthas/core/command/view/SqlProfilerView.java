package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SqlProfilerModel;
import com.taobao.arthas.core.command.monitor200.MonitorData;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.taobao.text.ui.Element.label;

/**
 * Term view for SqlProfilerModel
 *
 * @author yangxiaobing 2021/8/4
 */
public class SqlProfilerView extends ResultView<SqlProfilerModel> {

    @Override
    public void draw(CommandProcess process, SqlProfilerModel model) {
        if (model.getTraceData() != null) {
            drawTraceView(process, model.getTs(), model.getTraceData());
        } else {
            drawMonitorView(process, model.getTs(), model.getMonitorData());
        }
    }

    private void drawMonitorView(CommandProcess process, Date ts, SqlProfilerModel.MonitorData monitorData) {
        process.write(String.format("Top %s by total cost: \n", monitorData.getTopByTotalCost().size()));
        for (int i = 0; i < monitorData.getTopByTotalCost().size(); i++) {
            SqlProfilerModel.SqlStat sqlStat = monitorData.getTopByTotalCost().get(i);
            process.write(String.format("[Top %s sql]: \n" +
                            "[count]: %s\n" +
                            "[avgCost(ms)]: %s\n" +
                            "[totalCost(ms)]: %s\n" +
                            "[success]: %s\n" +
                            "[fail]: %s\n",
                    i,
                    sqlStat.getSql(),
                    sqlStat.getCount(),
                    sqlStat.getAvgCost(),
                    sqlStat.getTotalCost(),
                    sqlStat.getSuccessCount(),
                    sqlStat.getFailedCount()));
        }

        process.write(String.format("Top %s by avg cost: \n", monitorData.getTopByAvgCost().size()));
        for (int i = 0; i < monitorData.getTopByAvgCost().size(); i++) {
            SqlProfilerModel.SqlStat sqlStat = monitorData.getTopByAvgCost().get(i);
            process.write(String.format("[Top %s sql]: %s\n" +
                            "[count]: %s\n" +
                            "[avgCost(ms)]: %s\n" +
                            "[totalCost(ms)]: %s\n" +
                            "[success]: %s\n" +
                            "[fail]: %s\n",
                    i,
                    sqlStat.getSql(),
                    sqlStat.getCount(),
                    sqlStat.getAvgCost(),
                    sqlStat.getTotalCost(),
                    sqlStat.getSuccessCount(),
                    sqlStat.getFailedCount()));
        }
    }

    private void drawTraceView(CommandProcess process, Date ts, SqlProfilerModel.TraceData model) {
        StringBuilder outputBuilder = new StringBuilder(String.format("ts=%s; [method=%s.%s] [cost=%sms]\n" +
                        "sql: %s\n" +
                        "args: ",
                DateUtils.formatDate(ts),
                model.getClassName(),
                model.getMethodName(),
                model.getCost(),
                model.getSql()));
        if (model.getBatchParams() != null) {
            outputBuilder.append("\n");
            for (List<String> batchParam : model.getBatchParams()) {
                outputBuilder.append(String.format("--(%s)\n", StringUtils.join(batchParam.toArray(), ",")));
            }
        } else if (model.getParams() != null) {
            outputBuilder.append(StringUtils.join(model.getParams().toArray(), ","));
            outputBuilder.append("\n");
        } else {
            outputBuilder.append("\n");
        }

        process.write(outputBuilder.toString());
    }
}
