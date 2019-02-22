package com.taobao.arthas.bytekit.asm.binding;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

public class ThisBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        bindingContext.getMethodProcessor().loadThis(instructions);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(Object.class);
    }

}
