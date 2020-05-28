package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LineNumberNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.taobao.arthas.bytekit.asm.location.MethodInsnNodeWare;
import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * 包含 owner/method name/ method desc/ line number
 * 
 * @author hengyunabc 2020-05-14
 *
 */
public class InvokeInfoBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        Location location = bindingContext.getLocation();
        if (location instanceof MethodInsnNodeWare) {
            MethodInsnNodeWare methodInsnNodeWare = (MethodInsnNodeWare) location;
            MethodInsnNode methodInsnNode = methodInsnNodeWare.methodInsnNode();

            int line = -1;

            if (location.isWhenComplete() == false) {
                AbstractInsnNode insnNode = methodInsnNode.getPrevious();
                while (insnNode != null) {
                    if (insnNode instanceof LineNumberNode) {
                        line = ((LineNumberNode) insnNode).line;
                        break;
                    }
                    insnNode = insnNode.getPrevious();
                }
            } else {
                AbstractInsnNode insnNode = methodInsnNode.getNext();
                while (insnNode != null) {
                    if (insnNode instanceof LineNumberNode) {
                        line = ((LineNumberNode) insnNode).line;
                        break;
                    }
                    insnNode = insnNode.getNext();
                }
            }

            String result = methodInsnNode.owner + "|" + methodInsnNode.name + "|" + methodInsnNode.desc + "|" + line;
            AsmOpUtils.push(instructions, result);

        } else {
            throw new IllegalArgumentException(
                    "InvokeMethodNameBinding location is not Invocation location, location: " + location);
        }

    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(String.class);
    }

}
