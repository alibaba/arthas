package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.StatusResult;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class StatusView extends ResultView<StatusResult> {

    @Override
    public void draw(CommandProcess process, StatusResult result) {
        if (result.getMessage() != null) {
            writeln(process, result.getMessage());
        }
    }

}
