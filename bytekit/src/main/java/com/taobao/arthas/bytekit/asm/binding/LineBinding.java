package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LineNumberNode;

import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

public class LineBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        Location location = bindingContext.getLocation();
        AbstractInsnNode insnNode = location.getInsnNode();

        if (insnNode instanceof LineNumberNode) {
            AsmOpUtils.push(instructions, ((LineNumberNode) insnNode).line);
        } else {
            throw new IllegalArgumentException("LineBinding location is not LineNumberNode, insnNode: " + insnNode);
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(int.class);
    }

}
