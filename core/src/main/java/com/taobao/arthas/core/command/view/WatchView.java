package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class WatchView extends ResultView<WatchModel> {

    @Override
    public void draw(CommandProcess process, WatchModel result) {
        process.write("ts=" + result.getTs() + "; [cost=" + result.getCost() + "ms] result=" + result.getResult() + "\n");
    }

}
