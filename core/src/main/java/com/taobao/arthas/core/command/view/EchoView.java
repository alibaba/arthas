package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.EchoModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/5/11
 */
public class EchoView extends ResultView<EchoModel> {
    @Override
    public void draw(CommandProcess process, EchoModel result) {
        process.write(result.getContent()).write("\n");
    }
}
