package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.model.EnhancerAffectModel;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class EnhancerAffectView extends ResultView<EnhancerAffectModel> {

    @Override
    public void draw(CommandProcess process, EnhancerAffectModel result) {
        writeln(process, result.affect() + "");
    }

}
