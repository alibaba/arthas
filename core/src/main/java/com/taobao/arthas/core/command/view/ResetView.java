package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.ResetModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/6/22
 */
public class ResetView extends ResultView<ResetModel> {

    @Override
    public void draw(CommandProcess process, ResetModel result) {
        process.write(ViewRenderUtil.renderEnhancerAffect(result.getAffect()));
    }

}
