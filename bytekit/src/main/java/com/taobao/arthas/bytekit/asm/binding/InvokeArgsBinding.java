package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LocalVariableNode;

import com.taobao.arthas.bytekit.asm.location.Location;
import com.taobao.arthas.bytekit.asm.location.Location.InvokeLocation;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * invoke 传入的参数列表，有严格的限制，只能在 invoke 之前。
 * 
 * TODO ，当 static 函数时，在数组前，传一个null进去？ 不然，不好区分是否 static 函数调用？？
 * 
 * @author hengyunabc
 *
 */
public class InvokeArgsBinding extends Binding {

    @Override
    public boolean fromStack() {
        return true;
    }

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        Location location = bindingContext.getLocation();
        
        if(location instanceof InvokeLocation) {
            InvokeLocation invokeLocation = (InvokeLocation) location;
            if(invokeLocation.isWhenComplete()) {
                throw new IllegalArgumentException("InvokeArgsBinding can not work on InvokeLocation whenComplete is true.");
            }
        }else {
            throw new IllegalArgumentException("current location is not invoke location. location: " + location);
        }
        
        LocalVariableNode invokeArgsVariableNode = bindingContext.getMethodProcessor().initInvokeArgsVariableNode();
        AsmOpUtils.loadVar(instructions, AsmOpUtils.OBJECT_ARRAY_TYPE, invokeArgsVariableNode.index);
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        return AsmOpUtils.OBJECT_ARRAY_TYPE;
    }
}
