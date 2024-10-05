package java.arthas;

/**
 * <pre>
 * 一个adviceId 是什么呢？ 就是一个trace/monitor/watch命令能对应上的一个id，比如一个类某个函数，它的 enter/end/exception 统一是一个id，分配完了就不会再分配。
 * 
 * 同样一个method，如果它trace之后，也会有一个 adviceId， 这个method里的所有invoke都是统一处理，认为是一个 adviceId 。 但如果有匹配到不同的 invoke的怎么分配？？
 * 好像有点难了。。
 * 
 * 其实就是把所有可以插入的地方都分类好，那么怎么分类呢？？ 或者是叫同一种匹配，就是同一种的 adviceId? 
 * 
 * 比如入参是有  class , method ,是固定的  ,  某个行号，或者 某个
 * 
 * aop插入的叫 adviceId ， command插入的叫 ListenerId？
 * 
 * 
 * 
 * </pre>
 * 
 * @author hengyunabc
 *
 */
public class SpyAPI {
    public static final AbstractSpy NOPSPY = new NopSpy();
    private static volatile AbstractSpy spyInstance = NOPSPY;

    public static volatile boolean INITED;

    public static AbstractSpy getSpy() {
        return spyInstance;
    }

    public static void setSpy(AbstractSpy spy) {
        spyInstance = spy;
    }

    public static void setNopSpy() {
        setSpy(NOPSPY);
    }

    public static boolean isNopSpy() {
        return NOPSPY == spyInstance;
    }

    public static void init() {
        INITED = true;
    }

    public static boolean isInited() {
        return INITED;
    }

    public static void destroy() {
        setNopSpy();
        INITED = false;
    }

    public static void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        spyInstance.atEnter(clazz, methodInfo, target, args);
    }

    public static void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
            Object returnObject) {
        spyInstance.atExit(clazz, methodInfo, target, args, returnObject);
    }

    public static void atExceptionExit(Class<?> clazz, String methodInfo, Object target,
            Object[] args, Throwable throwable) {
        spyInstance.atExceptionExit(clazz, methodInfo, target, args, throwable);
    }

    public static void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {
        spyInstance.atBeforeInvoke(clazz, invokeInfo, target);
    }

    public static void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {
        spyInstance.atAfterInvoke(clazz, invokeInfo, target);
    }

    public static void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {
        spyInstance.atInvokeException(clazz, invokeInfo, target, throwable);
    }

    /**
     * 使用 LineCode 进行观测的入口
     * 至于为何需要分成 atLineCode 和 atLineNumber ，参见 {@link com.taobao.arthas.core.advisor.SpyInterceptors.SpyLineInterceptor} 的类注释
     * @param lineCode 一个生成的特殊的行标识，可以理解为一种特殊的自己生成的行号
     */
    public static void atLineCode(Class<?> clazz, String methodInfo, Object target, Object[] args,
                                          String lineCode, Object[] vars, String[] varNames) {
        try{
            spyInstance.atLine(clazz, methodInfo, target, args, lineCode, vars, varNames);
        }catch (Throwable t){
            //ignore 通常情况下不会抛出到外层,但是会有一些新旧版本混用可能会导致报错（先启动了旧版本，再启动新版本）,这里做一下保护
        }
    }

    /**
     * 使用 LineNumber 进行观测的入口
     * 至于为何需要分成 atLineCode 和 atLineNumber ，参见 {@link com.taobao.arthas.core.advisor.SpyInterceptors.SpyLineInterceptor} 的类注释
     * @param lineNumber 行号
     */
    public static void atLineNumber(Class<?> clazz, String methodInfo, Object target, Object[] args,
                                          String lineNumber, Object[] vars, String[] varNames) {
        try{
            spyInstance.atLine(clazz, methodInfo, target, args, lineNumber, vars, varNames);
        }catch (Throwable t){
            //ignore 通常情况下不会抛出到外层,但是会有一些新旧版本混用可能会导致报错（先启动了旧版本，再启动新版本）,这里做一下保护
        }
    }

    public static abstract class AbstractSpy {
        public abstract void atEnter(Class<?> clazz, String methodInfo, Object target,
                Object[] args);

        public abstract void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Object returnObject);

        public abstract void atExceptionExit(Class<?> clazz, String methodInfo, Object target,
                Object[] args, Throwable throwable);

        public abstract void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target);

        public abstract void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target);

        public abstract void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable);

        /**
         * 在某行进行观测
         * @param line 行标识，可能是行号(LineNumber)，也可能是行的特殊标号(LineCode)
         */
        public abstract void atLine(Class<?> clazz, String methodInfo, Object target, Object[] args,
                                    String line, Object[] vars, String[] varNames);
    }

    static class NopSpy extends AbstractSpy {

        @Override
        public void atEnter(Class<?> clazz, String methodInfo, Object target, Object[] args) {
        }

        @Override
        public void atExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Object returnObject) {
        }

        @Override
        public void atExceptionExit(Class<?> clazz, String methodInfo, Object target, Object[] args,
                Throwable throwable) {
        }

        @Override
        public void atBeforeInvoke(Class<?> clazz, String invokeInfo, Object target) {

        }

        @Override
        public void atAfterInvoke(Class<?> clazz, String invokeInfo, Object target) {

        }

        @Override
        public void atInvokeException(Class<?> clazz, String invokeInfo, Object target, Throwable throwable) {

        }

        @Override
        public void atLine(Class<?> clazz, String methodInfo, Object target, Object[] args, String line, Object[] vars, String[] varNames) {

        }

    }
}
