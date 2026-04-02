package com.taobao.arthas.core.advisor;

/**
 * 通知监听器接口
 * <p>
 * 定义了方法增强通知的监听器接口，用于在目标方法的不同执行阶段接收通知。
 * 当被增强的方法在特定接入点（Before、AfterReturning、AfterThrowing）执行时，
 * 会触发相应的监听器方法，允许用户在这些切入点执行自定义逻辑。
 * </p>
 * <p>
 * 该接口采用观察者模式，Arthas在方法执行的关键点会调用监听器的相应方法，
 * 从而实现对目标方法的监控、统计、日志记录等功能。
 * </p>
 * <p>
 * 典型实现类为 {@link AdviceListenerAdapter}，提供了默认实现和额外的辅助方法。
 * </p>
 *
 * @author vlinux
 * @since 2015-05-17
 */
public interface AdviceListener {

    /**
     * 获取监听器的唯一标识ID
     * <p>
     * 每个监听器实例都有一个唯一的ID，用于标识和管理该监听器。
     * </p>
     *
     * @return 监听器的唯一标识符
     */
    long id();

    /**
     * 监听器创建时的回调方法
     * <p>
     * 当监听器被注册到AdviceListenerManager时触发此方法。
     * 可以在此方法中执行初始化操作，如分配资源、设置初始状态等。
     * </p>
     */
    void create();

    /**
     * 监听器销毁时的回调方法
     * <p>
     * 当监听器从AdviceListenerManager中注销时触发此方法。
     * 可以在此方法中执行清理操作，如释放资源、保存统计数据等。
     * </p>
     */
    void destroy();

    /**
     * 前置通知（Before Advice）
     * <p>
     * 在目标方法执行之前触发此回调。
     * 此时方法尚未执行，可以访问调用参数，但无法获取返回值或异常信息。
     * </p>
     * <p>
     * 典型应用场景：
     * <ul>
     * <li>记录方法入参</li>
     * <li>统计方法调用次数</li>
     * <li>检查参数合法性</li>
     * <li>修改参数值（谨慎使用）</li>
     * </ul>
     * </p>
     *
     * @param clazz      目标方法所在的Class对象
     * @param methodName 目标方法的名称
     * @param methodDesc 目标方法的描述符（JVM格式，如"(Ljava/lang/String;)V"）
     * @param target     目标对象实例
     *                   如果是实例方法，则为方法被调用的对象
     *                   如果是静态方法，则为null
     * @param args       方法调用的参数数组
     * @throws Throwable 如果通知执行过程中抛出异常，将影响目标方法的执行
     */
    void before(
            Class<?> clazz, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable;

    /**
     * 返回通知（After Returning Advice）
     * <p>
     * 在目标方法正常执行完毕并返回后触发此回调。
     * 此时方法已成功执行完成，可以访问返回值，但不会有异常信息。
     * </p>
     * <p>
     * 典型应用场景：
     * <ul>
     * <li>记录方法返回值</li>
     * <li>统计方法执行时间</li>
     * <li>验证返回值合法性</li>
     * <li>修改返回值（谨慎使用）</li>
     * </ul>
     * </p>
     *
     * @param clazz        目标方法所在的Class对象
     * @param methodName   目标方法的名称
     * @param methodDesc   目标方法的描述符（JVM格式）
     * @param target       目标对象实例
     *                     如果是实例方法，则为方法被调用的对象
     *                     如果是静态方法，则为null
     * @param args         方法调用的参数数组
     * @param returnObject 方法的返回结果
     *                     如果是有返回值的方法，则为返回的对象
     *                     如果是void方法，则为null
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    void afterReturning(
            Class<?> clazz, String methodName, String methodDesc,
            Object target, Object[] args,
            Object returnObject) throws Throwable;

    /**
     * 异常通知（After Throwing Advice）
     * <p>
     * 在目标方法执行过程中抛出异常时触发此回调。
     * 此时方法因异常而中断，可以访问异常信息，但没有返回值。
     * </p>
     * <p>
     * 典型应用场景：
     * <ul>
     * <li>记录异常信息</li>
     * <li>统计异常发生频率</li>
     * <li>异常监控和告警</li>
     * <li>异常降级处理</li>
     * </ul>
     * </p>
     *
     * @param clazz      目标方法所在的Class对象
     * @param methodName 目标方法的名称
     * @param methodDesc 目标方法的描述符（JVM格式）
     * @param target     目标对象实例
     *                   如果是实例方法，则为方法被调用的对象
     *                   如果是静态方法，则为null
     * @param args       方法调用的参数数组
     * @param throwable  目标方法抛出的异常对象
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    void afterThrowing(
            Class<?> clazz, String methodName, String methodDesc,
            Object target, Object[] args,
            Throwable throwable) throws Throwable;

}
