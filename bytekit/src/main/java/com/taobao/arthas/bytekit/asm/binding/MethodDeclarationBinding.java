package com.taobao.arthas.bytekit.asm.binding;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

/**
 * 提供一个完整的 method 的string，包含类名，并不是desc？用户可以自己提取descs method的定义，前面是 public
 * /static 这些关键字，是有限的几个。后面是 throws ，的异常信息
 * 
 * @author hengyunabc
 *
 */
public class MethodDeclarationBinding extends Binding {

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
        AsmOpUtils.ldc(instructions, AsmUtils.methodDeclaration(Type.getObjectType(methodProcessor.getOwner()),
                methodProcessor.getMethodNode()));

    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(String.class);
    }

}
