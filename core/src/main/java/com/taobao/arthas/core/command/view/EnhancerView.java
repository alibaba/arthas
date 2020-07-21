package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Term view for EnhancerModel
 * @author gongdewei 2020/7/21
 */
public class EnhancerView extends ResultView<EnhancerModel> {
    @Override
    public void draw(CommandProcess process, EnhancerModel result) {
        if (result.isSuccess()) {
            process.write("Classes enhanced successfully.\n");
        } else {
            process.write("Classes enhanced failed.\n");
        }
        if (result.getEffect() != null) {
            process.write(ViewRenderUtil.renderEnhancerAffect(result.getEffect()));
        }
    }
}
