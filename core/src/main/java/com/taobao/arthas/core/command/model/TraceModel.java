package com.taobao.arthas.core.command.model;

/**
 * Data model of TraceCommand
 * @author gongdewei 2020/4/29
 */
public class TraceModel extends ResultModel {
    private TraceNode root;
    private int nodeCount;
    private String tag;

    public TraceModel() {
    }

    public TraceModel(TraceNode root, int nodeCount) {
        this.root = root;
        this.nodeCount = nodeCount;
    }

    public TraceModel(TraceNode root, int nodeCount, String tag) {
        this.root = root;
        this.nodeCount = nodeCount;
        this.tag = tag;
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

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
