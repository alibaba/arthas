package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

import com.taobao.arthas.bytekit.utils.AsmOpUtils;

public class ArgsBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AsmOpUtils.loadArgArray(instructions, bindingContext.getMethodProcessor().getMethodNode());
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return AsmOpUtils.OBJECT_ARRAY_TYPE;
    }

}
