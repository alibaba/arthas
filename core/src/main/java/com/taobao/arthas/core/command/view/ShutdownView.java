package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ShutdownModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/6/22
 */
public class ShutdownView extends ResultView<ShutdownModel> {
    @Override
    public void draw(CommandProcess process, ShutdownModel result) {
        process.write(result.getMessage()).write("\n");
    }
}
