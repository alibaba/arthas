package com.taobao.arthas.core.command.model;

/**
 * MBean attribute
 *
 * @author gongdewei 2020/4/26
 */
public class MBeanAttributeVO {
    private String name;
    private Object value;
    private String error;

    public MBeanAttributeVO(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public MBeanAttributeVO(String name, Object value, String error) {
        this.name = name;
        this.value = value;
        this.error = error;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
