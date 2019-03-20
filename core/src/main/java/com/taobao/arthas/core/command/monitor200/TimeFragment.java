package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;

import java.util.Date;

/**
 * 时间碎片
 */
class TimeFragment {

    public TimeFragment(Advice advice, Date gmtCreate, double cost, String stack) {
        this.advice = advice;
        this.gmtCreate = gmtCreate;
        this.cost = cost;
        this.stack = stack;
    }

    private final Advice advice;
    private final Date gmtCreate;
    private final double cost;
    private final String stack;

    public Advice getAdvice() {
        return advice;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public double getCost() {
        return cost;
    }

    public String getStack() {
        return stack;
    }
}
