package com.taobao.arthas.bytekit.asm;

import com.alibaba.arthas.deps.org.objectweb.asm.Label;
import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.commons.LocalVariablesSorter;

/**
 * Adapter for to be inlined code.
 *
 * This adapter does all parameter renaming and replacing of the RETURN opcodes
 *
 *
 */
public class InliningAdapter extends LocalVariablesSorter {
    private final Label end;
    private LocalVariablesSorter lvs;

    public InliningAdapter(LocalVariablesSorter mv, int access, String desc, Label end) {
        super(Opcodes.ASM9, access, desc, mv);
        this.end = end;
        this.lvs = mv;

//        int off = (access & Opcodes.ACC_STATIC) != 0 ?
//                0 : 1;
//        Type[] args = Type.getArgumentTypes(desc);
//        for (int i = args.length - 1; i >= 0; i--) {
//            super.visitVarInsn(args[i].getOpcode(
//                    Opcodes.ISTORE), i + off);
//        }
//        if (off > 0) {
//            super.visitVarInsn(Opcodes.ASTORE, 0);
//        }

        // save args to local vars
        int off = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
        Type[] args = Type.getArgumentTypes(desc);
        int argsOff = off;

        for(int i = 0; i < args.length; ++i) {
            argsOff += args[i].getSize();
        }

        for(int i = args.length - 1; i >= 0; --i) {
            argsOff -= args[i].getSize();
            this.visitVarInsn(args[i].getOpcode(Opcodes.ISTORE), argsOff);
        }

        // this
        if (off > 0) {
            this.visitVarInsn(Opcodes.ASTORE, 0);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            super.visitJumpInsn(Opcodes.GOTO, end);
        } else {
            super.visitInsn(opcode);
        }
    }

    @Override
    public void visitMaxs(int stack, int locals) {
//        super.visitMaxs(stack, locals);
    }

    @Override
    protected int newLocalMapping(Type type) {
        return lvs.newLocal(type);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var + this.firstLocal);
    }
    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var + this.firstLocal, increment);
    }
    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, index + this.firstLocal);
    }
}