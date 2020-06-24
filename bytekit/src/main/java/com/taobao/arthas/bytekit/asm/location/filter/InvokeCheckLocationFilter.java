package com.taobao.arthas.bytekit.asm.location.filter;

import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.taobao.arthas.bytekit.asm.location.LocationType;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

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
                    if (isIgnoredMethod(methodInsnNode)){
                        continue;
                    }
                    return methodInsnNode;
                }
            }
        } else {
            while (insnNode != null) {
                insnNode = insnNode.getPrevious();
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    if (isIgnoredMethod(methodInsnNode)){
                        continue;
                    }
                    return methodInsnNode;
                }
            }
        }
        return null;
    }

    private boolean isIgnoredMethod(MethodInsnNode methodInsnNode) {
        //过滤unbox的方法调用，解决invokeBefore判断不准确的问题（保存invoke args时可能进行box转换，额外引入了unbox方法）
        if (AsmOpUtils.isUnBoxMethod(methodInsnNode)) {
            return true;
        }
        return false;
    }

}
