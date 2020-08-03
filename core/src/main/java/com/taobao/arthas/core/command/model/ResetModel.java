package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.affect.EnhancerAffect;

/**
 * @author gongdewei 2020/6/22
 */
public class ResetModel extends ResultModel {

    private EnhancerAffectVO affect;

    public ResetModel(EnhancerAffectVO affect) {
        this.affect = affect;
    }

    public ResetModel(EnhancerAffect affect) {
        this.affect = new EnhancerAffectVO(affect);
    }

    @Override
    public String getType() {
        return "reset";
    }

    public ResetModel affect(EnhancerAffect affect) {
        this.affect = new EnhancerAffectVO(affect);
        return this;
    }

    public EnhancerAffectVO getAffect() {
        return affect;
    }
}
