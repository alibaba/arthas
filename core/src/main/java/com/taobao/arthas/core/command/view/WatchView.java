package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.DateUtils;
import com.taobao.arthas.core.util.object.ObjectExpandUtils;

/**
 * Term view for WatchModel
 *
 * @author gongdewei 2020/3/27
 */
public class WatchView extends ResultView<WatchModel> {

    @Override
    public void draw(CommandProcess process, WatchModel model) {
        String result = ObjectExpandUtils.toString(model.getValue());
        process.write("ts=" + DateUtils.formatDate(model.getTs()) + "; [cost=" + model.getCost() + "ms] result=" + result + "\n");
    }

}
