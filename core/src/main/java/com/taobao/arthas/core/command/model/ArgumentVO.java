package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/3
 */
public class ArgumentVO {
    private String argName;
    private boolean required;
    private boolean multiValued;

    public ArgumentVO() {
    }

    public ArgumentVO(String argName, boolean required, boolean multiValued) {
        this.argName = argName;
        this.required = required;
        this.multiValued = multiValued;
    }

    public String getArgName() {
        return argName;
    }

    public void setArgName(String argName) {
        this.argName = argName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public void setMultiValued(boolean multiValued) {
        this.multiValued = multiValued;
    }
}
