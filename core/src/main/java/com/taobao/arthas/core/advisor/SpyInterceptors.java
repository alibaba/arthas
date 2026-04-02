package com.taobao.arthas.core.advisor;

import java.arthas.SpyAPI;

import com.alibaba.bytekit.asm.binding.Binding;
import com.alibaba.bytekit.asm.interceptor.annotation.AtEnter;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExceptionExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtExit;
import com.alibaba.bytekit.asm.interceptor.annotation.AtInvoke;
import com.alibaba.bytekit.asm.interceptor.annotation.AtInvokeException;

/**
 * Spy拦截器集合类
 *
 * <p>此类定义了一系列静态内部类，每个内部类都是一个拦截器，用于在方法的不同执行时机进行拦截。
 * 这些拦截器通过ByteKit库的注解定义，在字节码层面插入到目标方法中，实现方法调用的监控和追踪。</p>
 *
 * <p><b>拦截器分类：</b></p>
 * <ol>
 *   <li><b>SpyInterceptor1/2/3：</b>用于watch命令，拦截方法的进入、正常退出和异常退出</li>
 *   <li><b>SpyTraceInterceptor1/2/3：</b>用于trace命令，拦截方法内部的调用（排除基本类型和SpyAPI本身）</li>
 *   <li><b>SpyTraceExcludeJDKInterceptor1/2/3：</b>用于trace命令，拦截方法内部的调用（排除所有JDK类）</li>
 * </ol>
 *
 * <p><b>设计说明：</b></p>
 * <ul>
 *   <li>所有拦截器方法都是静态方法，通过@Binding注解自动绑定方法上下文</li>
 *   <li>使用inline=true进行内联优化，提高性能</li>
 *   <li>通过excludes参数排除不需要拦截的类，避免无限递归和性能问题</li>
 *   <li>每个拦截器最终都调用SpyAPI的静态方法，将控制权转交给SpyImpl处理</li>
 * </ul>
 *
 * <p><b>注解说明：</b></p>
 * <ul>
 *   <li>@AtEnter：方法进入时触发</li>
 *   <li>@AtExit：方法正常退出时触发</li>
 *   <li>@AtExceptionExit：方法异常退出时触发</li>
 *   <li>@AtInvoke：方法内部调用其他方法时触发</li>
 *   <li>@AtInvokeException：方法内部调用抛出异常时触发</li>
 * </ul>
 *
 * @author hengyunabc
 * @since 2020-06-05
 */
public class SpyInterceptors {

    /**
     * 方法进入拦截器
     *
     * <p>此拦截器在目标方法开始执行时被调用，用于实现"watch"命令的方法进入监控。
     * 拦截器会收集方法的基本信息（类、方法名、方法描述符、参数等），然后转发给SpyAPI处理。</p>
     *
     * <p><b>拦截时机：</b>方法刚进入，第一条指令执行之前</p>
     * <p><b>性能优化：</b>使用inline=true进行内联，减少方法调用开销</p>
     */
    public static class SpyInterceptor1 {

