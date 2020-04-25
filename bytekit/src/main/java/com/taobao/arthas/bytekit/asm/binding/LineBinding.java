package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LineNumberNode;

import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * 
 * @author hengyunabc
 *
 */
public class LineBinding extends Binding {

    private boolean exact;

    public LineBinding(boolean exact) {
        this.exact = exact;
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        Location location = bindingContext.getLocation();
        AbstractInsnNode insnNode = location.getInsnNode();

        if (exact) {
            if (insnNode instanceof LineNumberNode) {
                AsmOpUtils.push(instructions, ((LineNumberNode) insnNode).line);
            } else {
                throw new IllegalArgumentException("LineBinding location is not LineNumberNode, insnNode: " + insnNode);
            }
        } else {
            while (insnNode != null) {
                if (insnNode instanceof LineNumberNode) {
                    AsmOpUtils.push(instructions, ((LineNumberNode) insnNode).line);
                    break;
                }
                insnNode = insnNode.getPrevious();
            }
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(int.class);
    }

}
