package com.taobao.arthas.core.advisor;

import java.arthas.SpyAPI;
import java.lang.instrument.Instrumentation;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

import com.alibaba.bytekit.utils.AsmUtils;
import com.alibaba.bytekit.utils.Decompiler;
import com.alibaba.deps.org.objectweb.asm.Type;
import com.alibaba.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.core.bytecode.TestHelper;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.util.matcher.EqualsMatcher;

import demo.MathGame;
import net.bytebuddy.agent.ByteBuddyAgent;

/**
 * 
 * @author hengyunabc 2020-05-19
 *
 */
public class EnhancerTest {

    @Test
    public void test() throws Throwable {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        TestHelper.appendSpyJar(instrumentation);

        ArthasBootstrap.getInstance(instrumentation, "ip=127.0.0.1");

        AdviceListener listener = Mockito.mock(AdviceListener.class);

        EqualsMatcher<String> methodNameMatcher = new EqualsMatcher<String>("print");
        EqualsMatcher<String> classNameMatcher = new EqualsMatcher<String>(MathGame.class.getName());

        Enhancer enhancer = new Enhancer(listener, true, false, null, classNameMatcher, null, methodNameMatcher);

        ClassLoader inClassLoader = MathGame.class.getClassLoader();
        String className = MathGame.class.getName();
        Class<?> classBeingRedefined = MathGame.class;

        ClassNode classNode = AsmUtils.loadClass(MathGame.class);

        byte[] classfileBuffer = AsmUtils.toBytes(classNode);

        byte[] result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, classfileBuffer);

        ClassNode resultClassNode1 = AsmUtils.toClassNode(result);

//        FileUtils.writeByteArrayToFile(new File("/tmp/MathGame1.class"), result);

        result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, result);

        ClassNode resultClassNode2 = AsmUtils.toClassNode(result);

