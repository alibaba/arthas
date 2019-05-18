package com.taobao.arthas.core.advisor;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * ASM code lock.
 *
 * Created by vlinux on 15/5/28.
 */
public class AsmCodeLock implements CodeLock, Opcodes {

    private final AdviceAdapter aa;

    /**
     * The lock mark.
     */
    private boolean isLock;

    /**
     * The special opcode array to begin code block.
     */
    private final int[] beginCodeArray;

    /**
     * The special opcode array to end code block.
     */
    private final int[] endCodeArray;

    /**
     * The special opcode array to mark a instruction.
     */
    private final int[] insMarkArray;

    /**
     * Code matching Index.
     */
    private int index = 0;

    /**
     * ASM code lock.
     *
     * @param aa             ASM MethodVisitor
     * @param beginCodeArray the special opcode stream to mark begin of a code block.
     * @param endCodeArray   the special opcode stream to mark end of a code block.
     * @param insMarkArray   the special opcode stream to mark a instruction.
     */
    public AsmCodeLock(AdviceAdapter aa, int[] beginCodeArray, int[] endCodeArray, int[] insMarkArray) {
        if (null == beginCodeArray
            || null == endCodeArray
            || beginCodeArray.length != endCodeArray.length) {
            throw new IllegalArgumentException();
        }

        this.aa = aa;
        this.beginCodeArray = beginCodeArray;
        this.endCodeArray = endCodeArray;
        this.insMarkArray = insMarkArray;

    }

    @Override
    public void code(int code) {

        final int[] codes = isLock() ? endCodeArray : beginCodeArray;

        if (index >= codes.length) {
            reset();
            return;
        }

        if (codes[index] != code) {
            reset();
            return;
        }

        if (++index == codes.length) {
            // revers lock
            isLock = !isLock;
            reset();
        }

    }

    /**
     * Reset match index.
     */
    private void reset() {
        index = 0;
    }


    private void visitInsn(int opcode) {
        aa.visitInsn(opcode);
    }

    /**
     * Push advice id to the byte code.
     *
     * @param adviceId advice id
     */
    private void adviceMark(int adviceId) {
        aa.push(adviceId);
        aa.pop();
    }

    /**
     * Insert lock opcode stream.
     */
    private void lock(Integer adviceId) {
        for (int op : beginCodeArray) {
            visitInsn(op);
        }
        adviceMark(adviceId);
    }

    /**
     * Insert unlock opcode stream.
     */
    private void unLock() {
        for (int op : endCodeArray) {
            visitInsn(op);
        }
    }

    @Override
    public boolean isLock() {
        return isLock;
    }

    @Override
    public void lock(Block block, int adviceId) {
        lock(adviceId);
        try {
            block.code();
        } finally {
            unLock();
        }
    }

    @Override
    public void markInsn(Block block, int adviceId) {
        for (int op : insMarkArray) {
            visitInsn(op);
        }
        adviceMark(adviceId);
        block.code();
    }

}
