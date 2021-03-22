package com.taobao.arthas.core.command.view;

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
        Object value = model.getValue();
        String result = StringUtils.objectToString(
                isNeedExpand(model) ? new ObjectView(value, model.getExpand(), model.getSizeLimit()).draw() : value);
        process.write("method=" + model.getClassName() + "." + model.getMethodName() + " location=" + model.getAccessPoint() + "\n");
        process.write("ts=" + DateUtils.formatDate(model.getTs()) + "; [cost=" + model.getCost() + "ms] result=" + result + "\n");
    }

    private boolean isNeedExpand(WatchModel model) {
        Integer expand = model.getExpand();
        return null != expand && expand >= 0;
    }
}
