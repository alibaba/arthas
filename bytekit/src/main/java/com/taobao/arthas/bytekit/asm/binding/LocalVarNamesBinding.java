package com.taobao.arthas.bytekit.asm.binding;

import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LocalVariableNode;

import com.taobao.arthas.bytekit.utils.AsmOpUtils;

public class LocalVarNamesBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AbstractInsnNode currentInsnNode = bindingContext.getLocation().getInsnNode();
        List<LocalVariableNode> results = AsmOpUtils
                .validVariables(bindingContext.getMethodProcessor().getMethodNode().localVariables, currentInsnNode);

        AsmOpUtils.push(instructions, results.size());
        AsmOpUtils.newArray(instructions, AsmOpUtils.STRING_TYPE);

        for (int i = 0; i < results.size(); ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);
            AsmOpUtils.push(instructions, results.get(i).name);

            AsmOpUtils.arrayStore(instructions, AsmOpUtils.STRING_TYPE);
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return AsmOpUtils.STRING_ARRAY_TYPE;
    }

}
