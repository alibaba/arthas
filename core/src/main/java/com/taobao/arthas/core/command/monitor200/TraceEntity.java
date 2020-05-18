package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.model.TraceModel;
import com.taobao.arthas.core.command.model.TraceTree;
import com.taobao.arthas.core.util.ThreadUtil;

import java.util.Map;

/**
 * 用于在ThreadLocal中传递的实体
 * @author ralf0131 2017-01-05 14:05.
 */
public class TraceEntity {

    protected TraceTree tree;
    protected int deep;

    public TraceEntity(Map<String, String> normalizeClassNameMap) {
        this.tree = createTraceTree(normalizeClassNameMap);
        this.deep = 0;
    }

    public int getDeep() {
        return deep;
    }

    public void setDeep(int deep) {
        this.deep = deep;
    }

    private TraceTree createTraceTree(Map<String, String> normalizeClassNameMap) {
        return new TraceTree(ThreadUtil.getThreadNode(Thread.currentThread()), normalizeClassNameMap);
    }

    public ResultModel getModel() {
        tree.normalizeClassName(tree.getRoot());
        return new TraceModel(tree.getRoot(), tree.getNodeCount());
    }
}
