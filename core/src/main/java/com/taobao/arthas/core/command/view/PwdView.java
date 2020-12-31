package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.PwdModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/5/11
 */
public class PwdView extends ResultView<PwdModel> {
    @Override
    public void draw(CommandProcess process, PwdModel result) {
        process.write(result.getWorkingDir()).write("\n");
    }
}
