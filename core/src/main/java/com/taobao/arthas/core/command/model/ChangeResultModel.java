package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/16
 */
public class ChangeResultModel extends ResultModel {
    private String name;
    private String beforeValue;
    private String afterValue;

    public ChangeResultModel() {
    }

    public ChangeResultModel(String name, String beforeValue, String afterValue) {
        this.name = name;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
    }

    @Override
    public String getType() {
        return "change_result";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }

}

