package com.taobao.arthas.core.command.model;


/**
 * VO for PerfCounterCommand
 *
 * @author gongdewei 2020/4/27
 */
public class PerfCounterVO {

    private String name;
    private String units;
    private String variability;
    private Object value;

    public PerfCounterVO() {
    }

    public PerfCounterVO(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public PerfCounterVO(String name, String units, String variability, Object value) {
        this.name = name;
        this.units = units;
        this.variability = variability;
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public void setVariability(String variability) {
        this.variability = variability;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getUnits() {
        return units;
    }

    public String getVariability() {
        return variability;
    }

    public Object getValue() {
        return value;
    }
}
