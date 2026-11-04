package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.VersionModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class VersionView extends ResultView<VersionModel> {

    @Override
    public void draw(CommandProcess process, VersionModel result) {
        writeln(process, result.getVersion());
    }

}
