package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.SystemEnvModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/4/2
 */
public class SystemEnvView extends ResultView<SystemEnvModel> {

    @Override
    public void draw(CommandProcess process, SystemEnvModel result) {
        process.write(ViewRenderUtil.renderKeyValueTable(result.getEnv(), process.width()));
    }

}
