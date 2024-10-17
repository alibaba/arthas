package com.taobao.arthas.core.command.model;

import java.time.LocalDateTime;

/**
 * Line command result model
 *
 */
public class LineModel extends ResultModel {

    private LocalDateTime ts;
    private ObjectVO value;

    private Integer sizeLimit;
    private String className;
    private String methodName;
    private String accessPoint;

    public LineModel() {
    }

    @Override
    public String getType() {
        return "line";
    }

    public LocalDateTime getTs() {
        return ts;
    }

    public void setTs(LocalDateTime ts) {
        this.ts = ts;
    }

    public ObjectVO getValue() {
        return value;
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
