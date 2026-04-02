package com.taobao.arthas.core.advisor;

import java.util.concurrent.atomic.AtomicLong;

import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.system.Process;
import com.taobao.arthas.core.shell.system.ProcessAware;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.StringUtils;

/**
 * 通知监听器适配器抽象类
 * <p>
 * 该类是 {@link AdviceListener} 接口的抽象适配器实现，提供了以下功能：
 * <ul>
 * <li>为每个监听器实例自动生成唯一ID</li>
 * <li>实现 ProcessAware 接口，支持与命令进程关联</li>
 * <li>提供默认的空实现 create() 和 destroy() 方法</li>
 * <li>将 AdviceListener 的方法转发给包含 ClassLoader 和 ArthasMethod 参数的重载方法</li>
 * <li>提供条件表达式判断、执行次数限制等辅助方法</li>
 * <li>支持详细输出模式（verbose）</li>
 * </ul>
 * </p>
 * <p>
 * 具体的命令监听器可以继承此类，只需要实现核心的 before、afterReturning、afterThrowing 方法，
 * 即可获得完整的通知监听功能。
 * </p>
 *
 * @author hengyunabc
 * @since 2020-05-20
 */
public abstract class AdviceListenerAdapter implements AdviceListener, ProcessAware {
    /**
     * ID生成器
     * 使用原子长整型保证线程安全，为每个监听器实例生成唯一的递增ID
     */
    private static final  AtomicLong ID_GENERATOR = new AtomicLong(0);

    /**
     * 关联的命令进程
     * 用于与Arthas命令行交互，如输出信息、终止命令等
     */
    private Process process;

    /**
     * 监听器的唯一标识ID
     * 在构造时自动从ID_GENERATOR获取，保证每个实例的ID唯一
     */
    private long id = ID_GENERATOR.addAndGet(1);

    /**
     * 详细输出模式标志
     * 为true时表示需要输出更详细的信息
     */
    private boolean verbose;

    /**
     * 获取监听器的唯一标识ID
     *
     * @return 监听器的唯一ID
     */
    @Override
    public long id() {
        return id;
    }

    /**
     * 监听器创建时的回调方法
     * <p>
     * 默认实现为空操作（no-op），子类可以根据需要重写此方法进行初始化操作。
     * </p>
     */
    @Override
    public void create() {
        // 默认空操作，子类可以重写
    }

    /**
     * 监听器销毁时的回调方法
     * <p>
     * 默认实现为空操作（no-op），子类可以根据需要重写此方法进行清理操作。
     * </p>
     */
    @Override
    public void destroy() {
        // 默认空操作，子类可以重写
    }

    /**
     * 获取关联的命令进程
     *
     * @return 关联的Process对象，可能为null
     */
    public Process getProcess() {
        return process;
    }

    /**
     * 设置关联的命令进程
     *
     * @param process 要关联的Process对象
     */
    public void setProcess(Process process) {
        this.process = process;
    }

