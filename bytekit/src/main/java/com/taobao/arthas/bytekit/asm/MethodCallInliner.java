package com.taobao.arthas.bytekit.asm;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Label;
import com.alibaba.arthas.deps.org.objectweb.asm.MethodVisitor;
import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.commons.GeneratorAdapter;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;

/**
 * @author hengyunabc 2018-01-31
 *
 */
public abstract class MethodCallInliner extends GeneratorAdapter {
	public class CatchBlock {

		private Label start;
		private Label handler;
		private String type;
		private Label end;

		public CatchBlock(Label start, Label end, Label handler, String type) {
			this.start = start;
			this.end = end;
			this.handler = handler;
			this.type = type;
		}

	}

	private final MethodNode toBeInlined;
	private List<CatchBlock> blocks = new ArrayList<CatchBlock>();
	private boolean inlining;
	private boolean afterInlining;

	public MethodCallInliner(int access, String name, String desc, MethodVisitor mv,
			MethodNode toBeInlined) {
		super(Opcodes.ASM9, mv, access, name, desc);
		this.toBeInlined = toBeInlined;
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if (!shouldBeInlined(owner, name, desc)) {
			mv.visitMethodInsn(opcode, owner, name, desc, itf);
			return;
		}

		// if (this.analyzerAdapter != null) {
		// mv = new MergeFrameAdapter(this.api, this.analyzerAdapter,
		// (MethodVisitor)mv);
		// }

		Label end = new Label();
		inlining = true;
		toBeInlined.instructions.resetLabels();

		// pass the to be inlined method through the inlining adapter to this
		toBeInlined.accept(new InliningAdapter(this, toBeInlined.access, toBeInlined.desc, end));
		inlining = false;
		afterInlining = true;

		// visit the end label
		super.visitLabel(end);

		// box the return value if necessary
		// Type returnType =
		// Type.getMethodType(toBeInlined.desc).getReturnType();
		// valueOf(returnType);

	}

	abstract boolean shouldBeInlined(String owner, String name, String desc);

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		if (!inlining) {
			blocks.add(new CatchBlock(start, end, handler, type));
		} else {
			super.visitTryCatchBlock(start, end, handler, type);
		}
	}

	@Override
	public void visitMaxs(int stack, int locals) {
		for (CatchBlock b : blocks)
			super.visitTryCatchBlock(b.start, b.end, b.handler, b.type);
		super.visitMaxs(stack, locals);
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
		// swallow
	}
}