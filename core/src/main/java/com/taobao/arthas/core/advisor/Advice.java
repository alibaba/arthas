package com.taobao.arthas.core.advisor;

/**
 * 通知（Advice）类
 * <p>
 * 封装了方法调用过程中的上下文信息，在AOP（面向切面编程）中用于传递方法调用的相关数据。
 * 当一个方法被增强时，Advice对象会包含该方法在调用点的所有关键信息，
 * 包括类加载器、目标类、方法信息、调用参数、返回值、异常等。
 * </p>
 * <p>
 * 该类是不可变类（Immutable），所有字段都是final的，创建后不能修改。
 * 通过工厂方法 {@link #newForBefore}、{@link #newForAfterReturning}、
 * {@link #newForAfterThrowing} 来创建不同场景下的Advice实例。
 * </p>
 *
 * @author vlinux
 * @since 2015-05-20
 */
public class Advice {

    /**
     * 类加载器
     * 用于加载目标类的类加载器实例
     */
    private final ClassLoader loader;

    /**
     * 目标类
     * 被增强的方法所在的Class对象
     */
    private final Class<?> clazz;

    /**
     * 目标方法
     * 被增强的方法的封装对象，包含方法名、方法描述等信息
     */
    private final ArthasMethod method;

    /**
     * 目标对象实例
     * 如果是实例方法，则为该方法被调用的对象实例
     * 如果是静态方法，则为null
     */
    private final Object target;

    /**
     * 方法调用参数数组
     * 调用被增强方法时传入的参数列表
     */
    private final Object[] params;

    /**
     * 方法返回值
     * 方法正常执行完成后的返回结果
     * 如果是void方法或尚未返回，则为null
     */
    private final Object returnObj;

    /**
     * 抛出的异常对象
     * 方法执行过程中抛出的异常
     * 如果没有异常或尚未抛出，则为null
     */
    private final Throwable throwExp;

    /**
     * 是否是方法执行前的通知
     * 标识当前Advice是否在方法执行前（Before）创建
     */
    private final boolean isBefore;

    /**
     * 是否是方法抛出异常的通知
     * 标识当前Advice是否在方法抛出异常后（AfterThrowing）创建
     */
    private final boolean isThrow;

    /**
     * 是否是方法正常返回的通知
     * 标识当前Advice是否在方法正常返回后（AfterReturning）创建
     */
    private final boolean isReturn;

    /**
     * 判断是否是方法执行前的通知
     *
     * @return 如果是Before通知返回true，否则返回false
     */
    public boolean isBefore() {
        return isBefore;
    }

    /**
     * 判断是否是方法正常返回后的通知
     *
     * @return 如果是AfterReturning通知返回true，否则返回false
     */
    public boolean isAfterReturning() {
        return isReturn;
    }

    /**
     * 判断是否是方法抛出异常后的通知
     *
     * @return 如果是AfterThrowing通知返回true，否则返回false
     */
    public boolean isAfterThrowing() {
        return isThrow;
    }

    /**
     * 获取类加载器
     *
     * @return 加载目标类的类加载器实例
     */
    public ClassLoader getLoader() {
        return loader;
    }

    /**
     * 获取目标对象实例
     *
     * @return 如果是实例方法返回目标对象，静态方法返回null
     */
    public Object getTarget() {
        return target;
    }

    /**
     * 获取方法调用参数
     *
     * @return 方法调用的参数数组
     */
    public Object[] getParams() {
        return params;
    }

    /**
     * 获取方法返回值
     *
     * @return 方法的返回对象，void方法或尚未返回时为null
     */
    public Object getReturnObj() {
        return returnObj;
    }

    /**
     * 获取抛出的异常
     *
     * @return 方法抛出的异常对象，无异常时为null
     */
    public Throwable getThrowExp() {
        return throwExp;
    }

    /**
     * 获取目标类
     *
     * @return 被增强方法所在的Class对象
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * 获取目标方法
     *
     * @return 被增强方法的封装对象
     */
    public ArthasMethod getMethod() {
        return method;
    }

