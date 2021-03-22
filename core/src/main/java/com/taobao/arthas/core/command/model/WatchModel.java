package com.taobao.arthas.core.command.model;

import java.util.Date;

/**
 * Watch command result model
 *
 * @author gongdewei 2020/03/26
 */
public class WatchModel extends ResultModel {

    private Date ts;
    private double cost;
    private Object value;

    private Integer expand;
    private Integer sizeLimit;
    private String className;
    private String methodName;
    private String accessPoint;

    public WatchModel() {
    }

    @Override
    public String getType() {
        return "watch";
    }

    public Date getTs() {
        return ts;
    }

    public void setTs(Date ts) {
        this.ts = ts;
    }

    public double getCost() {
        return cost;
    }

    public Object getValue() {
        return value;
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

    public Integer getExpand() {
        return expand;
    }

    public Integer getSizeLimit() {
        return sizeLimit;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(String accessPoint) {
        this.accessPoint = accessPoint;
    }
}
