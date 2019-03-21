package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LocalVariableNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

/**
 * invoke 的返回值
 * @author hengyunabc
 *
 */
public class InvokeReturnBinding extends Binding {


    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AbstractInsnNode insnNode = bindingContext.getLocation().getInsnNode();
        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
        if (insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            String uniqueNameForMethod = AsmUtils.uniqueNameForMethod(methodInsnNode.owner, methodInsnNode.name,
                    methodInsnNode.desc);
            Type invokeReturnType = Type.getMethodType(methodInsnNode.desc).getReturnType();
            if(invokeReturnType.equals(Type.VOID_TYPE)) {
                AsmOpUtils.push(instructions, null);
            }else {
                LocalVariableNode invokeReturnVariableNode = methodProcessor.initInvokeReturnVariableNode(
                        uniqueNameForMethod, Type.getMethodType(methodInsnNode.desc).getReturnType());
                AsmOpUtils.loadVar(instructions, invokeReturnType, invokeReturnVariableNode.index);
            }
        } else {
            throw new IllegalArgumentException(
                    "InvokeReturnBinding location is not MethodInsnNode, insnNode: " + insnNode);
        }

    }

    @Override
    public boolean fromStack() {
        return true;
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        AbstractInsnNode insnNode = bindingContext.getLocation().getInsnNode();
        if (insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            Type invokeReturnType = Type.getMethodType(methodInsnNode.desc).getReturnType();
            return invokeReturnType;
        } else {
            throw new IllegalArgumentException(
                    "InvokeReturnBinding location is not MethodInsnNode, insnNode: " + insnNode);
        }
    }

}
