package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SystemPropertyModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/2
 */
public class SystemPropertyView extends ResultView<SystemPropertyModel> {

    @Override
    public void draw(CommandProcess process, SystemPropertyModel result) {
        process.write(ViewRenderUtil.renderKeyValueTable(result.getProps(), process.width()));
    }

}
