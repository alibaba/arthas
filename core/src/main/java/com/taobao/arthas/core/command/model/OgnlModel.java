package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/29
 */
public class OgnlModel extends ResultModel {
    private Object value;
    private int expand = 1;


    @Override
    public String getType() {
        return "ognl";
    }

    public Object getValue() {
        return value;
    }

    public OgnlModel setValue(Object value) {
        this.value = value;
        return this;
    }

    public int getExpand() {
        return expand;
    }

    public OgnlModel setExpand(int expand) {
        this.expand = expand;
        return this;
    }
}
