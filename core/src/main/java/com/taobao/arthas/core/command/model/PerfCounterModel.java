package com.taobao.arthas.core.command.model;

import java.util.List;

/**
 * Model of 'perfcounter'
 *
 * @author gongdewei 2020/4/27
 */
public class PerfCounterModel extends ResultModel {
    private List<PerfCounterVO> perfCounters;
    private boolean details;

    public PerfCounterModel() {
    }

    public PerfCounterModel(List<PerfCounterVO> perfCounters, boolean details) {
        this.perfCounters = perfCounters;
        this.details = details;
    }

    @Override
    public String getType() {
        return "perfcounter";
    }

    public List<PerfCounterVO> getPerfCounters() {
        return perfCounters;
    }

    public void setPerfCounters(List<PerfCounterVO> perfCounters) {
        this.perfCounters = perfCounters;
    }

    public boolean isDetails() {
        return details;
    }

    public void setDetails(boolean details) {
        this.details = details;
    }
}
