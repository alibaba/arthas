package com.taobao.arthas.core.command.result;

import com.taobao.arthas.core.util.affect.EnhancerAffect;

public class EnhancerAffectResult extends ExecResult {
    private EnhancerAffect affect;

    public EnhancerAffectResult() {
    }

    public EnhancerAffectResult(EnhancerAffect affect) {
        this.affect = affect;
    }

    public int getClassCount() {
        return affect.cCnt();
    }

    public int getMethodCount() {
        return affect.mCnt();
    }

    public EnhancerAffect affect() {
        return affect;
    }

    @Override
    public String getType() {
        return "EnhancerAffect";
    }

}
