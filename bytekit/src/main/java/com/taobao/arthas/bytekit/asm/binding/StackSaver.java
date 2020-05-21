package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

/**
 * 在 return/throw/invoke 等location时，需要把栈上的值保存到locals里
 * @author hengyunabc
 *
 */
public interface StackSaver {
    /**
     * 有可能在两个地方被调用。1: 在最开始保存栈上的值时， 2: callback函数有返回值，想更新这个值时。stackSaver自己内部要保证保存的locals index是一致的
     * @param instructions
     * @param bindingContext
     */
    public void store(InsnList instructions, BindingContext bindingContext);
    
    public void load(InsnList instructions, BindingContext bindingContext);
    
    public Type getType(BindingContext bindingContext);

}
