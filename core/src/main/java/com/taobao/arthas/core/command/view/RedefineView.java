package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.RedefineModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/16
 */
public class RedefineView extends ResultView<RedefineModel> {

    @Override
    public void draw(CommandProcess process, RedefineModel result) {
        StringBuilder sb = new StringBuilder();
        for (String aClass : result.getRedefinedClasses()) {
            sb.append(aClass).append("\n");
        }
        process.write("redefine success, size: " + result.getRedefinitionCount())
                .write(", classes:\n")
                .write(sb.toString());
    }

}
