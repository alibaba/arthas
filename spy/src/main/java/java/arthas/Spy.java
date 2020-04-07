package java.arthas;

import java.lang.reflect.Method;

/**
 * 间谍类<br/>
 * 藏匿在各个ClassLoader中
 * Created by vlinux on 15/8/23.
 */
public class Spy {
    public static final String ON_BEFORE = "methodOnBegin";
    public static final String ON_RETURN = "methodOnReturnEnd";
    public static final String ON_THROWS = "methodOnThrowingEnd";
    public static final String BEFORE_INVOKE = "methodOnInvokeBeforeTracing";
    public static final String AFTER_INVOKE = "methodOnInvokeAfterTracing";
    public static final String THROW_INVOKE = "methodOnInvokeThrowTracing";

    // -- 各种Advice的钩子引用 --
    public static volatile Method ON_BEFORE_METHOD;
    public static volatile Method ON_RETURN_METHOD;
    public static volatile Method ON_THROWS_METHOD;
    public static volatile Method BEFORE_INVOKING_METHOD;
    public static volatile Method AFTER_INVOKING_METHOD;
    public static volatile Method THROW_INVOKING_METHOD;

    /**
     * arthas's classloader 引用
     */
    public static volatile ClassLoader CLASSLOADER;

    /**
     * 代理重设方法
     */
    public static volatile Method AGENT_RESET_METHOD;

    /**
     * 用于普通的间谍初始化
     */
    public static void init(
            ClassLoader classLoader,
            Method onBeforeMethod,
            Method onReturnMethod,
            Method onThrowsMethod,
            Method beforeInvokingMethod,
            Method afterInvokingMethod,
            Method throwInvokingMethod) {
        CLASSLOADER = classLoader;
        ON_BEFORE_METHOD = onBeforeMethod;
        ON_RETURN_METHOD = onReturnMethod;
        ON_THROWS_METHOD = onThrowsMethod;
        BEFORE_INVOKING_METHOD = beforeInvokingMethod;
        AFTER_INVOKING_METHOD = afterInvokingMethod;
        THROW_INVOKING_METHOD = throwInvokingMethod;
    }

    /**
     * Clean up the reference to com.taobao.arthas.agent.AgentLauncher$1
     * to avoid classloader leak.
     */
    public static void destroy() {
        initEmptySpy();
        // clear the reference to ArthasClassLoader in AgentLauncher
        if (AGENT_RESET_METHOD != null) {
            try {
                AGENT_RESET_METHOD.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        AGENT_RESET_METHOD = null;
    }

    private static void initEmptySpy() {
        try {
            Class<?> adviceWeaverClass = Spy.class;
            Method onBefore = adviceWeaverClass.getMethod(Spy.ON_BEFORE, int.class, ClassLoader.class, String.class,
                    String.class, String.class, Object.class, Object[].class);
            Method onReturn = adviceWeaverClass.getMethod(Spy.ON_RETURN, Object.class);
            Method onThrows = adviceWeaverClass.getMethod(Spy.ON_THROWS, Throwable.class);
            Method beforeInvoke = adviceWeaverClass.getMethod(Spy.BEFORE_INVOKE, int.class, String.class, String.class,
                    String.class, int.class);
            Method afterInvoke = adviceWeaverClass.getMethod(Spy.AFTER_INVOKE, int.class, String.class, String.class,
                    String.class, int.class);
            Method throwInvoke = adviceWeaverClass.getMethod(Spy.THROW_INVOKE, int.class, String.class, String.class,
                    String.class, int.class);
            Spy.init(null, onBefore, onReturn, onThrows, beforeInvoke, afterInvoke, throwInvoke);
        } catch (Exception e) {
        }
    }

    /**
     * empty method
     * 
     * @see com.taobao.arthas.core.advisor.AdviceWeaver#methodOnBegin(int,
     *      ClassLoader, String, String, String, Object, Object[])
     * @param adviceId
     * @param loader
     * @param className
     * @param methodName
     * @param methodDesc
     * @param target
     * @param args
     */
    public static void methodOnBegin(int adviceId, ClassLoader loader, String className, String methodName,
            String methodDesc, Object target, Object[] args) {
    }

    /**
     * empty method
     * 
     * @see com.taobao.arthas.core.advisor.AdviceWeaver#methodOnReturnEnd(Object)
     * @param returnObject
     */
    public static void methodOnReturnEnd(Object returnObject) {
    }

    public static void methodOnThrowingEnd(Throwable throwable) {
    }

    public static void methodOnInvokeBeforeTracing(int adviceId, String owner, String name, String desc,
            int lineNumber) {
    }

    public static void methodOnInvokeAfterTracing(int adviceId, String owner, String name, String desc,
            int lineNumber) {
    }

    public static void methodOnInvokeThrowTracing(int adviceId, String owner, String name, String desc,
            int lineNumber) {
    }
}
