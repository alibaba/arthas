package com.taobao.arthas.core.advisor;

import java.arthas.SpyAPI;

import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtInvoke;
import com.alibaba.bytekit.asm.interceptor.annotation.AtInvokeException;
import com.taobao.arthas.core.command.monitor200.LineHelper;

/**
 * 
 * @author hengyunabc 2020-06-05
 *
 */
public class SpyInterceptors {

    public static class SpyInterceptor1 {

        @AtEnter(inline = true)
        public static void atEnter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args) {
            SpyAPI.atEnter(clazz, methodInfo, target, args);
        }
    }
    
    public static class SpyInterceptor2 {
        @AtExit(inline = true)
        public static void atExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args, @Binding.Return Object returnObj) {
            SpyAPI.atExit(clazz, methodInfo, target, args, returnObj);
        }
    }
    
    public static class SpyInterceptor3 {
        @AtExceptionExit(inline = true)
        public static void atExceptionExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                @Binding.Throwable Throwable throwable) {
            SpyAPI.atExceptionExit(clazz, methodInfo, target, args, throwable);
        }
    }

    public static class SpyTraceInterceptor1 {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = {"java.arthas.SpyAPI", "java.lang.Byte"
                , "java.lang.Boolean"
                , "java.lang.Short"
                , "java.lang.Character"
                , "java.lang.Integer"
                , "java.lang.Float"
                , "java.lang.Long"
                , "java.lang.Double"})
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }
    }
    
    public static class SpyTraceInterceptor2 {
        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = {"java.arthas.SpyAPI", "java.lang.Byte"
                , "java.lang.Boolean"
                , "java.lang.Short"
                , "java.lang.Character"
                , "java.lang.Integer"
                , "java.lang.Float"
                , "java.lang.Long"
                , "java.lang.Double"})
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }
    }
    
    public static class SpyTraceInterceptor3 {
        @AtInvokeException(name = "", inline = true, excludes = {"java.arthas.SpyAPI", "java.lang.Byte"
                , "java.lang.Boolean"
                , "java.lang.Short"
                , "java.lang.Character"
                , "java.lang.Integer"
                , "java.lang.Float"
                , "java.lang.Long"
                , "java.lang.Double"})
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
        }
    }

    public static class SpyTraceExcludeJDKInterceptor1 {
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = "java.**")
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }
    }

    public static class SpyTraceExcludeJDKInterceptor2 {
        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = "java.**")
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }
    }

    public static class SpyTraceExcludeJDKInterceptor3 {
        @AtInvokeException(name = "", inline = true, excludes = "java.**")
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo, @Binding.Throwable Throwable throwable) {
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
        }
    }

    /**
     * 行观测（line命令）使用的 Interceptor
     * 为什么要用两个参数一模一样的方法？
     * 场景：两个人分别使用 LineNumber 和 LineCode 进行观测，如果只用一个方法，其 AdviceLister 能正常注册和监听回调吗？
     * 不可以！因为 LineNumber 和 LineCode 不能一对一映射，也就是不能进行转换（eg.需要在方法退出前插桩时）
     * 在现有的 AdviceListener 注册和查询机制下，使用的key是增强时就已经确定的了（如类名、方法签名），所以使用 LineNumber 和 LineCode 所计算出来的 key 是不一致的，也没有办法进行转换
     */
    public static class SpyLineInterceptor {

        public static void atLineCode(@Binding.This Object target, @Binding.Class Class<?> clazz,
                                              @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                                              @Binding.LocalVars(excludePattern = LineHelper.LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER) Object[] vars,
                                              @Binding.LocalVarNames(excludePattern = LineHelper.LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER) String[] varNames,
                                              //这个是由Arthas传递的动态变化值，无法使用binding
                                              String location) {
            SpyAPI.atLineCode(clazz, methodInfo, target, args, location, vars, varNames);
        }

        public static void atLineNumber(@Binding.This Object target, @Binding.Class Class<?> clazz,
                                              @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                                              @Binding.LocalVars(excludePattern = LineHelper.LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER) Object[] vars,
                                              @Binding.LocalVarNames(excludePattern = LineHelper.LOCAL_VARIABLES_NAME_EXCLUDE_MATCHER) String[] varNames,
                                              //这个是由Arthas传递的动态变化值，无法使用binding
                                              String location) {
            SpyAPI.atLineNumber(clazz, methodInfo, target, args, location, vars, varNames);
        }

    }

}
