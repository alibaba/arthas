package com.taobao.arthas.bytekit.asm.binding;

import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LocalVariableNode;

import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * TODO 增加一个配置，是否包含 method args
 * @author hengyunabc
 *
 */
public class LocalVarsBinding extends Binding{

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {

        AbstractInsnNode currentInsnNode = bindingContext.getLocation().getInsnNode();

        List<LocalVariableNode> results = AsmOpUtils
                .validVariables(bindingContext.getMethodProcessor().getMethodNode().localVariables, currentInsnNode);

        AsmOpUtils.push(instructions, results.size());
        AsmOpUtils.newArray(instructions, AsmOpUtils.OBJECT_TYPE);

        for (int i = 0; i < results.size(); ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);

            LocalVariableNode variableNode = results.get(i);
            AsmOpUtils.loadVar(instructions, Type.getType(variableNode.desc), variableNode.index);
            AsmOpUtils.box(instructions, Type.getType(variableNode.desc));

            AsmOpUtils.arrayStore(instructions, AsmOpUtils.OBJECT_TYPE);
        }

    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return AsmOpUtils.OBJECT_ARRAY_TYPE;
    }

}
