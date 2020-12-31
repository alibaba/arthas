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
 * 
 * @author hengyunabc 2020-05-20
 *
 */
public abstract class AdviceListenerAdapter implements AdviceListener, ProcessAware {
    private static final  AtomicLong ID_GENERATOR = new AtomicLong(0);
    private Process process;
    private long id = ID_GENERATOR.addAndGet(1);

    private boolean verbose;

    @Override
    public long id() {
        return id;
    }

    @Override
    public void create() {
        // default no-op
    }

    @Override
    public void destroy() {
        // default no-op
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    @Override
    final public void before(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args)
            throws Throwable {
        before(clazz.getClassLoader(), clazz, new ArthasMethod(clazz, methodName, methodDesc), target, args);
    }

    @Override
    final public void afterReturning(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
            Object returnObject) throws Throwable {
        afterReturning(clazz.getClassLoader(), clazz, new ArthasMethod(clazz, methodName, methodDesc), target, args,
                returnObject);
    }

    @Override
    final public void afterThrowing(Class<?> clazz, String methodName, String methodDesc, Object target, Object[] args,
            Throwable throwable) throws Throwable {
        afterThrowing(clazz.getClassLoader(), clazz, new ArthasMethod(clazz, methodName, methodDesc), target, args,
                throwable);
    }

    /**
     * 前置通知
     *
     * @param loader 类加载器
     * @param clazz  类
     * @param method 方法
     * @param target 目标类实例 若目标为静态方法,则为null
     * @param args   参数列表
     * @throws Throwable 通知过程出错
     */
    public abstract void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable;

    /**
     * 返回通知
     *
     * @param loader       类加载器
     * @param clazz        类
     * @param method       方法
     * @param target       目标类实例 若目标为静态方法,则为null
     * @param args         参数列表
     * @param returnObject 返回结果 若为无返回值方法(void),则为null
     * @throws Throwable 通知过程出错
     */
    public abstract void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
            Object[] args, Object returnObject) throws Throwable;

    /**
     * 异常通知
     *
     * @param loader    类加载器
     * @param clazz     类
     * @param method    方法
     * @param target    目标类实例 若目标为静态方法,则为null
     * @param args      参数列表
     * @param throwable 目标异常
     * @throws Throwable 通知过程出错
     */
    public abstract void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target,
            Object[] args, Throwable throwable) throws Throwable;

    /**
     * 判断条件是否满足，满足的情况下需要输出结果
     * 
     * @param conditionExpress 条件表达式
     * @param advice           当前的advice对象
     * @param cost             本次执行的耗时
     * @return true 如果条件表达式满足
     */
    protected boolean isConditionMet(String conditionExpress, Advice advice, double cost) throws ExpressException {
        return StringUtils.isEmpty(conditionExpress)
                || ExpressFactory.threadLocalExpress(advice).bind(Constants.COST_VARIABLE, cost).is(conditionExpress);
    }

    protected Object getExpressionResult(String express, Advice advice, double cost) throws ExpressException {
        return ExpressFactory.threadLocalExpress(advice).bind(Constants.COST_VARIABLE, cost).get(express);
    }

    /**
     * 是否超过了上限，超过之后，停止输出
     * 
     * @param limit        命令执行上限
     * @param currentTimes 当前执行次数
     * @return true 如果超过或者达到了上限
     */
    protected boolean isLimitExceeded(int limit, int currentTimes) {
        return currentTimes >= limit;
    }

    /**
     * 超过次数上限，则不再输出，命令终止
     * 
     * @param process the process to be aborted
     * @param limit   the limit to be printed
     */
    protected void abortProcess(CommandProcess process, int limit) {
        process.write("Command execution times exceed limit: " + limit
                + ", so command will exit. You can set it with -n option.\n");
        process.end();
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}
