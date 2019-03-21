package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

import com.taobao.arthas.bytekit.utils.AsmOpUtils;

public class ArgNamesBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        
        String[] parameterNames = bindingContext.getMethodProcessor().getParameterNames();
        
        AsmOpUtils.push(instructions, parameterNames.length);
        AsmOpUtils.newArray(instructions, AsmOpUtils.STRING_TYPE);

        for(int i = 0; i < parameterNames.length; ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);
            AsmOpUtils.push(instructions, parameterNames[i]);

            AsmOpUtils.arrayStore(instructions, AsmOpUtils.STRING_TYPE);
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return AsmOpUtils.STRING_ARRAY_TYPE;
    }

}
