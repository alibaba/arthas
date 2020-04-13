package com.taobao.arthas.core.command.model;

/**
 * Watch command result model
 *
 * @author gongdewei 2020.03.26
 */
public class WatchModel extends ResultModel {

    private String ts;
    private double cost;
    private Object value;

    private Integer expand;
    private Integer sizeLimit;

    public WatchModel() {
    }

    @Override
    public String getType() {
        return "watch";
    }

    public String getTs() {
        return ts;
    }

    public double getCost() {
        return cost;
    }

    public Object getValue() {
        return value;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    public Integer expand() {
        return expand;
    }

    public Integer sizeLimit() {
        return sizeLimit;
    }
}
