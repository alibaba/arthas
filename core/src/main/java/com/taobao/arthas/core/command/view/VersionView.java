package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.VersionResult;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class VersionView extends ResultView<VersionResult> {

    @Override
    public void draw(CommandProcess process, VersionResult result) {
        writeln(process, result.getVersion());
    }

}
