package com.taobao.arthas.core.command.model;

/**
 * @author gongdewei 2020/4/29
 */
public class TraceModel extends ResultModel {
    private TraceNode root;

    public TraceModel() {
    }

    public TraceModel(TraceNode root) {
        this.root = root;
    }

    @Override
    public String getType() {
        return "trace";
    }

    public TraceNode getRoot() {
        return root;
    }

    public void setRoot(TraceNode root) {
        this.root = root;
    }
}
