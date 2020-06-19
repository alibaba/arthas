package com.taobao.arthas.bytekit.asm.inst.impl;

import java.util.List;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.commons.Method;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.TypeInsnNode;

import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;

/**
 * InstrumentApi.invokeOrigin()的处理类
 *
 * @author hengyunabc 2019-03-15
 * @author gongdewei 2020-06-18
 */
public class InstrumentImpl {

    /**
     * 使用inline方式实现invokeOrigin功能
     * @param classNode target classNode
     * @param originMethodNode origin method node
     * @param apmMethodNode apm method node
     * @return
     */
    public static MethodNode inlineInvokeOrigin(ClassNode classNode, MethodNode originMethodNode,
                                                MethodNode apmMethodNode) {
        // 修改apmMethodNode：替换InstrumentApi.invokeOrigin()语句为originMethod invoke语句
        String originOwner = classNode.name;
        replaceInvokeOrigin(originOwner, originMethodNode, apmMethodNode);

        // 修改apmMethodNode：将originMethod invoke语句替换为originMethod内容
        MethodProcessor methodProcessor = new MethodProcessor(originOwner, apmMethodNode);
        methodProcessor.inline(originOwner, originMethodNode);

        // 用apmMethod替换originMethod内容
        AsmUtils.replaceMethod(classNode, apmMethodNode);

        return apmMethodNode;
    }

    /**
     * 使用delegate方式实现invokeOrigin功能
     *
     * @param classNode target classNode
     * @param originMethodNode origin method node
     * @param apmMethodNode apm method node
     * @return
     */
    public static MethodNode delegateInvokeOrigin(ClassNode classNode, MethodNode originMethodNode,
                                                  MethodNode apmMethodNode) {
        // 生成幂等delegateMethodName，重复retransform的方法名相同
        String originMethodName = originMethodNode.name;
        String delegateMethodName = originMethodName +"_origin_"+ getHash(classNode.name, originMethodName, originMethodNode.desc);

        // rename originMethod to delegateMethodName
        MethodNode delegateMethodNode = originMethodNode;
        delegateMethodNode.name = delegateMethodName;

        // 修改apmMethodNode：替换InstrumentApi.invokeOrigin()语句为delegateMethod invoke语句
        replaceInvokeOrigin(classNode.name, delegateMethodNode, apmMethodNode);

        // 用apmMethod替换originMethod内容
        apmMethodNode.name = originMethodName;
        AsmUtils.replaceMethod(classNode, apmMethodNode);

        // 添加delegateMethod 到classNode
        classNode.methods.add(delegateMethodNode);

        return apmMethodNode;
    }

    private static String getHash(String originOwner, String methodName, String methodDesc) {
        return Integer.toHexString((originOwner+"_"+methodName+"_"+methodDesc).hashCode());
    }

    private static void replaceInvokeOrigin(String originOwner, MethodNode originMethodNode,
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
    }

}
