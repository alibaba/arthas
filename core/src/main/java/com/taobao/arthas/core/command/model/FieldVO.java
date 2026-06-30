package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/8
 */
public class FieldVO {
    private String name;
    private String type;
    private String modifier;
    private String[] annotations;
    private ObjectVO value;
    private boolean isStatic;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModifier() {
        return modifier;
    }

    public void setModifier(String modifier) {
        this.modifier = modifier;
    }

    public ObjectVO getValue() {
        return value;
    }

    public void setValue(ObjectVO value) {
        this.value = value;
    }

    public String[] getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String[] annotations) {
        this.annotations = annotations;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setStatic(boolean aStatic) {
        isStatic = aStatic;
    }

}
