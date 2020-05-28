package com.taobao.arthas.bytekit.asm;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.InsnList;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.JumpInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.LabelNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.bytekit.utils.AsmOpUtils;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.Decompiler;
import com.taobao.arthas.bytekit.utils.VerifyUtils;

import net.bytebuddy.agent.ByteBuddyAgent;

public class TryCatchBlockTest {

    static public class Hello {

        public long sss(String msg, int i, long l) {
            return 124L;
        }

        public long toBeCall(int i , long l, String s) {
            return l + i;
        }
        
//        public String say(String msg, int i, long l) {
//            i = 0;
//            System.out.println("hello");
//            i = 0;
//            sss(msg, i, l);
//            return "";
//        }
        
        public int say(int ii) {
            toBeCall(ii, 123L, "");
            return 123;
        }
    }

    public static void ttt() throws Throwable {
        try {
            System.out.println();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
        try {
            System.out.println();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void test() throws Exception {

        Instrumentation instrumentation = ByteBuddyAgent.install();

        ClassNode classNode = AsmUtils.loadClass(Hello.class);

        List<MethodNode> methods = AsmUtils.findMethods(classNode.methods, "say");

        MethodNode methodNode = methods.get(0);

        AbstractInsnNode insnNode = methodNode.instructions.getFirst();

        List<MethodInsnNode> methodInsnNodes = new ArrayList<MethodInsnNode>();

        while (insnNode != null) {
            if (insnNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                methodInsnNodes.add(methodInsnNode);
            }
            insnNode = insnNode.getNext();
        }

        for (MethodInsnNode methodInsnNode : methodInsnNodes) {
            TryCatchBlock tryCatchBlock = new TryCatchBlock(methodNode);

            InsnList toInsert = new InsnList();

            LabelNode gotoDest = new LabelNode();

            LabelNode startLabelNode = tryCatchBlock.getStartLabelNode();
            LabelNode endLabelNode = tryCatchBlock.getEndLabelNode();

            toInsert.add(new JumpInsnNode(Opcodes.GOTO, gotoDest));
            toInsert.add(endLabelNode);

            AsmOpUtils.dup(toInsert);
            toInsert.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getType(Throwable.class).getInternalName(),
                    "printStackTrace", "()V", false));

            AsmOpUtils.throwException(toInsert);

            toInsert.add(gotoDest);

            methodNode.instructions.insertBefore(methodInsnNode, startLabelNode);
            methodNode.instructions.insert(methodInsnNode, toInsert);

            tryCatchBlock.sort();
        }

        byte[] bytes = AsmUtils.toBytes(classNode);

        String decompile = Decompiler.decompile(bytes);
        System.err.println(decompile);

        VerifyUtils.asmVerify(bytes, true);

        VerifyUtils.instanceVerity(bytes);

    }

}
