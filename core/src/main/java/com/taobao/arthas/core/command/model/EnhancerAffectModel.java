package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.affect.EnhancerAffect;

public class EnhancerAffectModel extends ResultModel {
    private EnhancerAffect affect;

    public EnhancerAffectModel() {
    }

    public EnhancerAffectModel(EnhancerAffect affect) {
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
        return "enhancer_affect";
    }

}
