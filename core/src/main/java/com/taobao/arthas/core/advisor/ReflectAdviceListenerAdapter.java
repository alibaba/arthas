package com.taobao.arthas.core.advisor;

import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.ArthasCheckUtils;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.StringUtils;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 反射通知适配器<br/>
 * 通过反射拿到对应的Class/Method类，而不是原始的ClassName/MethodName
 * 当然性能开销要比普通监听器高许多
 */
public abstract class ReflectAdviceListenerAdapter implements AdviceListener {

    @Override
    public void create() {
        // default no-op
    }

    @Override
    public void destroy() {
        // default no-op
    }

    private ClassLoader toClassLoader(ClassLoader loader) {
        return null != loader
                ? loader
                : AdviceListener.class.getClassLoader();
    }

    private Class<?> toClass(ClassLoader loader, String className) throws ClassNotFoundException {
        return Class.forName(StringUtils.normalizeClassName(className), true, toClassLoader(loader));
    }

    private ArthasMethod toMethod(ClassLoader loader, Class<?> clazz, String methodName, String methodDesc)
            throws ClassNotFoundException, NoSuchMethodException {
        final org.objectweb.asm.Type asmType = org.objectweb.asm.Type.getMethodType(methodDesc);

        // to arg types
        final Class<?>[] argsClasses = new Class<?>[asmType.getArgumentTypes().length];
		for (int index = 0; index < argsClasses.length; index++) {
			// asm class descriptor to jvm class
			final Class<?> argumentClass;
			final Type argumentAsmType = asmType.getArgumentTypes()[index];
			switch (argumentAsmType.getSort()) {
			case Type.BOOLEAN: {
				argumentClass = boolean.class;
				break;
			}
			case Type.CHAR: {
				argumentClass = char.class;
				break;
			}
			case Type.BYTE: {
				argumentClass = byte.class;
				break;
			}
			case Type.SHORT: {
				argumentClass = short.class;
				break;
			}
			case Type.INT: {
				argumentClass = int.class;
				break;
			}
			case Type.FLOAT: {
				argumentClass = float.class;
				break;
			}
			case Type.LONG: {
				argumentClass = long.class;
				break;
			}
			case Type.DOUBLE: {
				argumentClass = double.class;
				break;
			}
			case Type.ARRAY: {
				argumentClass = toClass(loader, argumentAsmType.getInternalName());
				break;
			}
			case Type.VOID: {
				argumentClass = void.class;
				break;
			}
			case Type.OBJECT:
			case Type.METHOD:
			default: {
				argumentClass = toClass(loader, argumentAsmType.getClassName());
				break;
			}
			}

			argsClasses[index] = argumentClass;
		}

        // to method or constructor
        if (ArthasCheckUtils.isEquals(methodName, "<init>")) {
            return ArthasMethod.newInit(toConstructor(clazz, argsClasses));
        } else {
            return ArthasMethod.newMethod(toMethod(clazz, methodName, argsClasses));
        }
    }

    private Method toMethod(Class<?> clazz, String methodName, Class<?>[] argClasses) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodName, argClasses);
    }

    private Constructor<?> toConstructor(Class<?> clazz, Class<?>[] argClasses) throws NoSuchMethodException {
        return clazz.getDeclaredConstructor(argClasses);
    }


    @Override
    final public void before(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        before(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args);
    }

    @Override
    final public void afterReturning(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args, Object returnObject) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        afterReturning(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args, returnObject);
    }

    @Override
    final public void afterThrowing(
            ClassLoader loader, String className, String methodName, String methodDesc,
            Object target, Object[] args, Throwable throwable) throws Throwable {
        final Class<?> clazz = toClass(loader, className);
        afterThrowing(loader, clazz, toMethod(loader, clazz, methodName, methodDesc), target, args, throwable);
    }


    /**
     * 前置通知
     *
     * @param loader 类加载器
     * @param clazz  类
     * @param method 方法
     * @param target 目标类实例
     *               若目标为静态方法,则为null
     * @param args   参数列表
     * @throws Throwable 通知过程出错
     */
    public abstract void before(
            ClassLoader loader, Class<?> clazz, ArthasMethod method,
            Object target, Object[] args) throws Throwable;

    /**
     * 返回通知
     *
     * @param loader       类加载器
     * @param clazz        类
     * @param method       方法
     * @param target       目标类实例
     *                     若目标为静态方法,则为null
     * @param args         参数列表
     * @param returnObject 返回结果
     *                     若为无返回值方法(void),则为null
     * @throws Throwable 通知过程出错
     */
    public abstract void afterReturning(
            ClassLoader loader, Class<?> clazz, ArthasMethod method,
            Object target, Object[] args,
            Object returnObject) throws Throwable;

    /**
     * 异常通知
     *
     * @param loader    类加载器
     * @param clazz     类
     * @param method    方法
     * @param target    目标类实例
     *                  若目标为静态方法,则为null
     * @param args      参数列表
     * @param throwable 目标异常
     * @throws Throwable 通知过程出错
     */
    public abstract void afterThrowing(
            ClassLoader loader, Class<?> clazz, ArthasMethod method,
            Object target, Object[] args,
            Throwable throwable) throws Throwable;


    /**
     * 判断条件是否满足，满足的情况下需要输出结果
     * @param conditionExpress 条件表达式
     * @param advice 当前的advice对象
     * @param cost 本次执行的耗时
     * @return true 如果条件表达式满足
     */
    protected boolean isConditionMet(String conditionExpress, Advice advice, double cost) throws ExpressException {
        return StringUtils.isEmpty(conditionExpress) ||
                ExpressFactory.threadLocalExpress(advice).bind(Constants.COST_VARIABLE, cost).is(conditionExpress);
    }

    protected Object getExpressionResult(String express, Advice advice, double cost) throws ExpressException {
        return ExpressFactory.threadLocalExpress(advice)
                .bind(Constants.COST_VARIABLE, cost).get(express);
    }

    /**
     * 是否超过了上限，超过之后，停止输出
     * @param limit 命令执行上限
     * @param currentTimes 当前执行次数
     * @return true 如果超过或者达到了上限
     */
    protected boolean isLimitExceeded(int limit, int currentTimes) {
        return currentTimes >= limit;
    }

    /**
     * 超过次数上限，则不再输出，命令终止
     * @param process the process to be aborted
     * @param limit the limit to be printed
     */
    protected void abortProcess(CommandProcess process, int limit) {
        process.write("Command execution times exceed limit: " + limit + ", so command will exit. You can set it with -n option.\n");
        process.end();
    }

}
