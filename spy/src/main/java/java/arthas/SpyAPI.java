package java.arthas;

/**
 * Arthas Spy API - Java代码埋点和监控的核心接口
 *
 * <pre>
 * 什么是adviceId？
 * 一个adviceId对应一个可以被trace/monitor/watch命令匹配到的拦截点。
 * 例如：某个类的某个方法，它的enter（进入）、end（退出）、exception（异常）这三个事件
 * 共用同一个adviceId，一旦分配就不会再改变。
 *
 * 同一个方法如果被trace，也会有一个adviceId。
 * 该方法内的所有invoke（调用）都统一处理，认为是同一个adviceId。
 * 但如果有匹配到不同的invoke该如何分配？
 * 这就有些复杂了。
 *
 * 实际上，就是把所有可以插入代码的地方都分类好，那么怎么分类呢？
 * 或者说，同一种匹配就是同一种adviceId？
 *
 * 例如入参包含：class（类）、method（方法），这是固定的，
 * 或者是某个行号，或者是某个表达式。
 *
 * AOP（面向切面编程）插入的叫adviceId，
 * command（命令）插入的叫ListenerId？
 * </pre>
 *
 * 该类是Arthas实现Java代码无侵入式监控的核心API。
 * 通过在目标代码的关键位置插入对这些静态方法的调用，
 * Arthas能够在不修改源码的情况下，实现方法调用的拦截、监控和增强。
 *
 * 核心设计模式：
 * - 使用委托模式，将实际的处理逻辑委托给AbstractSpy实现
 * - 使用NOP（No Operation）模式，提供默认的空实现
 * - 使用volatile变量保证多线程环境下的可见性
 *
 * 主要功能：
 * - 方法入口拦截（atEnter）
 * - 方法正常退出拦截（atExit）
 * - 方法异常退出拦截（atExceptionExit）
 * - 方法调用前拦截（atBeforeInvoke）
 * - 方法调用后拦截（atAfterInvoke）
 * - 方法调用异常拦截（atInvokeException）
 *
 * @author hengyunabc
 */
public class SpyAPI {

    /**
     * 空操作Spy实例（NOP Spy）
     *
     * 这是一个默认的Spy实现，所有方法都是空实现，不执行任何操作。
     * 当Arthas未激活或被禁用时，使用这个实例可以避免对应用性能的影响。
     * 使用final修饰，确保该实例不可变，全局唯一。
     */
    public static final AbstractSpy NOPSPY = new NopSpy();

    /**
     * 当前活跃的Spy实例
     *
     * 使用volatile关键字修饰，确保在多线程环境下的可见性和有序性。
     * 任何线程对该变量的修改都能立即被其他线程看到。
     *
     * 默认值为NOPSPY，即默认不进行任何拦截操作。
     * 当Arthas激活时，会被替换为实际的Spy实现。
     */
    private static volatile AbstractSpy spyInstance = NOPSPY;

    /**
     * 标识SpyAPI是否已经初始化
     *
     * 使用volatile关键字修饰，确保多线程环境下的可见性。
     * 当Arthas完成初始化后，该标志位会被设置为true。
     */
    public static volatile boolean INITED;

    /**
     * 获取当前活跃的Spy实例
     *
     * @return 当前使用的AbstractSpy实例，可能是NOPSPY或实际的Spy实现
     */
    public static AbstractSpy getSpy() {
        return spyInstance;
    }

    /**
     * 设置当前活跃的Spy实例
     *
     * 该方法用于在运行时动态切换Spy实现。
     * 通常在Arthas激活时，会将NOPSPY替换为实际的Spy实现。
     *
     * @param spy 要设置的Spy实例
     */
    public static void setSpy(AbstractSpy spy) {
        spyInstance = spy;
    }

    /**
     * 将Spy实例设置为NOP（无操作）模式
     *
     * 调用此方法后，所有的拦截点都将不再执行任何操作，
     * 但不会移除已插入的代码，只是让这些代码变成空操作。
     *
     * 通常在以下场景使用：
     * - Arthas被禁用时
     * - 需要临时关闭监控时
     * - 应用关闭时清理资源
     */
    public static void setNopSpy() {
        setSpy(NOPSPY);
    }

