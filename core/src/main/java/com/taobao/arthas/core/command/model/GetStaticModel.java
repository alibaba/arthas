package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/20
 */
public class GetStaticModel extends ResultModel {

    private String fieldName;
    private Object fieldValue;
    //only for view
    private transient int expand;

    public GetStaticModel() {
    }

    public GetStaticModel(String fieldName, Object fieldValue, int expand) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.expand = expand;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(Object fieldValue) {
        this.fieldValue = fieldValue;
    }

    public int expand() {
        return expand;
    }

    public void setExpand(int expand) {
        this.expand = expand;
    }

    @Override
    public String getType() {
        return "getstatic";
    }
}
