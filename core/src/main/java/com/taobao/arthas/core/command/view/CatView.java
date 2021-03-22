package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.CatModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Result view for CatCommand
 * @author gongdewei 2020/5/11
 */
public class CatView extends ResultView<CatModel> {

    @Override
    public void draw(CommandProcess process, CatModel result) {
        process.write(result.getContent()).write("\n");
    }

}
