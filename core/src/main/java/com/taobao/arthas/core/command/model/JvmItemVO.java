package com.taobao.arthas.core.command.model;

/**
 * Key/value/desc
 * @author gongdewei 2020/4/24
 */
public class JvmItemVO {
    private String name;
    private Object value;
    private String desc;

    public JvmItemVO(String name, Object value, String desc) {
        this.name = name;
        this.value = value;
        this.desc = desc;
    }

    public JvmItemVO(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
