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
public class GroupFilterTest {

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

    public static class SpyTraceInterceptor {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = { "java.**", "**SpyAPI**" })
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }

        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = { "java.**", "**SpyAPI**" })
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }

        @AtInvokeException(name = "", inline = true, excludes = { "java.**", "**SpyAPI**" })
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
        }
    }

    public static class SpyTraceInterceptor2 {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = { "**SpyAPI**" })
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }

        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = { "**SpyAPI**" })
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }

        @AtInvokeException(name = "", inline = true, excludes = { "**SpyAPI**" })
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
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

        public static void atBeforeInvoke(Class<?> clazz, String invokeInfo,
                Object target) {

        }

        public static void atInvokeException(Class<?> clazz, String invokeInfo,
                Object target, Throwable throwable) {

        }

        public static void atAfterInvoke(Class<?> clazz, String invokeInfo,
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
        boolean skipJDKTrace = false;

        DefaultInterceptorClassParser defaultInterceptorClassParser = new DefaultInterceptorClassParser();

        List<InterceptorProcessor> interceptorProcessors = defaultInterceptorClassParser
                .parse(SpyInterceptor.class);
        
        Class<?> spyTraceInterceptorClass = SpyTraceInterceptor2.class;
        if (skipJDKTrace == false) {
            spyTraceInterceptorClass = SpyTraceInterceptor.class;
        }
        List<InterceptorProcessor> traceInvokeProcessors = defaultInterceptorClassParser
                .parse(spyTraceInterceptorClass);
        interceptorProcessors.addAll(traceInvokeProcessors);
        

        ClassNode classNode = AsmUtils.loadClass(TestAAA.class);

        List<MethodNode> matchedMethods = new ArrayList<MethodNode>();
        for (MethodNode methodNode : classNode.methods) {
            if (MatchUtils.wildcardMatch(methodNode.name, "*")) {
                matchedMethods.add(methodNode);
            }
        }

        GroupLocationFilter groupLocationFilter = new GroupLocationFilter();
        
        LocationFilter enterFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atEnter",
                LocationType.ENTER);
        LocationFilter existFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class), "atExit",
                LocationType.EXIT);
        LocationFilter exceptionFilter = new InvokeContainLocationFilter(Type.getInternalName(SpyAPI.class),
                "atExceptionExit", LocationType.EXCEPTION_EXIT);
        
        groupLocationFilter.addFilter(enterFilter);
        groupLocationFilter.addFilter(existFilter);
        groupLocationFilter.addFilter(exceptionFilter);
        
        LocationFilter invokeBeforeFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class), "atBeforeInvoke",
                LocationType.INVOKE);
        LocationFilter invokeAfterFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atInvokeException", LocationType.INVOKE_COMPLETED);
        LocationFilter invokeExceptionFilter = new InvokeCheckLocationFilter(Type.getInternalName(SpyAPI.class),
                "atInvokeException", LocationType.INVOKE_EXCEPTION_EXIT);
        groupLocationFilter.addFilter(invokeBeforeFilter);
        groupLocationFilter.addFilter(invokeAfterFilter);
        groupLocationFilter.addFilter(invokeExceptionFilter);
        

        for(int i = 0 ; i < 20 ; ++i) {
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
