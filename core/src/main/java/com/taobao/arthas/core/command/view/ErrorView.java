package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.ErrorResult;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class ErrorView extends ResultView<ErrorResult> {

    @Override
    public void draw(CommandProcess process, ErrorResult result) {
        writeln(process, result.getMessage());
    }

}