//        FileUtils.writeByteArrayToFile(new File("/tmp/MathGame2.class"), result);

        MethodNode resultMethodNode1 = AsmUtils.findMethods(resultClassNode1.methods, "print").get(0);
        MethodNode resultMethodNode2 = AsmUtils.findMethods(resultClassNode2.methods, "print").get(0);

        Assertions
                .assertThat(AsmUtils
                        .findMethodInsnNode(resultMethodNode1, Type.getInternalName(SpyAPI.class), "atEnter").size())
                .isEqualTo(AsmUtils.findMethodInsnNode(resultMethodNode2, Type.getInternalName(SpyAPI.class), "atEnter")
                        .size());

        Assertions.assertThat(AsmUtils
                .findMethodInsnNode(resultMethodNode1, Type.getInternalName(SpyAPI.class), "atExceptionExit").size())
                .isEqualTo(AsmUtils
                        .findMethodInsnNode(resultMethodNode2, Type.getInternalName(SpyAPI.class), "atExceptionExit")
                        .size());

        Assertions.assertThat(AsmUtils
                .findMethodInsnNode(resultMethodNode1, Type.getInternalName(SpyAPI.class), "atBeforeInvoke").size())
                .isEqualTo(AsmUtils
                        .findMethodInsnNode(resultMethodNode2, Type.getInternalName(SpyAPI.class), "atBeforeInvoke")
                        .size());
        Assertions.assertThat(AsmUtils
                .findMethodInsnNode(resultMethodNode1, Type.getInternalName(SpyAPI.class), "atInvokeException").size())
                .isEqualTo(AsmUtils
                        .findMethodInsnNode(resultMethodNode2, Type.getInternalName(SpyAPI.class), "atInvokeException")
                        .size());

        String string = Decompiler.decompile(result);

        System.err.println(string);
    }

    /**
     * 测试line命令使用的LineNumber的增强
     */
    @Test
    public void testLineWithLineNumber() throws Throwable {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        TestHelper.appendSpyJar(instrumentation);

        ArthasBootstrap.getInstance(instrumentation, "ip=127.0.0.1");

        AdviceListener listener = Mockito.mock(AdviceListener.class);

        EqualsMatcher<String> methodNameMatcher = new EqualsMatcher<String>("run");
        EqualsMatcher<String> classNameMatcher = new EqualsMatcher<String>(MathGame.class.getName());

        //第25行
        String line = "25";
        Enhancer enhancer = new Enhancer(listener, true, false, line, classNameMatcher, null, methodNameMatcher);

        ClassLoader inClassLoader = MathGame.class.getClassLoader();
        String className = MathGame.class.getName();
        Class<?> classBeingRedefined = MathGame.class;

        ClassNode classNode = AsmUtils.loadClass(MathGame.class);

        byte[] classfileBuffer = AsmUtils.toBytes(classNode);

        byte[] result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, classfileBuffer);

        ClassNode resultClassNode1 = AsmUtils.toClassNode(result);

        result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, result);

        ClassNode resultClassNode2 = AsmUtils.toClassNode(result);

        MethodNode resultMethodNode1 = AsmUtils.findMethods(resultClassNode1.methods, "run").get(0);
        MethodNode resultMethodNode2 = AsmUtils.findMethods(resultClassNode2.methods, "run").get(0);

        Assertions
                .assertThat(AsmUtils
                        .findMethodInsnNode(resultMethodNode1, Type.getInternalName(SpyAPI.class), "atLineNumber").size())
                .isEqualTo(AsmUtils.findMethodInsnNode(resultMethodNode2, Type.getInternalName(SpyAPI.class), "atLineNumber")
                        .size());

        String string = Decompiler.decompile(result);

        System.err.println(string);
    }


    /**
     * 测试line命令使用的LineNumber的增强
     * (方法退出前)
     */
    @Test
    public void testLineWithLineNumberBeforeMethodExit() throws Throwable {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        TestHelper.appendSpyJar(instrumentation);

        ArthasBootstrap.getInstance(instrumentation, "ip=127.0.0.1");

        AdviceListener listener = Mockito.mock(AdviceListener.class);

        EqualsMatcher<String> methodNameMatcher = new EqualsMatcher<String>("run");
        EqualsMatcher<String> classNameMatcher = new EqualsMatcher<String>(MathGame.class.getName());

        //方法退出前
        String line = "-1";
        Enhancer enhancer = new Enhancer(listener, true, false, line, classNameMatcher, null, methodNameMatcher);

        ClassLoader inClassLoader = MathGame.class.getClassLoader();
        String className = MathGame.class.getName();
        Class<?> classBeingRedefined = MathGame.class;

        ClassNode classNode = AsmUtils.loadClass(MathGame.class);

        byte[] classfileBuffer = AsmUtils.toBytes(classNode);

        byte[] result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, classfileBuffer);

        ClassNode resultClassNode1 = AsmUtils.toClassNode(result);

        result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, result);

        ClassNode resultClassNode2 = AsmUtils.toClassNode(result);

        MethodNode resultMethodNode1 = AsmUtils.findMethods(resultClassNode1.methods, "run").get(0);
        MethodNode resultMethodNode2 = AsmUtils.findMethods(resultClassNode2.methods, "run").get(0);

        Assertions
                .assertThat(AsmUtils
                        .findMethodInsnNode(resultMethodNode1, Type.getInternalName(SpyAPI.class), "atLineNumber").size())
                .isEqualTo(AsmUtils.findMethodInsnNode(resultMethodNode2, Type.getInternalName(SpyAPI.class), "atLineNumber")
                        .size());

        String string = Decompiler.decompile(result);

        System.err.println(string);
    }

    /**
     * 测试line命令使用的LineCode的增强
     */
    @Test
    public void testLineWithLine() throws Throwable {
        Instrumentation instrumentation = ByteBuddyAgent.install();

        TestHelper.appendSpyJar(instrumentation);

        ArthasBootstrap.getInstance(instrumentation, "ip=127.0.0.1");

        AdviceListener listener = Mockito.mock(AdviceListener.class);

        EqualsMatcher<String> methodNameMatcher = new EqualsMatcher<String>("run");
        EqualsMatcher<String> classNameMatcher = new EqualsMatcher<String>(MathGame.class.getName());

        // 通过 jad --lineCode demo.MathGame run 找到
        String lineCode = "fbea-1";
        Enhancer enhancer = new Enhancer(listener, true, false, lineCode, classNameMatcher, null, methodNameMatcher);

        ClassLoader inClassLoader = MathGame.class.getClassLoader();
        String className = MathGame.class.getName();
        Class<?> classBeingRedefined = MathGame.class;

        ClassNode classNode = AsmUtils.loadClass(MathGame.class);

        byte[] classfileBuffer = AsmUtils.toBytes(classNode);

        byte[] result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, classfileBuffer);

        ClassNode resultClassNode1 = AsmUtils.toClassNode(result);

        result = enhancer.transform(inClassLoader, className, classBeingRedefined, null, result);

        ClassNode resultClassNode2 = AsmUtils.toClassNode(result);

        MethodNode resultMethodNode1 = AsmUtils.findMethods(resultClassNode1.methods, "run").get(0);
        MethodNode resultMethodNode2 = AsmUtils.findMethods(resultClassNode2.methods, "run").get(0);

        Assertions
                .assertThat(AsmUtils
                        .findMethodInsnNode(resultMethodNode1, Type.getInternalName(SpyAPI.class), "atLineCode").size())
                .isEqualTo(AsmUtils.findMethodInsnNode(resultMethodNode2, Type.getInternalName(SpyAPI.class), "atLineCode")
                        .size());

        String string = Decompiler.decompile(result);

        System.err.println(string);
    }

}
