package com.taobao.arthas.bytekit.asm.location.filter;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.alibaba.arthas.deps.org.objectweb.asm.Type;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.ClassNode;
import com.alibaba.arthas.deps.org.objectweb.asm.tree.MethodNode;
import com.taobao.arthas.bytekit.asm.MethodProcessor;
import com.taobao.arthas.bytekit.asm.binding.Binding;
import com.taobao.arthas.bytekit.asm.interceptor.InterceptorProcessor;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtInvoke;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtInvokeException;
import com.taobao.arthas.bytekit.asm.interceptor.parser.DefaultInterceptorClassParser;
import com.taobao.arthas.bytekit.asm.location.LocationType;
import com.taobao.arthas.bytekit.utils.AsmUtils;
import com.taobao.arthas.bytekit.utils.Decompiler;
import com.taobao.arthas.bytekit.utils.MatchUtils;
import com.taobao.arthas.bytekit.utils.VerifyUtils;

/**
 * 
 * @author hengyunabc 2020-05-04
 *
 */
public class InvokeCheckLocationFilterTest {

    public static class SpyTraceInterceptor {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = { "java.**", "**SpyAPI**" })
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeMethodDeclaration String methodDesc, @Binding.InvokeMethodOwner String owner,
                @Binding.InvokeMethodName String methodName) {
            SpyAPI.atBeforeInvoke(clazz, owner, methodName, methodDesc, target);
        }

        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = { "java.**", "**SpyAPI**" })
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeMethodDeclaration String methodDesc, @Binding.InvokeMethodOwner String owner,
                @Binding.InvokeMethodName String methodName) {
            SpyAPI.atAfterInvoke(clazz, owner, methodName, methodDesc, target);
        }

        @AtInvokeException(name = "", inline = true, excludes = { "java.**", "**SpyAPI**" })
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeMethodDeclaration String methodDesc, @Binding.InvokeMethodOwner String owner,
                @Binding.InvokeMethodName String methodName, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, owner, methodName, methodDesc, target, throwable);
        }
    }
    
    public static class SpyTraceInterceptor2 {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = { "**SpyAPI**" })
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeMethodDeclaration String methodDesc, @Binding.InvokeMethodOwner String owner,
                @Binding.InvokeMethodName String methodName) {
            SpyAPI.atBeforeInvoke(clazz, owner, methodName, methodDesc, target);
        }

        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = { "**SpyAPI**" })
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeMethodDeclaration String methodDesc, @Binding.InvokeMethodOwner String owner,
                @Binding.InvokeMethodName String methodName) {
            SpyAPI.atAfterInvoke(clazz, owner, methodName, methodDesc, target);
        }

        @AtInvokeException(name = "", inline = true, excludes = { "**SpyAPI**" })
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeMethodDeclaration String methodDesc, @Binding.InvokeMethodOwner String owner,
                @Binding.InvokeMethodName String methodName, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, owner, methodName, methodDesc, target, throwable);
        }
    }

    public static class SpyAPI {

        public static void atBeforeInvoke(Class<?> clazz, String owner, String methodName, String methodDesc,
                Object target) {

        }

        public static void atInvokeException(Class<?> clazz, String owner, String methodName, String methodDesc,
                Object target, Throwable throwable) {

        }

        public static void atAfterInvoke(Class<?> clazz, String owner, String methodName, String methodDesc,
                Object target) {

        }

    }

    public static class TestAAA {

        int i = 0;
        long l = 0;

        public TestAAA() {
            xxx(1, 134L);
        }

        public String hello(String str) {

            String result = "";

            xxx(i, l);

            return result;

        }

        public String xxx(int i, long l) {
            return "" + i + l;
        }

    }

    @Test
    public void test() throws Exception {

        DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

        List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser
                .parse(SpyTraceInterceptor.class);

        ClassNode classNode = AsmUtils.loadClass(TestAAA.class);

        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, "*")) {
                matchedMethods.add(methodNode);
            }
        }

        LocationFilter enterFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class), "atBeforeInvoke",
                LocationType.INVOKE);
        LocationFilter existFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atInvokeException", LocationType.INVOKE_COMPLETED);
        LocationFilter exceptionFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atInvokeException", LocationType.INVOKE_EXCEPTION_EXIT);
        GroupLocationFilter groupLocationFilter = new GroupLocationFilter();
        groupLocationFilter.addFilter(enterFilter);
        groupLocationFilter.addFilter(existFilter);
        groupLocationFilter.addFilter(exceptionFilter);

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        byte[] bytes = AsmUtils.toBytes(classNode);
        VerifyUtils.asmVerify(bytes);

        VerifyUtils.instanceVerity(bytes);

        System.out.println(Decompiler.decompile(bytes));

        ClassNode classNode2 = AsmUtils.toClassNode(bytes);
        for (MethodNode methodNode : classNode2.methods) {
            if (!methodNode.name.equals("xxx")) {
                System.err.println("method name: " + methodNode.name);
                Assertions.assertThat(
                        AsmUtils.findMethodInsnNode(methodNode, Type.getInternalName(SpyAPI.class), "atBeforeInvoke"))
                        .size().isEqualTo(1);
            }
        }

    }
    
    
    @Test
    public void test2() throws Exception {

        DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

        List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser
                .parse(SpyTraceInterceptor2.class);

        ClassNode classNode = AsmUtils.loadClass(TestAAA.class);

        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, "*")) {
                matchedMethods.add(methodNode);
            }
        }

        LocationFilter enterFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class), "atBeforeInvoke",
                LocationType.INVOKE);
        LocationFilter existFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atInvokeException", LocationType.INVOKE_COMPLETED);
        LocationFilter exceptionFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atInvokeException", LocationType.INVOKE_EXCEPTION_EXIT);
        GroupLocationFilter groupLocationFilter = new GroupLocationFilter();
        groupLocationFilter.addFilter(enterFilter);
        groupLocationFilter.addFilter(existFilter);
        groupLocationFilter.addFilter(exceptionFilter);

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        for (MethodNode methodNode : matchedMethods) {
            MethodProcessor methodProcessor = new MethodProcessor(classNode, methodNode, groupLocationFilter);
            for (InterceptorProcessor interceptor : interceptorProcessors) {
                interceptor.process(methodProcessor);
            }
        }

        byte[] bytes = AsmUtils.toBytes(classNode);
        VerifyUtils.asmVerify(bytes);

        VerifyUtils.instanceVerity(bytes);

        System.out.println(Decompiler.decompile(bytes));

        ClassNode classNode2 = AsmUtils.toClassNode(bytes);
        for (MethodNode methodNode : classNode2.methods) {
            if (!methodNode.name.equals("xxx")) {
                System.err.println("method name: " + methodNode.name);
                Assertions.assertThat(
                        AsmUtils.findMethodInsnNode(methodNode, Type.getInternalName(SpyAPI.class), "atBeforeInvoke"))
                        .size().isEqualTo(1);
            }
        }

    }

}
