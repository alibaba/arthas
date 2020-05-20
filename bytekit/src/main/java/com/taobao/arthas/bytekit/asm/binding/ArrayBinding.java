package com.taobao.arthas.bytekit.asm.binding;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;

import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * TODO 这个判断是否要从stack上取数据，要看 其它的binding是否需要。 是否 optional，这个应该是由 ArrayBinding 整体设定？？
 * @author hengyunabc
 *
 */
public class ArrayBinding extends Binding{

    // TODO 数组的 type是什么？
//    private Type type;

    List<Binding> bindingList = new ArrayList<Binding>();

    public ArrayBinding(List<Binding> bindingList) {
		this.bindingList = bindingList;
	}

    @Override
    public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
        AsmOpUtils.push(instructions, bindingList.size());
        AsmOpUtils.newArray(instructions, AsmOpUtils.OBJECT_TYPE);

        for(int i = 0; i < bindingList.size(); ++i) {
            AsmOpUtils.dup(instructions);

            AsmOpUtils.push(instructions, i);
            Binding binding = bindingList.get(i);
            binding.pushOntoStack(instructions, bindingContext);
            AsmOpUtils.box(instructions, binding.getType(bindingContext));

            AsmOpUtils.arrayStore(instructions, AsmOpUtils.OBJECT_TYPE);
        }
    }

    @Override
    public Type getType(BindingContext bindingContext) {
        // TODO Auto-generated method stub
        return null;
    }

}
