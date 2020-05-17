package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * method name | method desc 的方式组织
 * 
 * TODO 是否要有 line number ?
 * 
 * @author hengyunabc 2020-05-16
 *
 */
public class MethodInfoBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
        MethodNode methodNode = methodProcessor.getMethodNode();
        AsmOpUtils.ldc(instructions, methodNode.name + '|' +  methodNode.desc);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(String.class);
    }
}
