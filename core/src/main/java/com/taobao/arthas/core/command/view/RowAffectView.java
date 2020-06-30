package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/8
 */
public class RowAffectView extends ResultView<RowAffectModel> {
    @Override
    public void draw(CommandProcess process, RowAffectModel result) {
        process.write(result.affect() + "\n");
    }
}
