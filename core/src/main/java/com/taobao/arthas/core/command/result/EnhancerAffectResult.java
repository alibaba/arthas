package com.taobao.arthas.core.command.result;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.affect.EnhancerAffect;

public class EnhancerAffectResult extends ExecResult {
    private EnhancerAffect affect;

    public EnhancerAffectResult(EnhancerAffect affect) {
        this.affect = affect;
    }

    public int getClassCount() {
        return affect.cCnt();
    }

    public int getMethodCount() {
        return affect.mCnt();
    }

    @Override
    public String getType() {
        return "EnhancerAffect";
    }

    @Override
    protected void write(CommandProcess process) {
        writeln(process, affect+"");
    }
}
