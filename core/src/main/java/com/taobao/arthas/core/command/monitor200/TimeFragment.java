package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;

import java.time.LocalDateTime;

/**
 * 时间碎片
 */
class TimeFragment {

    public TimeFragment(Advice advice, LocalDateTime gmtCreate, double cost) {
        this.advice = advice;
        this.gmtCreate = gmtCreate;
        this.cost = cost;
    }

    private final Advice advice;
    private final LocalDateTime gmtCreate;
    private final double cost;

    public Advice getAdvice() {
        return advice;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public double getCost() {
        return cost;
    }
}