    /**
     * 前置通知（Before Advice）- 来自AdviceListener接口的实现
     * <p>
     * 该方法为final，不可被子类重写。
     * 它的作用是将调用转换为包含ClassLoader和ArthasMethod参数的重载方法。
     * </p>
     *
     * @param clazz      目标方法所在的Class对象
     * @param methodName 目标方法的名称
     * @param methodDesc 目标方法的描述符（JVM格式）
     * @param target     目标对象实例，静态方法时为null
     * @param args       方法调用的参数数组
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    @Override
    final public void before(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args)
            throws Throwable {
        // 从Class对象获取类加载器
        // 创建ArthasMethod对象封装方法信息
        // 调用包含ClassLoader和ArthasMethod参数的重载方法
        before(clazz.getClassLoader(), clazz, new ArthasMethod(clazz, methodName, methodDesc), target, args);
    }

    /**
     * 返回通知（After Returning Advice）- 来自AdviceListener接口的实现
     * <p>
     * 该方法为final，不可被子类重写。
     * 它的作用是将调用转换为包含ClassLoader和ArthasMethod参数的重载方法。
     * </p>
     *
     * @param clazz        目标方法所在的Class对象
     * @param methodName   目标方法的名称
     * @param methodDesc   目标方法的描述符（JVM格式）
     * @param target       目标对象实例，静态方法时为null
     * @param args         方法调用的参数数组
     * @param returnObject 方法的返回结果
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    @Override
    final public void afterReturning(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
            Object returnObject) throws Throwable {
        // 从Class对象获取类加载器
        // 创建ArthasMethod对象封装方法信息
        // 调用包含ClassLoader和ArthasMethod参数的重载方法
        afterReturning(clazz.getClassLoader(), clazz, new ArthasMethod(clazz, methodName, methodDesc), target, args,
                returnObject);
    }

    /**
     * 异常通知（After Throwing Advice）- 来自AdviceListener接口的实现
     * <p>
     * 该方法为final，不可被子类重写。
     * 它的作用是将调用转换为包含ClassLoader和ArthasMethod参数的重载方法。
     * </p>
     *
     * @param clazz      目标方法所在的Class对象
     * @param methodName 目标方法的名称
     * @param methodDesc 目标方法的描述符（JVM格式）
     * @param target     目标对象实例，静态方法时为null
     * @param args       方法调用的参数数组
     * @param throwable  目标方法抛出的异常对象
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    @Override
    final public void afterThrowing(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
            Throwable throwable) throws Throwable {
        // 从Class对象获取类加载器
        // 创建ArthasMethod对象封装方法信息
        // 调用包含ClassLoader和ArthasMethod参数的重载方法
        afterThrowing(clazz.getClassLoader(), clazz, new ArthasMethod(clazz, methodName, methodDesc), target, args,
                throwable);
    }

    /**
     * 前置通知（Before Advice）- 抽象方法，子类必须实现
     * <p>
     * 在目标方法执行之前触发。此方法提供了比AdviceListener接口更完整的参数信息，
     * 包括ClassLoader和ArthasMethod对象，便于子类实现更复杂的逻辑。
     * </p>
     *
     * @param loader 类加载器，用于加载目标类
     * @param clazz  目标方法所在的Class对象
     * @param method ArthasMethod对象，封装了方法的详细信息
     * @param target 目标对象实例，若为静态方法则为null
     * @param args   方法调用的参数数组
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    public abstract void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable;

    /**
     * 返回通知（After Returning Advice）- 抽象方法，子类必须实现
     * <p>
     * 在目标方法正常执行完毕并返回后触发。此方法提供了比AdviceListener接口更完整的参数信息。
     * </p>
     *
     * @param loader       类加载器，用于加载目标类
     * @param clazz        目标方法所在的Class对象
     * @param method       ArthasMethod对象，封装了方法的详细信息
     * @param target       目标对象实例，若为静态方法则为null
     * @param args         方法调用的参数数组
     * @param returnObject 方法的返回结果，若为void方法则为null
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    public abstract void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
            Object[] args, Object returnObject) throws Throwable;

    /**
     * 异常通知（After Throwing Advice）- 抽象方法，子类必须实现
     * <p>
     * 在目标方法执行过程中抛出异常时触发。此方法提供了比AdviceListener接口更完整的参数信息。
     * </p>
     *
     * @param loader    类加载器，用于加载目标类
     * @param clazz     目标方法所在的Class对象
     * @param method    ArthasMethod对象，封装了方法的详细信息
     * @param target    目标对象实例，若为静态方法则为null
     * @param args      方法调用的参数数组
     * @param throwable 目标方法抛出的异常对象
     * @throws Throwable 如果通知执行过程中抛出异常
     */
    public abstract void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
            Object[] args, Throwable throwable) throws Throwable;

    /**
     * 判断条件表达式是否满足
     * <p>
     * 根据给定的条件表达式判断是否需要输出结果。如果条件表达式为空，则默认满足条件。
     * 否则使用表达式引擎计算条件表达式的值。
     * </p>
     * <p>
     * 条件表达式可以访问Advice对象中的所有信息（参数、返回值、异常等）以及本次执行的耗时。
     * </p>
     *
     * @param conditionExpress 条件表达式字符串，可以为空
     * @param advice           当前的Advice对象，包含方法调用的上下文信息
     * @param cost             本次方法执行的耗时（毫秒）
     * @return 如果条件表达式为空或计算结果为true，返回true；否则返回false
     * @throws ExpressException 如果条件表达式解析或执行出错
     */
    protected boolean isConditionMet(String conditionExpress, Advice advice, double cost) throws ExpressException {
        // 如果条件表达式为空，默认认为条件满足
        // 否则使用表达式引擎计算条件表达式的值
        return StringUtils.isEmpty(conditionExpress)
                || ExpressFactory.threadLocalExpress(advice).bind(Constants.COST_VARIABLE, cost).is(conditionExpress);
    }

    /**
     * 获取表达式的计算结果
     * <p>
     * 根据给定的表达式计算并返回结果。表达式可以访问Advice对象中的所有信息以及本次执行的耗时。
     * </p>
     *
     * @param express 要计算的表达式字符串
     * @param advice  当前的Advice对象，包含方法调用的上下文信息
     * @param cost    本次方法执行的耗时（毫秒）
     * @return 表达式的计算结果
     * @throws ExpressException 如果表达式解析或执行出错
     */
    protected Object getExpressionResult(String express, Advice advice, double cost) throws ExpressException {
        // 使用表达式引擎计算表达式的值
        // 表达式可以访问Advice中的所有上下文信息以及cost变量
        return ExpressFactory.threadLocalExpress(advice).bind(Constants.COST_VARIABLE, cost).get(express);
    }

    /**
     * 判断是否超过执行次数上限
     * <p>
     * 当命令执行次数达到或超过设定的上限时，应该停止输出并终止命令。
     * </p>
     *
     * @param limit        命令执行次数的上限值
     * @param currentTimes 当前已经执行的次数
     * @return 如果当前执行次数大于或等于上限，返回true；否则返回false
     */
    protected boolean isLimitExceeded(int limit, int currentTimes) {
        return currentTimes >= limit;
    }

    /**
     * 超过次数上限时终止命令进程
     * <p>
     * 当命令执行次数超过上限时，向用户输出提示信息并终止命令进程。
     * 提示用户可以通过 -n 选项设置执行次数上限。
     * </p>
     *
     * @param process 需要终止的命令进程
     * @param limit   执行次数的上限值
     */
    protected void abortProcess(CommandProcess process, int limit) {
        // 向用户输出提示信息，说明命令因超过执行次数上限而终止
        process.write("Command execution times exceed limit: " + limit
                + ", so command will exit. You can set it with -n option.\n");
        // 结束命令进程
        process.end();
    }

    /**
     * 判断是否为详细输出模式
     *
     * @return 如果是详细输出模式返回true，否则返回false
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * 设置详细输出模式
     *
     * @param verbose 是否启用详细输出模式
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}
