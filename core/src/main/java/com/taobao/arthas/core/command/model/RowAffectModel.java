package com.taobao.arthas.core.command.model;

import com.taobao.arthas.core.util.affect.RowAffect;

/**
 * @author gongdewei 2020/4/8
 */
public class RowAffectModel extends ResultModel {
    private RowAffect affect;

    public RowAffectModel() {
    }

    public RowAffectModel(RowAffect affect) {
        this.affect = affect;
    }

    @Override
    public String getType() {
        return "row_affect";
    }

    public int getRowCount() {
        return affect.rCnt();
    }

    public RowAffect affect() {
        return affect;
    }
}
