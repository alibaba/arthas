package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

/**
 * TODO 要检查 location 是否是合法的
 * @author hengyunabc
 *
 */
public class ThrowableBinding extends Binding {

    @Override
    public boolean fromStack() {
        return true;
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        // TODO 这里从 StackSaver 里取是否合理？
        bindingContext.getStackSaver().load(instructions, bindingContext);
        // 是否要 check cast ?
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(Throwable.class);
    }

}
