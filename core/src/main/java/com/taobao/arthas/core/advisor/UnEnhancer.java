package com.taobao.arthas.core.advisor;

import com.taobao.arthas.core.util.LogUtil;
import com.taobao.middleware.logger.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;

/**
 * Date: 2019/5/16
 *
 * @author xuzhiyi
 */
public class UnEnhancer implements ClassFileTransformer {

    private static final Logger logger = LogUtil.getArthasLogger();

    private final Set<Class<?>> matchingClasses;

    private static final int EMPTY_ADVICE_ID = -1;

    public UnEnhancer(Set<Class<?>> matchingClasses) {
        this.matchingClasses = matchingClasses;
    }

    public static synchronized void unEnhance(final Instrumentation inst, Set<Class<?>> unEnhanceClassSet) {
        UnEnhancer unEnhancer = new UnEnhancer(unEnhanceClassSet);
        try {
            inst.addTransformer(unEnhancer, true);
            EnhanceUtils.transform(inst, unEnhanceClassSet);
        } catch (Throwable t) {
            logger.warn("UnEnhancer transform error", t);
        } finally {
            inst.removeTransformer(unEnhancer);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        try {

            if (matchingClasses == null || !matchingClasses.contains(classBeingRedefined)) {
                return null;
            }

            ClassReader cr;
            final byte[] byteOfClassInCache = EnhanceUtils.CLASS_BYTES_CACHE.get(classBeingRedefined);
            if (byteOfClassInCache != null) {
                cr = new ClassReader(byteOfClassInCache);
            } else {
                cr = new ClassReader(classfileBuffer);
            }

            ClassNode classNode = new ClassNode();
            cr.accept(classNode, EXPAND_FRAMES);
            for (MethodNode methodNode : classNode.methods) {
                InsnList instructions = methodNode.instructions;
                List<TryCatchBlockNode> tryCatchBlocks = methodNode.tryCatchBlocks;
                Iterator<TryCatchBlockNode> iterator = tryCatchBlocks.iterator();
                while (iterator.hasNext()) {
                    TryCatchBlockNode tryCatchBlockNode = iterator.next();
                    int adviceId = getTryCatchBlockLockNodeId(tryCatchBlockNode);
                    if (adviced(adviceId) && EnhanceUtils.isUnRegistered(adviceId)) {
                        iterator.remove();
                    }
                }
                AbstractInsnNode insNode = instructions.getFirst();
                while (insNode != null) {
                    insNode = tryClearAndNext(instructions, insNode, false);
                }
            }

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw);
            byte[] bytes = cw.toByteArray();
            EnhanceUtils.CLASS_BYTES_CACHE.put(classBeingRedefined, bytes);
            EnhanceUtils.dumpClassIfNecessary(className, bytes);
            return bytes;

        } catch (Throwable t) {
            logger.warn("unEnhance :class[{}] failed.", className, t);
        }
        return null;
    }

    /**
     * Try to clear the special instruction generate by arthas.
     *
     * @param instructions  the instructions
     * @param insNode       the instruction node
     * @param deleteCurrent whether delete current node
     * @return the next node
     */
    private AbstractInsnNode tryClearAndNext(InsnList instructions, AbstractInsnNode insNode, boolean deleteCurrent) {
        int adviceId;
        if (adviced(adviceId = getLockCodeId(insNode))) {
            if (EnhanceUtils.isUnRegistered(adviceId)) {
                AbstractInsnNode next = tryClearAndNext(instructions, insNode.getNext(), true);
                while (next != null) {
                    if (isEndCodeStream(next)) {
                        for (int op : LockOpStream.END_CODE_STREAM) {
                            next = removeAndNext(instructions, next);
                        }
                        break;
                    } else {
                        next = removeAndNext(instructions, next);
                    }
                }
                return removeAndNext(instructions, insNode);
            }
        } else if (adviced(adviceId = getLockInsId(insNode))) {
            if (EnhanceUtils.isUnRegistered(adviceId)) {
                return clearLockIns(instructions, insNode);
            }
        }

        return deleteCurrent ? removeAndNext(instructions, insNode) : insNode.getNext();
    }

    /**
     * Remove the node and return next node.
     *
     * @param instructions the instructions
     * @param remove       the instruction node
     * @return next node
     */
    private AbstractInsnNode removeAndNext(InsnList instructions, AbstractInsnNode remove) {
        AbstractInsnNode next = remove.getNext();
        instructions.remove(remove);
        return next;
    }

