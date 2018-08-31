package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;

import java.util.Date;

/**
 * 时间碎片
 */
class TimeFragment {

    public TimeFragment(Advice advice, Date gmtCreate, double cost) {
        this.advice = advice;
        this.gmtCreate = gmtCreate;
        this.cost = cost;
    }

    private final Advice advice;
    private final Date gmtCreate;
    private final double cost;

    public Advice getAdvice() {
        return advice;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public double getCost() {
        return cost;
    }
}
