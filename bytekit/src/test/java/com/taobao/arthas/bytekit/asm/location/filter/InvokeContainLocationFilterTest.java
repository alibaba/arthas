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
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtEnter;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.taobao.arthas.bytekit.asm.interceptor.annotation.AtExit;
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
public class InvokeContainLocationFilterTest {

    public static class SpyInterceptor {

        @AtEnter(inline = true)
        public static void atEnter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodName String methodName, @Binding.MethodDesc String methodDesc,
                @Binding.Args Object[] args) {
            SpyAPI.atEnter(clazz, methodName, methodDesc, target, args);
        }

        @AtExit(inline = true)
        public static void atExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodName String methodName, @Binding.MethodDesc String methodDesc,
                @Binding.Args Object[] args, @Binding.Return Object returnObj) {
            SpyAPI.atExit(clazz, methodName, methodDesc, target, args, returnObj);
        }

        @AtExceptionExit(inline = true)
        public static void atExceptionExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodName String methodName, @Binding.MethodDesc String methodDesc,
                @Binding.Args Object[] args, @Binding.Throwable Throwable throwable) {
            SpyAPI.atExceptionExit(clazz, methodName, methodDesc, target, args, throwable);
        }
    }

    public static class SpyAPI {

        public static void atEnter(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args) {

        }

        public static void atExceptionExit(Class<?> clazz, String methodName, String methodDesc, Object target,
                Object[] args, Throwable throwable) {

        }

        public static void atExit(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
                Object returnObj) {

        }

    }

    public static class TestAAA {

        int i = 0;
        long l = 0;

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

        List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser.parse(SpyInterceptor.class);

        ClassNode classNode = AsmUtils.loadClass(TestAAA.class);

        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, "*")) {
                matchedMethods.add(methodNode);
            }
        }

        LocationFilter enterFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atEnter",
                LocationType.ENTER);
        LocationFilter existFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atExit",
                LocationType.EXIT);
        LocationFilter exceptionFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class),
                "atExceptionExit", LocationType.EXCEPTION_EXIT);
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

        System.out.println(Decompiler.decompile(bytes));

        ClassNode classNode2 = AsmUtils.toClassNode(bytes);
        for (MethodNode methodNode : classNode2.methods) {
            System.err.println("method name: " + methodNode.name);
            Assertions
                    .assertThat(AsmUtils.findMethodInsnNode(methodNode, Type.getInternalName(SpyAPI.class), "atEnter"))
                    .size().isEqualTo(1);
            Assertions
            .assertThat(AsmUtils.findMethodInsnNode(methodNode, Type.getInternalName(SpyAPI.class), "atExit"))
            .size().isEqualTo(1);
            Assertions
            .assertThat(AsmUtils.findMethodInsnNode(methodNode, Type.getInternalName(SpyAPI.class), "atExceptionExit"))
            .size().isEqualTo(1);
        }

    }

}
