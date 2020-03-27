package com.taobao.arthas.core.command.view;

import com.taobao.arthas.core.command.result.EnhancerAffectResult;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author gongdewei 2020/3/27
 */
public class EnhancerAffectView extends ResultView<EnhancerAffectResult> {

    @Override
    public void draw(CommandProcess process, EnhancerAffectResult result) {
        writeln(process, result.affect() + "");
    }

}
