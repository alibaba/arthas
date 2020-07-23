package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.MonitorModel;
import com.taobao.arthas.core.command.monitor200.MonitorData;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.text.Decoration;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.taobao.text.ui.Element.label;

/**
 * Term view for MonitorModel
 * @author gongdewei 2020/4/28
 */
public class MonitorView extends ResultView<MonitorModel> {
    @Override
    public void draw(CommandProcess process, MonitorModel result) {
        TableElement table = new TableElement(2, 3, 3, 1, 1, 1, 1, 1).leftCellPadding(1).rightCellPadding(1);
        table.row(true, label("timestamp").style(Decoration.bold.bold()),
                label("class").style(Decoration.bold.bold()),
                label("method").style(Decoration.bold.bold()),
                label("total").style(Decoration.bold.bold()),
                label("success").style(Decoration.bold.bold()),
                label("fail").style(Decoration.bold.bold()),
                label("avg-rt(ms)").style(Decoration.bold.bold()),
                label("fail-rate").style(Decoration.bold.bold()));

        final DecimalFormat df = new DecimalFormat("0.00");

        for (MonitorData data : result.getMonitorDataList()) {
            table.row(
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),
                    data.getClassName(),
                    data.getMethodName(),
                    "" + data.getTotal(),
                    "" + data.getSuccess(),
                    "" + data.getFailed(),
                    df.format(div(data.getCost(), data.getTotal())),
                    df.format(100.0d * div(data.getFailed(), data.getTotal())) + "%"
            );
        }

        process.write(RenderUtil.render(table, process.width()) + "\n");

    }

    private double div(double a, double b) {
        if (b == 0) {
            return 0;
        }
        return a / b;
    }
}
