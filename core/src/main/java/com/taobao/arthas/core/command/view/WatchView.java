package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.WatchResult;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class WatchView extends ResultView<WatchResult> {

    @Override
    public void draw(CommandProcess process, WatchResult result) {
        process.write("ts=" + result.getTs() + "; [cost=" + result.getCost() + "ms] result=" + result.getResult() + "\n");
    }

}
