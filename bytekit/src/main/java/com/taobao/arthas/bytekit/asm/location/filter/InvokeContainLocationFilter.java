package com.taobao.arthas.bytekit.asm.location.filter;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.taobao.arthas.bytekit.asm.location.LocationType;

/**
 * 
 * 检查整个method里，是否有某个函数调用。用于检查 enter/exit/exception exit
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class InvokeContainLocationFilter implements LocationFilter {

    private String owner;
    private String methodName;
    private LocationType locationType;

    public InvokeContainLocationFilter(String owner, String methodName, LocationType locationType) {
        this.owner = owner;
        this.methodName = methodName;
        this.locationType = locationType;
    }

    @Override
    public boolean allow(AbstractInsnNode insnNode, LocationType locationType, boolean complete) {
        // 只检查自己对应的 LocationType
        if (!this.locationType.equals(locationType)) {
            return false;
        }

        MethodInsnNode methodInsnNode = findMethodInsnNode(insnNode);
        if (methodInsnNode != null) {
            if (methodInsnNode.owner.equals(this.owner) && methodInsnNode.name.equals(this.methodName)) {
                return false;
            }
        }

        return true;
    }

    private MethodInsnNode findMethodInsnNode(AbstractInsnNode insnNode) {

        AbstractInsnNode current = insnNode;
        while (current != null) {
            current = current.getNext();
            if (current instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) current;
                if (methodInsnNode.owner.equals(this.owner) && methodInsnNode.name.equals(this.methodName)) {
                    return methodInsnNode;
                }
            }
        }
        current = insnNode;
        while (current != null) {
            current = current.getPrevious();
            if (current instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) current;
                if (methodInsnNode.owner.equals(this.owner) && methodInsnNode.name.equals(this.methodName)) {
                    return methodInsnNode;
                }
            }
        }
        return null;
    }

}