    /**
     * Remove two node and return next node.
     *
     * @param instructions the instructions
     * @param remove       the instruction node
     * @return next node
     */
    private AbstractInsnNode removeAndNextX2(InsnList instructions, AbstractInsnNode remove) {
        AbstractInsnNode next = removeAndNext(instructions, remove);
        if (next != null) {
            return removeAndNext(instructions, next);
        } else {
            return null;
        }
    }

    /**
     * Remove three node and return next node.
     *
     * @param instructions the instructions
     * @param remove       the instruction node
     * @return next node
     */
    private AbstractInsnNode removeAndNextX3(InsnList instructions, AbstractInsnNode remove) {
        AbstractInsnNode next = removeAndNextX2(instructions, remove);
        if (next != null) {
            return removeAndNext(instructions, next);
        } else {
            return null;
        }
    }

    /**
     * Is adviced.
     *
     * @param adviceId the advice id
     * @return is adviced
     */
    private boolean adviced(int adviceId) {
        return adviceId != EMPTY_ADVICE_ID;
    }

    /**
     * If the instruction node is the begin of special lock code array, then return the advice id.
     *
     * @param insNode the instruction node
     * @return the advice id
     */
    private int getLockCodeId(AbstractInsnNode insNode) {
        AbstractInsnNode node = insNode;
        for (int op : LockOpStream.BEGIN_CODE_STREAM) {
            if (node == null) {
                return EMPTY_ADVICE_ID;
            }
            if (op == node.getOpcode()) {
                node = node.getNext();
            } else {
                return EMPTY_ADVICE_ID;
            }
        }
        return decodeAdviceId(node);
    }

    /**
     * If the instruction node is the special marked instruction, then return the advice id.
     *
     * @param insNode the instruction node
     * @return the advice id
     */
    private int getLockInsId(AbstractInsnNode insNode) {
        AbstractInsnNode node = insNode;
        for (int op : LockOpStream.INS_MARK_STREAM) {
            if (node == null) {
                return EMPTY_ADVICE_ID;
            }
            if (op == node.getOpcode()) {
                node = node.getNext();
            } else {
                return EMPTY_ADVICE_ID;
            }
        }
        return decodeAdviceId(node);
    }

    /**
     * If the try catch block is generate by arthas, then return the advice id.
     *
     * @param tryCatchBlockNode the try catch block
     * @return the advice id
     */
    private int getTryCatchBlockLockNodeId(TryCatchBlockNode tryCatchBlockNode) {
        return getLockCodeId(tryCatchBlockNode.handler.getNext().getNext());
    }

    /**
     * Clear the marked instruction.
     * The structure of instruction stream: instruction mark array + advice mark + your instruction
     * Example: ICONST_2, ICONST_0, ICONST_1, SWAP, SWAP, POP2, POP, ICONST_0, ATHROW
     *
     * @param instructions the instructions
     * @param insNode      the begin of instruction node
     */
    private AbstractInsnNode clearLockIns(InsnList instructions, AbstractInsnNode insNode) {
        AbstractInsnNode node = insNode;
        for (int op : LockOpStream.INS_MARK_STREAM) {
            node = removeAndNext(instructions, node);
        }
        return removeAndNextX3(instructions, node);
    }

    /**
     * Is the first code of end code array.
     *
     * @param insNode the instruction node
     * @return is the first code of end code stream
     */
    private boolean isEndCodeStream(AbstractInsnNode insNode) {
        AbstractInsnNode node = insNode;
        for (int op : LockOpStream.END_CODE_STREAM) {
            if (node == null) {
                return false;
            }
            if (op == node.getOpcode()) {
                node = node.getNext();
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the advice id in node.
     *
     * @param insNode the instruction node
     * @return the advice id
     */
    private int decodeAdviceId(AbstractInsnNode insNode) {
        if (insNode instanceof IntInsnNode) {
            return ((IntInsnNode) insNode).operand;
        } else {
            switch (insNode.getOpcode()) {
                case ICONST_0:
                    return 0;
                case ICONST_1:
                    return 1;
                case ICONST_2:
                    return 2;
                case ICONST_3:
                    return 3;
                case ICONST_4:
                    return 4;
                case ICONST_5:
                    return 5;
                default:
                    throw new IllegalArgumentException("get advice id fail");
            }
        }
    }

}
