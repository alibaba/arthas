package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/16
 */
public class ChangeResultVO {
    private String name;
    private Object beforeValue;
    private Object afterValue;

    public ChangeResultVO() {
    }

    public ChangeResultVO(String name, Object beforeValue, Object afterValue) {
        this.name = name;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(Object beforeValue) {
        this.beforeValue = beforeValue;
    }

    public Object getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(Object afterValue) {
        this.afterValue = afterValue;
    }
}

