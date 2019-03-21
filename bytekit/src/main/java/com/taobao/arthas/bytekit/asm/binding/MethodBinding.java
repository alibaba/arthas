package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * @author hengyunabc
 *
 */
public class MethodBinding  extends Binding{

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        // 先获取类本身的 class ，再调用 getDeclaredMethod ，它需要一个变长参数，实际上要传一个数组
        /**
         * @see java.lang.Class.getDeclaredMethod(String, Class<?>...)
         */
        MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
        AsmOpUtils.ldc(instructions, Type.getObjectType(methodProcessor.getOwner()));
        
        AsmOpUtils.push(instructions, methodProcessor.getMethodNode().name);
        
        Type[] argumentTypes = Type.getMethodType(methodProcessor.getMethodNode().desc).getArgumentTypes();
        
        AsmOpUtils.push(instructions, argumentTypes.length);
        AsmOpUtils.newArray(instructions, Type.getType(Class.class));

        for(int i = 0; i < argumentTypes.length; ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);
            
            AsmOpUtils.ldc(instructions, argumentTypes[i]);
            AsmOpUtils.arrayStore(instructions, Type.getType(Class.class));
        }
        
        MethodInsnNode declaredMethodInsnNode = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getType(Class.class).getInternalName(),
                "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
        instructions.add(declaredMethodInsnNode);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return Type.getType(java.lang.reflect.Method.class);
    }

}
