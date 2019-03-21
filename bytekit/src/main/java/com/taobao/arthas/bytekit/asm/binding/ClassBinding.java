package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

import com.taobao.arthas.bytekit.utils.AsmOpUtils;

public class ClassBinding extends Binding{

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        String owner = bindingContext.getMethodProcessor().getOwner();
        AsmOpUtils.ldc(instructions, Type.getObjectType(owner));
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(Class.class);
    }

}
