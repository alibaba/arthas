package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.view.ObjectView;

/**
 * Term view for WatchModel
 *
 * @author gongdewei 2020/3/27
 */
public class WatchView extends ResultView<WatchModel> {

    @Override
    public void draw(CommandProcess process, WatchModel model) {
        ObjectVO objectVO = model.getValue();
        String result = StringUtils.objectToString(
                objectVO.needExpand() ? new ObjectView(model.getSizeLimit(), objectVO).draw() : objectVO.getObject());

        StringBuilder sb = new StringBuilder();
        sb.append("method=").append(model.getClassName()).append(".").append(model.getMethodName())
                .append(" location=").append(model.getAccessPoint()).append("\n");
        sb.append("ts=").append(DateUtils.formatDateTime(model.getTs()))
                .append("; [cost=").append(model.getCost()).append("ms] result=").append(result).append("\n");

        process.write(sb.toString());
    }
}
