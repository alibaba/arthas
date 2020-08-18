package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/29
 */
public class ObjectVO {
    private String name;
    private Object value;

    public ObjectVO(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public ObjectVO(Object value) {
        this.value = value;
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
}
