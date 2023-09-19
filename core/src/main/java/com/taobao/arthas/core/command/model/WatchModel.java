package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * Watch command result model
 *
 * @author gongdewei 2020/03/26
 */
public class WatchModel extends ResultModel {

    private LocalDateTime ts;
    private double cost;
    private ObjectVO value;

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

    public LocalDateTime getTs() {
        return ts;
    }

    public void setTs(LocalDateTime ts) {
        this.ts = ts;
    }

    public double getCost() {
        return cost;
    }

    public ObjectVO getValue() {
        return value;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public void setValue(ObjectVO value) {
        this.value = value;
    }

    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
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
