package com.taobao.arthas.bytekit.asm.inst.impl;

import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.commons.Method;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.TypeInsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

/**
 *
 * @author hengyunabc 2019-03-15
 *
 */
public class InstrumentImpl {

    public static MethodNode replaceInvokeOrigin(String originOwner, MethodNode originMethodNode,
                    MethodNode apmMethodNode) {

        // 查找到所有的 InstrumentApi.invokeOrigin() 指令
        List<MethodInsnNode> methodInsnNodes = AsmUtils.findMethodInsnNode(apmMethodNode,
                        "com/taobao/arthas/bytekit/asm/inst/InstrumentApi", "invokeOrigin", "()Ljava/lang/Object;");

        Type originReturnType = Type.getMethodType(originMethodNode.desc).getReturnType();

        for (MethodInsnNode methodInsnNode : methodInsnNodes) {
            InsnList instructions = new InsnList();

            AbstractInsnNode secondInsnNode = methodInsnNode.getNext();

            // 如果是 非 static ，则要 load this
            boolean isStatic = AsmUtils.isStatic(originMethodNode);
            int opcode = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
            if (!isStatic) {
                AsmOpUtils.loadThis(instructions);
            }
            AsmOpUtils.loadArgs(instructions, originMethodNode);

            MethodInsnNode originMethodInsnNode = new MethodInsnNode(opcode, originOwner, originMethodNode.name,
                            originMethodNode.desc, false);
            // 调用原来的函数
            instructions.add(originMethodInsnNode);

            int sort = originReturnType.getSort();
            if (sort == Type.VOID) {
                if (secondInsnNode != null) {
                    if (secondInsnNode.getOpcode() == Opcodes.POP) {
                        // TODO 原来的函数没有返回值，这里要把 POP去掉。有没有可能是 POP2 ?
                        apmMethodNode.instructions.remove(secondInsnNode);
                    } else {
                        // TODO 原来函数没有返回值，这里有没有可能要赋值？？是否要 push null?
                        AsmOpUtils.pushNUll(instructions);
                    }
                }
            } else if (sort >= Type.BOOLEAN && sort <= Type.DOUBLE) {
                if (secondInsnNode.getOpcode() == Opcodes.POP) {
                    // 原来是 pop掉一个栈，如果函数返回的是 long，则要pop2
                    if (originReturnType.getSize() == 2) {
                        apmMethodNode.instructions.insert(secondInsnNode, new InsnNode(Opcodes.POP2));
                        apmMethodNode.instructions.remove(secondInsnNode);
                    }
                } else {
                    /**
                     * 需要把下面两条cast和unbox的指令删掉
                     *
                     * <pre>
                     * CHECKCAST java/lang/Integer
                     * INVOKEVIRTUAL java/lang/Integer.intValue ()I
                     * </pre>
                     */
                    boolean removeCheckCast = false;
                    if (secondInsnNode.getOpcode() == Opcodes.CHECKCAST) {
                        TypeInsnNode typeInsnNode = (TypeInsnNode) secondInsnNode;
                        // 从原始函数的返回值，获取到它对应的自动box的类
                        Type boxedType = AsmOpUtils.getBoxedType(originReturnType);
                        if (Type.getObjectType(typeInsnNode.desc).equals(boxedType)) {
                            AbstractInsnNode thridInsnNode = secondInsnNode.getNext();
                            if (thridInsnNode != null && thridInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                                MethodInsnNode valueInsnNode = (MethodInsnNode) thridInsnNode;
                                Method unBoxMethod = AsmOpUtils.getUnBoxMethod(originReturnType);
                                if (unBoxMethod.getDescriptor().equals(valueInsnNode.desc)
                                                && valueInsnNode.owner.equals(boxedType.getInternalName())) {
                                    apmMethodNode.instructions.remove(thridInsnNode);
                                    apmMethodNode.instructions.remove(secondInsnNode);
                                    removeCheckCast = true;
                                }
                            }
                        }
                    }
                    if (!removeCheckCast) {
                        // 没有被转换为原始类型，也没有pop，则说明赋值给了一个对象，用类似Long.valudOf转换为Object
                        AsmOpUtils.box(instructions, originReturnType);

                    }

                }
            } else {// ARRAY/OBJECT
                // 移掉可能有的 check cast
                if (secondInsnNode.getOpcode() == Opcodes.CHECKCAST) {
                    TypeInsnNode typeInsnNode = (TypeInsnNode) secondInsnNode;
                    if (Type.getObjectType(typeInsnNode.desc).equals(originReturnType)) {
                        apmMethodNode.instructions.remove(secondInsnNode);
                    }
                }
            }
            apmMethodNode.instructions.insertBefore(methodInsnNode, instructions);
            apmMethodNode.instructions.remove(methodInsnNode);
        }

        MethodProcessor methodProcessor = new MethodProcessor(originOwner, apmMethodNode);
        methodProcessor.inline(originOwner, originMethodNode);

        return apmMethodNode;
    }

}
