package com.taobao.arthas.bytekit.asm.location.filter;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.taobao.arthas.bytekit.asm.location.LocationType;

/**
 * 
 * 检查某个 AbstractInsnNode 的前面是否有某个函数调用，如果有，则认为这个location是已被处理过的
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class InvokeCheckLocationFilter implements LocationFilter {

    private String owner;
    private String methodName;
    private LocationType locationType;

    public InvokeCheckLocationFilter(String owner, String methodName, LocationType locationType) {
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

        MethodInsnNode methodInsnNode = findMethodInsnNode(insnNode, complete);
        if (methodInsnNode != null) {
            if (methodInsnNode.owner.equals(this.owner) && methodInsnNode.name.equals(this.methodName)) {
                return false;
            }
        }

        return true;
    }

    private MethodInsnNode findMethodInsnNode(AbstractInsnNode insnNode, boolean complete) {
        if (complete) {
            while (insnNode != null) {
                insnNode = insnNode.getNext();
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    return methodInsnNode;
                }
            }
        } else {
            while (insnNode != null) {
                insnNode = insnNode.getPrevious();
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    return methodInsnNode;
                }
            }
        }
        return null;
    }

}
