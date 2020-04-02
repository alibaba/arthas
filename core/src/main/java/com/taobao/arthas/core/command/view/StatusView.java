package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.StatusResult;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class StatusView extends ResultView<StatusResult> {

    @Override
    public void draw(CommandProcess process, StatusResult result) {
        writeln(process, result.getMessage());
    }

}