    /**
     * 判断当前是否处于NOP模式
     *
     * @return 如果当前使用的是NOPSPY实例返回true，否则返回false
     */
    public static boolean isNopSpy() {
        return NOPSPY == spyInstance;
    }

    /**
     * 初始化SpyAPI
     *
     * 该方法将INITED标志设置为true，表示Arthas已完成初始化。
     * 通常在Arthas Agent启动时调用。
     */
    public static void init() {
        INITED = true;
    }

    /**
     * 判断SpyAPI是否已经初始化
     *
     * @return 如果已完成初始化返回true，否则返回false
     */
    public static boolean isInited() {
        return INITED;
    }

    /**
     * 销毁SpyAPI
     *
     * 该方法用于清理Arthas的资源：
     * 1. 将Spy实例重置为NOPSPY，停止所有拦截操作
     * 2. 将INITED标志设置为false
     *
     * 通常在以下场景使用：
     * - Arthas Agent被卸载时
     * - 应用关闭时
     * - 需要完全禁用Arthas时
     */
    public static void destroy() {
        // 重置为NOP模式，停止所有拦截
        setNopSpy();
        // 清除初始化标志
        INITED = false;
    }

    /**
     * 方法入口拦截点
     *
     * 该方法会在目标方法开始执行时被调用。
     * 通过在目标方法的第一行插入对该方法的调用，Arthas可以：
     * - 记录方法调用信息
     * - 统计方法调用次数和耗时
     * - 打印方法入参
     * - 实现watch命令的功能
     *
     * @param clazz 目标方法所在的类
     * @param methodInfo 方法信息，通常包含方法名、参数类型等
     * @param target 目标对象实例（如果是静态方法则为null）
     * @param args 方法调用时的参数数组
     */
    public static void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        // 委托给当前的Spy实例处理
        spyInstance.atEnter(clazz, methodInfo, target, args);
    }

    /**
     * 方法正常退出拦截点
     *
     * 该方法会在目标方法正常执行完成（即没有抛出异常）时被调用。
     * 通过在目标方法的每个return语句前插入对该方法的调用，Arthas可以：
     * - 记录方法返回值
     * - 统计方法执行时间
     * - 实现trace命令的功能
     * - 实现monitor命令的功能
     *
     * @param clazz 目标方法所在的类
     * @param methodInfo 方法信息，通常包含方法名、参数类型等
     * @param target 目标对象实例（如果是静态方法则为null）
     * @param args 方法调用时的参数数组
     * @param returnObject 方法的返回值
     */
    public static void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
            Object returnObject) {
        // 委托给当前的Spy实例处理
        spyInstance.atExit(clazz, methodInfo, target, args, returnObject);
    }

    /**
     * 方法异常退出拦截点
     *
     * 该方法会在目标方法抛出异常时被调用。
     * 通过在目标方法的异常处理路径上插入对该方法的调用，Arthas可以：
     * - 捕获和记录异常信息
     * - 统计方法异常率
     * - 实现trace命令的异常追踪功能
     *
     * @param clazz 目标方法所在的类
     * @param methodInfo 方法信息，通常包含方法名、参数类型等
     * @param target 目标对象实例（如果是静态方法则为null）
     * @param args 方法调用时的参数数组
     * @param throwable 抛出的异常对象
     */
    public static void atExceptionExit(Class<?> clazz, String methodInfo, Object target,
            Object[] args, Throwable throwable) {
        // 委托给当前的Spy实例处理
        spyInstance.atExceptionExit(clazz, methodInfo, target, args, throwable);
    }

    /**
     * 方法调用前拦截点
     *
     * 该方法会在目标方法内部调用另一个方法之前被调用。
     * 用于追踪方法调用链路，实现类似"调用链跟踪"的功能。
     *
     * @param clazz 包含调用的类
     * @param invokeInfo 被调用的方法信息
     * @param target 目标对象实例
     */
    public static void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
        // 委托给当前的Spy实例处理
        spyInstance.atBeforeInvoke(clazz, invokeInfo, target);
    }

    /**
     * 方法调用后拦截点
     *
     * 该方法会在目标方法内部调用另一个方法之后被调用。
     * 用于记录方法调用完成的状态。
     *
     * @param clazz 包含调用的类
     * @param invokeInfo 被调用的方法信息
     * @param target 目标对象实例
     */
    public static void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
        // 委托给当前的Spy实例处理
        spyInstance.atAfterInvoke(clazz, invokeInfo, target);
    }

    /**
     * 方法调用异常拦截点
     *
     * 该方法会在目标方法内部调用另一个方法抛出异常时被调用。
     * 用于追踪调用链中的异常传播。
     *
     * @param clazz 包含调用的类
     * @param invokeInfo 被调用的方法信息
     * @param target 目标对象实例
     * @param throwable 调用时抛出的异常对象
     */
    public static void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
        // 委托给当前的Spy实例处理
        spyInstance.atInvokeException(clazz, invokeInfo, target, throwable);
    }

    /**
     * 抽象Spy类 - 所有Spy实现的基类
     *
     * 该类定义了Spy API的核心接口方法，所有具体的Spy实现都需要继承此类。
     *
     * 设计模式：模板方法模式
     * - 定义了所有拦截点的抽象方法
     * - 具体的实现由子类提供
     *
     * 主要实现：
     * - NopSpy：空实现，不执行任何操作
     * - 其他实现可以提供实际的监控和增强逻辑
     */
    public static abstract class AbstractSpy {

        /**
         * 方法入口拦截
         *
         * @param clazz 目标方法所在的类
         * @param methodInfo 方法信息
         * @param target 目标对象实例
         * @param args 方法参数数组
         */
        public abstract void atEnter(Class<?> clazz, String methodInfo, Object target,
                Object[] args);

        /**
         * 方法正常退出拦截
         *
         * @param clazz 目标方法所在的类
         * @param methodInfo 方法信息
         * @param target 目标对象实例
         * @param args 方法参数数组
         * @param returnObject 方法返回值
         */
        public abstract void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Object returnObject);

        /**
         * 方法异常退出拦截
         *
         * @param clazz 目标方法所在的类
         * @param methodInfo 方法信息
         * @param target 目标对象实例
         * @param args 方法参数数组
         * @param throwable 抛出的异常对象
         */
        public abstract void atExceptionExit(Class<?> clazz, String methodInfo, Object target,
                Object[] args, Throwable throwable);

        /**
         * 方法调用前拦截
         *
         * @param clazz 包含调用的类
         * @param invokeInfo 被调用的方法信息
         * @param target 目标对象实例
         */
        public abstract void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target);

        /**
         * 方法调用后拦截
         *
         * @param clazz 包含调用的类
         * @param invokeInfo 被调用的方法信息
         * @param target 目标对象实例
         */
        public abstract void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target);

        /**
         * 方法调用异常拦截
         *
         * @param clazz 包含调用的类
         * @param invokeInfo 被调用的方法信息
         * @param target 目标对象实例
         * @param throwable 抛出的异常对象
         */
        public abstract void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable);
    }

    /**
     * 空操作Spy实现（NOP Spy）
     *
     * 这是AbstractSpy的一个默认实现，所有方法体都是空的。
     * 当Arthas未激活时，使用这个实现可以避免对应用性能的影响。
     *
     * 设计模式：
     * - Null Object模式：提供一个什么都不做的对象，避免null判断
     * - Singleton模式：全局唯一实例（NOPSPY常量）
     *
     * 性能优化：
     * - JIT编译器可以将空方法内联优化
     * - 不会产生任何对象分配
     * - 对应用性能的影响可以忽略不计
     */
    static class NopSpy extends AbstractSpy {

        /**
         * 空实现的方法入口拦截
         * 不执行任何操作
         */
        @Override
        public void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
            // 空实现，不执行任何操作
        }

        /**
         * 空实现的方法正常退出拦截
         * 不执行任何操作
         */
        @Override
        public void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Object returnObject) {
            // 空实现，不执行任何操作
        }

        /**
         * 空实现的方法异常退出拦截
         * 不执行任何操作
         */
        @Override
        public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Throwable throwable) {
            // 空实现，不执行任何操作
        }

        /**
         * 空实现的方法调用前拦截
         * 不执行任何操作
         */
        @Override
        public void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
            // 空实现，不执行任何操作
        }

        /**
         * 空实现的方法调用后拦截
         * 不执行任何操作
         */
        @Override
        public void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
            // 空实现，不执行任何操作
        }

        /**
         * 空实现的方法调用异常拦截
         * 不执行任何操作
         */
        @Override
        public void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
            // 空实现，不执行任何操作
        }

    }
}
