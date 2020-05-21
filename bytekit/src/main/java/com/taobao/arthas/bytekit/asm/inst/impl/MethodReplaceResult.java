package com.taobao.arthas.bytekit.asm.inst.impl;

import com.alibaba.arthas.deps.org.objectweb.asm.Label;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;

/**
 *
 * @author hengyunabc 2019-03-18
 *
 */
public class MethodReplaceResult {

    private boolean success;

    private Label start;
    private Label end;

    private MethodNode methodNode;

    public Label getStart() {
        return start;
    }

    public void setStart(Label start) {
        this.start = start;
    }

    public Label getEnd() {
        return end;
    }

    public void setEnd(Label end) {
        this.end = end;
    }

    public MethodNode getMethodNode() {
        return methodNode;
    }

    public void setMethodNode(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

}
