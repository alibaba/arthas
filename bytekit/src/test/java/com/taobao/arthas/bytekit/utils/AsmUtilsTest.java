package com.taobao.arthas.bytekit.utils;

import java.io.IOException;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import com.alibaba.arthas.deps.org.objectweb.asm.ClassWriter;
import com.alibaba.arthas.deps.org.objectweb.asm.MethodVisitor;
import com.alibaba.arthas.deps.org.objectweb.asm.Opcodes;
import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.AbstractInsnNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;

import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.VerifyUtils;

public class AsmUtilsTest {

     abstract static class  TestClass {
        public static synchronized List<String> sss(int i, long l, List<String> list) throws IOException, ArrayIndexOutOfBoundsException {
            return null;
        }
        protected abstract String hello(String ss);
    }

     static class TestConstructorClass {
         public TestConstructorClass(int i, String s) {

         }
     }

    @Test
    public void testMethodDeclaration() throws IOException {
        ClassNode classNode = AsmUtils.loadClass(TestClass.class);
        MethodNode sss = AsmUtils.findFirstMethod(classNode.methods, "sss");

        MethodNode hello = AsmUtils.findFirstMethod(classNode.methods, "hello");

        MethodNode constructor = AsmUtils.findFirstMethod(AsmUtils.loadClass(TestConstructorClass.class).methods, "<init>");

        String helloDeclaration = AsmUtils.methodDeclaration(Type.getType(TestClass.class), hello);
        String sssDeclaration = AsmUtils.methodDeclaration(Type.getType(TestClass.class), sss);

        String constructorDeclaration = AsmUtils.methodDeclaration(Type.getType(TestConstructorClass.class), constructor);

        System.err.println(helloDeclaration);
        System.err.println(sssDeclaration);
        System.err.println(constructorDeclaration);

        Assertions.assertThat(helloDeclaration).isEqualTo("protected abstract java.lang.String hello(java.lang.String)");
        Assertions.assertThat(sssDeclaration).isEqualTo(
                "public static synchronized java.util.List sss(int, long, java.util.List) throws java.io.IOException, java.lang.ArrayIndexOutOfBoundsException");
        Assertions.assertThat(constructorDeclaration).isEqualTo("public com.taobao.arthas.bytekit.utils.AsmUtilsTest$TestConstructorClass(int, java.lang.String)");
    }

    public static byte[] emptyMethodBytes() throws Exception {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, "LEmptyClass", null, "java/lang/Object", null);

        {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "emptyMethod", "()V", null, null);
            // mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            // mv.visitMaxs(0, 0);
            // mv.visitEnd();
        }
        cw.visitEnd();

        return cw.toByteArray();
    }

    @Test
    public void emptyMethodTest() throws Exception {

        byte[] emptyMethodBytes = emptyMethodBytes();

        VerifyUtils.asmVerify(emptyMethodBytes);
        VerifyUtils.instanceVerity(emptyMethodBytes);

        ClassNode classNode = AsmUtils.toClassNode(emptyMethodBytes);
        MethodNode methodNode = AsmUtils.findFirstMethod(classNode.methods, "emptyMethod");

        AbstractInsnNode first = methodNode.instructions.getFirst();
        AbstractInsnNode last = methodNode.instructions.getLast();
        System.err.println(first);
        System.err.println(last);

        int size = methodNode.instructions.size();
        for (int i = 0; i < size; ++i) {
            System.err.println(methodNode.instructions.get(i));
        }

        // String asmCode = AsmUtils.toASMCode(classNode);
        // System.err.println(asmCode);
    }

    private String aaa = "";
    public void xxx () {
        aaa = "bbb";
    }

    @Test
    public void testFieldAccess() throws IOException {
        ClassNode classNode = AsmUtils.loadClass(AsmUtilsTest.class);

        MethodNode methodNode = AsmUtils.findFirstMethod(classNode.methods, "xxx");

        int size = methodNode.instructions.size();
        for (int i = 0; i < size; ++i) {
            System.err.println(methodNode.instructions.get(i));
        }


    }


    @Test
    public void testRenameClass() throws Exception {
    	ClassNode classNode = AsmUtils.loadClass(AsmUtilsTest.class);

    	byte[] classBytes = AsmUtils.toBytes(classNode);

    	byte[] renameClass = AsmUtils.renameClass(classBytes, "com.test.Test.XXX");

    	VerifyUtils.asmVerify(renameClass);
    	Object object = VerifyUtils.instanceVerity(renameClass);

    	Assertions.assertThat(object.getClass().getName()).isEqualTo("com.test.Test.XXX");
    }

}
