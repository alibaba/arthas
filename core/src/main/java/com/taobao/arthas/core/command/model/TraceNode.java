package com.taobao.arthas.core.command.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gongdewei 2020/4/28
 */
public abstract class TraceNode {
    protected Integer nodeId;
    protected TraceNode parent;
    protected List<TraceNode> children ;//= new ArrayList<TraceNode>();
    protected Map<Integer, TraceNode> childrenMap = new HashMap<Integer, TraceNode>();
    /**
     * 备注
     */
    private String mark;
    /**
     * TODO marks数量的作用？是否可以去掉
     */
    private int marks = 0;

    public TraceNode findChild(Integer nodeId) {
        return childrenMap.get(nodeId);
    }

    public void addChild(TraceNode child) {
        if (children == null) {
            children = new ArrayList<TraceNode>();
        }
        this.children.add(child);
        this.childrenMap.put(child.nodeId, child);
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

    public TraceNode parent() {
        return parent;
    }

    public void setParent(TraceNode parent) {
        this.parent = parent;
    }

    public Integer nodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public List<TraceNode> getChildren() {
        return children;
    }
}
