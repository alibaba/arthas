package com.taobao.arthas.bytekit.asm.binding;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;

/**
 * @author hengyunabc
 *
 */
public class MethodNameBinding extends Binding {

	@Override
	public void pushOntoStack(InsnList instructions, BindingContext bindingContext) {
		MethodProcessor methodProcessor = bindingContext.getMethodProcessor();
		AsmOpUtils.ldc(instructions, methodProcessor.getMethodNode().name);
	}

	@Override
	public Type getType(BindingContext bindingContext) {
		return Type.getType(String.class);
	}

}
