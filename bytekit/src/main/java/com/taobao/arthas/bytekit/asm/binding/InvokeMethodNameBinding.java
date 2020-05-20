package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.taobao.arthas.bytekit.asm.location.MethodInsnNodeWare;
import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * 
 * @author hengyunabc
 *
 */
public class InvokeMethodNameBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        Location location = bindingContext.getLocation();
        if (location instanceof MethodInsnNodeWare) {
            MethodInsnNodeWare methodInsnNodeWare = (MethodInsnNodeWare) location;
            MethodInsnNode methodInsnNode = methodInsnNodeWare.methodInsnNode();
            AsmOpUtils.push(instructions, methodInsnNode.name);

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