    /**
     * 私有构造方法
     * <p>
     * 创建一个Advice实例，初始化所有字段。
     * 根据access参数的位掩码值判断当前Advice的类型（Before/AfterReturning/AfterThrowing）。
     * 通过按位与运算判断access中包含哪些接入点标志。
     * </p>
     *
     * @param loader    类加载器，用于加载目标类
     * @param clazz     目标类，被增强方法所在的Class对象
     * @param method    方法，被增强的ArthasMethod对象
     * @param target    目标对象实例，实例方法时为对象本身，静态方法时为null
     * @param params    方法调用参数数组
     * @param returnObj 方法返回值，仅在AfterReturning场景下有效
     * @param throwExp  抛出的异常，仅在AfterThrowing场景下有效
     * @param access    接入点标志位掩码，用于判断当前Advice的类型
     */
    private Advice(
            ClassLoader loader,
            Class<?> clazz,
            ArthasMethod method,
            Object target,
            Object[] params,
            Object returnObj,
            Throwable throwExp,
            int access) {
        // 保存类加载器
        this.loader = loader;
        // 保存目标类
        this.clazz = clazz;
        // 保存目标方法
        this.method = method;
        // 保存目标对象
        this.target = target;
        // 保存方法参数
        this.params = params;
        // 保存返回值
        this.returnObj = returnObj;
        // 保存异常对象
        this.throwExp = throwExp;

        // 通过按位与运算判断是否包含Before接入点标志
        // 如果access和ACCESS_BEFORE的位掩码进行与运算后等于ACCESS_BEFORE的值，说明包含Before标志
        isBefore = (access & AccessPoint.ACCESS_BEFORE.getValue()) == AccessPoint.ACCESS_BEFORE.getValue();

        // 通过按位与运算判断是否包含AfterThrowing接入点标志
        isThrow = (access & AccessPoint.ACCESS_AFTER_THROWING.getValue()) == AccessPoint.ACCESS_AFTER_THROWING.getValue();

        // 通过按位与运算判断是否包含AfterReturning接入点标志
        isReturn = (access & AccessPoint.ACCESS_AFTER_RETUNING.getValue()) == AccessPoint.ACCESS_AFTER_RETUNING.getValue();
    }

    /**
     * 创建方法执行前的Advice实例
     * <p>
     * 在方法体执行之前创建Advice对象，此时没有返回值和异常信息。
     * </p>
     *
     * @param loader  类加载器，用于加载目标类
     * @param clazz   目标类，被增强方法所在的Class对象
     * @param method  方法，被增强的ArthasMethod对象
     * @param target  目标对象实例，实例方法时为对象本身，静态方法时为null
     * @param params  方法调用参数数组
     * @return Before场景的Advice实例
     */
    public static Advice newForBefore(ClassLoader loader,
                                      Class<?> clazz,
                                      ArthasMethod method,
                                      Object target,
                                      Object[] params) {
        return new Advice(
                loader,          // 类加载器
                clazz,           // 目标类
                method,          // 目标方法
                target,          // 目标对象
                params,          // 方法参数
                null,            // Before场景下返回值为null
                null,            // Before场景下异常为null
                AccessPoint.ACCESS_BEFORE.getValue()  // 标识为Before接入点
        );
    }

    /**
     * 创建方法正常返回后的Advice实例
     * <p>
     * 在方法正常执行完毕并返回结果后创建Advice对象，此时包含返回值但无异常。
     * </p>
     *
     * @param loader     类加载器，用于加载目标类
     * @param clazz      目标类，被增强方法所在的Class对象
     * @param method     方法，被增强的ArthasMethod对象
     * @param target     目标对象实例，实例方法时为对象本身，静态方法时为null
     * @param params     方法调用参数数组
     * @param returnObj  方法返回值，方法执行后的返回结果
     * @return AfterReturning场景的Advice实例
     */
    public static Advice newForAfterReturning(ClassLoader loader,
                                              Class<?> clazz,
                                              ArthasMethod method,
                                              Object target,
                                              Object[] params,
                                              Object returnObj) {
        return new Advice(
                loader,          // 类加载器
                clazz,           // 目标类
                method,          // 目标方法
                target,          // 目标对象
                params,          // 方法参数
                returnObj,       // 保存返回值
                null,            // AfterReturning场景下异常为null
                AccessPoint.ACCESS_AFTER_RETUNING.getValue()  // 标识为AfterReturning接入点
        );
    }

    /**
     * 创建方法抛出异常后的Advice实例
     * <p>
     * 在方法执行过程中抛出异常时创建Advice对象，此时包含异常信息但无返回值。
     * </p>
     *
     * @param loader    类加载器，用于加载目标类
     * @param clazz     目标类，被增强方法所在的Class对象
     * @param method    方法，被增强的ArthasMethod对象
     * @param target    目标对象实例，实例方法时为对象本身，静态方法时为null
     * @param params    方法调用参数数组
     * @param throwExp  方法抛出的异常对象
     * @return AfterThrowing场景的Advice实例
     */
    public static Advice newForAfterThrowing(ClassLoader loader,
                                             Class<?> clazz,
                                             ArthasMethod method,
                                             Object target,
                                             Object[] params,
                                             Throwable throwExp) {
        return new Advice(
                loader,          // 类加载器
                clazz,           // 目标类
                method,          // 目标方法
                target,          // 目标对象
                params,          // 方法参数
                null,            // AfterThrowing场景下返回值为null
                throwExp,        // 保存异常对象
                AccessPoint.ACCESS_AFTER_THROWING.getValue()  // 标识为AfterThrowing接入点
        );

    }

}