        /**
         * 在方法进入时调用
         *
         * <p>此方法会被字节码增强插入到目标方法的开头。</p>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 被拦截的类对象
         * @param methodInfo 方法信息，格式为 "methodName|methodDesc"
         * @param args 方法参数数组
         */
        @AtEnter(inline = true)
        public static void atEnter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args) {
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atEnter(clazz, methodInfo, target, args);
        }
    }

    /**
     * 方法正常退出拦截器
     *
     * <p>此拦截器在目标方法正常执行完成（未抛出异常）时被调用，用于实现"watch"命令的方法返回监控。
     * 拦截器会收集方法的返回值等信息，然后转发给SpyAPI处理。</p>
     *
     * <p><b>拦截时机：</b>方法正常返回，return语句执行之后</p>
     * <p><b>性能优化：</b>使用inline=true进行内联，减少方法调用开销</p>
     */
    public static class SpyInterceptor2 {
        /**
         * 在方法正常退出时调用
         *
         * <p>此方法会被字节码增强插入到目标方法的每个return语句之前。</p>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 被拦截的类对象
         * @param methodInfo 方法信息，格式为 "methodName|methodDesc"
         * @param args 方法参数数组
         * @param returnObj 方法的返回值
         */
        @AtExit(inline = true)
        public static void atExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args, @Binding.Return Object returnObj) {
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atExit(clazz, methodInfo, target, args, returnObj);
        }
    }

    /**
     * 方法异常退出拦截器
     *
     * <p>此拦截器在目标方法执行过程中抛出异常时被调用，用于实现"watch"命令的异常监控。
     * 拦截器会收集异常对象等信息，然后转发给SpyAPI处理。</p>
     *
     * <p><b>拦截时机：</b>方法抛出异常，异常被抛出之前</p>
     * <p><b>性能优化：</b>使用inline=true进行内联，减少方法调用开销</p>
     */
    public static class SpyInterceptor3 {
        /**
         * 在方法异常退出时调用
         *
         * <p>此方法会被字节码增强插入到目标方法的每个异常抛出点。</p>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 被拦截的类对象
         * @param methodInfo 方法信息，格式为 "methodName|methodDesc"
         * @param args 方法参数数组
         * @param throwable 方法抛出的异常对象
         */
        @AtExceptionExit(inline = true)
        public static void atExceptionExit(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.MethodInfo String methodInfo, @Binding.Args Object[] args,
                @Binding.Throwable Throwable throwable) {
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atExceptionExit(clazz, methodInfo, target, args, throwable);
        }
    }

    /**
     * 方法内部调用拦截器（调用前）
     *
     * <p>此拦截器用于实现"trace"命令，在方法内部调用其他方法之前进行拦截。
     * 通过跟踪方法内部的调用链路，可以帮助开发者了解方法的执行流程和调用关系。</p>
     *
     * <p><b>拦截时机：</b>方法内部调用其他方法之前</p>
     * <p><b>排除类：</b>SpyAPI本身和Java基本类型包装类，避免无限递归和性能问题</p>
     *
     * <p><b>排除原因说明：</b></p>
     * <ul>
     *   <li>SpyAPI：避免拦截Arthas自己的调用，防止无限递归</li>
     *   <li>基本类型包装类：避免拦截基本类型操作，这些操作过于频繁且无意义</li>
     * </ul>
     */
    public static class SpyTraceInterceptor1 {
        /**
         * 在方法内部调用其他方法之前调用
         *
         * <p>此方法会被字节码增强插入到目标方法内部每个方法调用的调用点之前。</p>
         *
         * <p><b>参数说明：</b></p>
         * <ul>
         *   <li>whenComplete=false：在方法调用之前触发</li>
         *   <li>excludes：排除的类列表，避免无限递归</li>
         * </ul>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 当前正在执行的类对象
         * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
         */
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
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }
    }

    /**
     * 方法内部调用拦截器（调用后）
     *
     * <p>此拦截器用于实现"trace"命令，在方法内部调用其他方法完成之后进行拦截。
     * 通过跟踪方法内部的调用链路，可以构建完整的方法调用树，展示方法的执行路径。</p>
     *
     * <p><b>拦截时机：</b>方法内部调用其他方法之后（正常返回）</p>
     * <p><b>排除类：</b>SpyAPI本身和Java基本类型包装类</p>
     *
     * <p><b>与SpyTraceInterceptor1的区别：</b></p>
     * <ul>
     *   <li>whenComplete=false：在调用前触发（SpyTraceInterceptor1）</li>
     *   <li>whenComplete=true：在调用后触发（此类）</li>
     * </ul>
     */
    public static class SpyTraceInterceptor2 {
        /**
         * 在方法内部调用其他方法之后调用
         *
         * <p>此方法会被字节码增强插入到目标方法内部每个方法调用的调用点之后。</p>
         *
         * <p><b>参数说明：</b></p>
         * <ul>
         *   <li>whenComplete=true：在方法调用之后触发</li>
         *   <li>excludes：排除的类列表，避免无限递归</li>
         * </ul>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 当前正在执行的类对象
         * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
         */
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
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }
    }

    /**
     * 方法内部调用异常拦截器
     *
     * <p>此拦截器用于实现"trace"命令，在方法内部调用其他方法抛出异常时进行拦截。
     * 通过跟踪方法调用链路中的异常情况，可以帮助开发者定位问题。</p>
     *
     * <p><b>拦截时机：</b>方法内部调用其他方法抛出异常时</p>
     * <p><b>排除类：</b>SpyAPI本身和Java基本类型包装类</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>跟踪方法调用链路中的异常传播</li>
     *   <li>分析异常的来源和影响范围</li>
     *   <li>定位隐藏的异常处理逻辑</li>
     * </ul>
     */
    public static class SpyTraceInterceptor3 {
        /**
         * 在方法内部调用其他方法抛出异常时调用
         *
         * <p>此方法会被字节码增强插入到目标方法内部每个方法调用的异常处理点。</p>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 当前正在执行的类对象
         * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
         * @param throwable 被调用方法抛出的异常对象
         */
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
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
        }
    }

    /**
     * 方法内部调用拦截器（排除JDK，调用前）
     *
     * <p>此拦截器与SpyTraceInterceptor1功能类似，但排除了所有JDK类（java.**）。
     * 用于实现更精确的trace命令，只跟踪用户代码的调用链路，忽略JDK内部的调用。</p>
     *
     * <p><b>拦截时机：</b>方法内部调用其他方法之前</p>
     * <p><b>排除类：</b>所有JDK类（java.**），包括Java标准库的所有类</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>只关注用户代码的调用链路</li>
     *   <li>减少JDK内部调用的干扰</li>
     *   <li>提高trace命令的可读性</li>
     * </ul>
     *
     * <p><b>与SpyTraceInterceptor1的区别：</b></p>
     * <ul>
     *   <li>SpyTraceInterceptor1：只排除SpyAPI和基本类型包装类</li>
     *   <li>此类：排除所有JDK类（java.**）</li>
     * </ul>
     */
    public static class SpyTraceExcludeJDKInterceptor1 {
        /**
         * 在方法内部调用其他方法之前调用（排除JDK类）
         *
         * <p>此方法会被字节码增强插入到目标方法内部每个非JDK方法调用的调用点之前。</p>
         *
         * <p><b>排除说明：</b></p>
         * <pre>excludes = "java.**"</pre>
         * 这表示排除所有以"java."开头的类，即整个Java标准库。</p>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 当前正在执行的类对象
         * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
         */
        @AtInvoke(name = "", inline = true, whenComplete = false, excludes = "java.**")
        public static void onInvoke(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atBeforeInvoke(clazz, invokeInfo, target);
        }
    }

    /**
     * 方法内部调用拦截器（排除JDK，调用后）
     *
     * <p>此拦截器与SpyTraceInterceptor2功能类似，但排除了所有JDK类（java.**）。
     * 用于实现更精确的trace命令，只跟踪用户代码的调用链路，忽略JDK内部的调用。</p>
     *
     * <p><b>拦截时机：</b>方法内部调用其他方法之后（正常返回）</p>
     * <p><b>排除类：</b>所有JDK类（java.**）</p>
     */
    public static class SpyTraceExcludeJDKInterceptor2 {
        /**
         * 在方法内部调用其他方法之后调用（排除JDK类）
         *
         * <p>此方法会被字节码增强插入到目标方法内部每个非JDK方法调用的调用点之后。</p>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 当前正在执行的类对象
         * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
         */
        @AtInvoke(name = "", inline = true, whenComplete = true, excludes = "java.**")
        public static void onInvokeAfter(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo) {
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atAfterInvoke(clazz, invokeInfo, target);
        }
    }

    /**
     * 方法内部调用异常拦截器（排除JDK）
     *
     * <p>此拦截器与SpyTraceInterceptor3功能类似，但排除了所有JDK类（java.**）。
     * 用于实现更精确的trace命令，只跟踪用户代码中的异常情况，忽略JDK内部的异常。</p>
     *
     * <p><b>拦截时机：</b>方法内部调用其他方法抛出异常时</p>
     * <p><b>排除类：</b>所有JDK类（java.**）</p>
     *
     * <p><b>使用场景：</b></p>
     * <ul>
     *   <li>只关注用户代码中的异常</li>
     *   <li>减少JDK内部异常的干扰</li>
     *   <li>提高异常分析的精确度</li>
     * </ul>
     */
    public static class SpyTraceExcludeJDKInterceptor3 {
        /**
         * 在方法内部调用其他方法抛出异常时调用（排除JDK类）
         *
         * <p>此方法会被字节码增强插入到目标方法内部每个非JDK方法调用的异常处理点。</p>
         *
         * @param target 目标对象（this引用），静态方法为null
         * @param clazz 当前正在执行的类对象
         * @param invokeInfo 调用信息，包含被调用类的类名、方法名、方法描述符和行号
         * @param throwable 被调用方法抛出的异常对象
         */
        @AtInvokeException(name = "", inline = true, excludes = "java.**")
        public static void onInvokeException(@Binding.This Object target, @Binding.Class Class<?> clazz,
                @Binding.InvokeInfo String invokeInfo, @Binding.Throwable Throwable throwable) {
            // 将拦截事件转发给SpyAPI处理
            SpyAPI.atInvokeException(clazz, invokeInfo, target, throwable);
        }
    }

}
