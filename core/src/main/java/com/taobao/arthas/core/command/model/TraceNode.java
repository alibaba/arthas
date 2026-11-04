package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract Node of TraceCommand
 * @author gongdewei 2020/4/28
 */
public abstract class TraceNode {

    protected TraceNode parent;
    protected List<TraceNode> children;

    /**
     * node type: method,
     */
    private String type;

    /**
     * 备注
     */
    private String mark;
    /**
     * TODO marks数量的作用？是否可以去掉
     */
    private int marks = 0;

    public TraceNode(String type) {
        this.type = type;
    }

    public void addChild(TraceNode child) {
        if (children == null) {
            children = new ArrayList<TraceNode>();
        }
        this.children.add(child);
        child.setParent(this);
    }

    public void setMark(String mark) {
        this.mark = mark;
        marks++;
    }

    public String getMark() {
        return mark;
    }

    public Integer marks() {
        return marks;
    }

    public void begin() {
    }

    public void end() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TraceNode parent() {
        return parent;
    }

    public void setParent(TraceNode parent) {
        this.parent = parent;
    }

    public List<TraceNode> getChildren() {
        return children;
    }
}
