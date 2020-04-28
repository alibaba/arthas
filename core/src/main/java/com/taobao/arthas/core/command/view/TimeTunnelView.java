package com.taobao.arthas.core.command.view;

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
 * Term view for TimeTunnelCommand
 * @author gongdewei 2020/4/27
 */
public class TimeTunnelView extends ResultView<TimeTunnelModel> {

    @Override
    public void draw(CommandProcess process, TimeTunnelModel timeTunnelModel) {
        Integer expand = timeTunnelModel.getExpand();
        boolean isNeedExpand = isNeedExpand(expand);
        Integer sizeLimit = timeTunnelModel.getSizeLimit();

        if (timeTunnelModel.getTimeFragmentList() != null) {
            //show list table: tt -l / tt -t
            Element table = drawTimeTunnelTable(timeTunnelModel.getTimeFragmentList(), timeTunnelModel.getFirst());
            process.write(RenderUtil.render(table, process.width()));

        } else if (timeTunnelModel.getTimeFragment() != null) {
            //show detail of single TimeFragment: tt -i 1000
            TimeFragmentVO tf = timeTunnelModel.getTimeFragment();
            TableElement table = TimeTunnelTable.createDefaultTable();
            TimeTunnelTable.drawTimeTunnel(table, tf);
            TimeTunnelTable.drawParameters(table, tf.getParams(), isNeedExpand, expand);
            TimeTunnelTable.drawReturnObj(table, tf, isNeedExpand, expand, sizeLimit);
            TimeTunnelTable.drawThrowException(table, tf, isNeedExpand, expand);
            process.write(RenderUtil.render(table, process.width()));

        } else if (timeTunnelModel.getWatchValue() != null) {
            //watch single TimeFragment: tt -i 1000 -w 'params'
            Object value = timeTunnelModel.getWatchValue();
            if (isNeedExpand) {
                process.write(new ObjectView(value, expand, sizeLimit).draw()).write("\n");
            } else {
                process.write(StringUtils.objectToString(value)).write("\n");
            }

        } else if (timeTunnelModel.getWatchResults() != null) {
            //search & watch: tt -s 'returnObj!=null' -w 'returnObj'
            TableElement table = TimeTunnelTable.createDefaultTable();
            TimeTunnelTable.drawWatchTableHeader(table);
            TimeTunnelTable.drawWatchResults(table, timeTunnelModel.getWatchResults(), isNeedExpand, expand, sizeLimit);
            process.write(RenderUtil.render(table, process.width()));

        } else if (timeTunnelModel.getReplayResult() != null) {
            //replay: tt -i 1000 -p
            TimeFragmentVO replayResult = timeTunnelModel.getReplayResult();
            Integer replayNo = timeTunnelModel.getReplayNo();
            TableElement table = TimeTunnelTable.createDefaultTable();
            TimeTunnelTable.drawPlayHeader(replayResult.getClassName(), replayResult.getMethodName(), replayResult.getObject(), replayResult.getIndex(), table);
            TimeTunnelTable.drawParameters(table, replayResult.getParams(), isNeedExpand, expand);
            if (replayResult.isReturn()) {
                TimeTunnelTable.drawPlayResult(table, replayResult.getReturnObj(), isNeedExpand, expand, sizeLimit, replayResult.getCost());
            } else {
                TimeTunnelTable.drawPlayException(table, replayResult.getThrowExp(), isNeedExpand, expand);
            }
            process.write(RenderUtil.render(table, process.width()))
                    .write(format("Time fragment[%d] successfully replayed %d times.", replayResult.getIndex(), replayNo))
                    .write("\n\n");
        }
    }

    private boolean isNeedExpand(Integer expand) {
        return null != expand && expand > 0;
    }

}
