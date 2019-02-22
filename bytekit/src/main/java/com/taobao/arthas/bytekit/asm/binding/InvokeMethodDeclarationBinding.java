package com.taobao.arthas.bytekit.asm.binding;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

/**
 * 
 * @author hengyunabc
 *
 */
public class InvokeMethodDeclarationBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AbstractInsnNode insnNode = bindingContext.getLocation().getInsnNode();
        if (insnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
            String methodDeclaration = AsmUtils.methodDeclaration(methodInsnNode);
            AsmOpUtils.push(instructions, methodDeclaration);

        } else {
            throw new IllegalArgumentException(
                    "InvokeMethodDeclarationBinding location is not MethodInsnNode, insnNode: " + insnNode);
        }

    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(String.class);
    }

}
