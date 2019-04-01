package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.ReflectAdviceListenerAdapter;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.EqualsMatcher;
import com.taobao.arthas.core.util.matcher.GroupMatcher;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ralf0131 2017-01-06 16:02.
 */
public class AbstractTraceAdviceListener extends ReflectAdviceListenerAdapter {

    protected final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();
    protected TraceCommand command;
    protected CommandProcess process;
    protected int smartTraceTime = 0;

    protected final ThreadLocal<TraceEntity> threadBoundEntity = new ThreadLocal<TraceEntity>() {

        @Override
        protected TraceEntity initialValue() {
            return new TraceEntity();
        }
    };

    /**
     * Constructor
     */
    public AbstractTraceAdviceListener(TraceCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }

    public AbstractTraceAdviceListener(TraceCommand command, CommandProcess process, int smartTraceTime) {
        this.command = command;
        this.process = process;
        this.smartTraceTime = smartTraceTime;
    }

    @Override
    public void destroy() {
        threadBoundEntity.remove();
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        threadBoundEntity.get().begin(clazz.getName(), method.getName());
        threadBoundEntity.get().deep++;
        // 开始计算本次方法调用耗时
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        threadBoundEntity.get().end();
        final Advice advice = Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject);
        finishing(advice);
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        threadBoundEntity.get().view.begin("throw:" + throwable.getClass().getName() + "()").end().end();
        final Advice advice = Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable);
        finishing(advice);
    }

    public TraceCommand getCommand() {
        return command;
    }

    private void finishing(Advice advice) {
        // 本次调用的耗时
        double cost = threadLocalWatch.costInMillis();
        if (--threadBoundEntity.get().deep == 0) {
            try {
                if (isConditionMet(command.getConditionExpress(), advice, cost)) {
                    // 满足输出条件
                    if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                        // TODO: concurrency issue to abort process
                        abortProcess(process, command.getNumberOfLimit());
                    } else {
                        process.times().incrementAndGet();
                        // TODO: concurrency issues for process.write
                        process.write(threadBoundEntity.get().view.draw() + "\n");
                        // smart trace if need
                        if (!isLimitExceeded(command.getSmartTrace(), smartTraceTime)) {
                            smartTrace();
                        }
                    }
                }
            } catch (Throwable e) {
                LogUtil.getArthasLogger().warn("trace failed.", e);
                try {
                    process.write("trace failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                                  + ", visit " + LogUtil.LOGGER_FILE + " for more details.\n");
                } catch (IllegalStateException ignore){

                } finally {
                    process.end();
                }
            } finally {
                threadBoundEntity.remove();
            }
        }
    }

    private void smartTrace() throws Throwable {
        Session session = process.session();
        Instrumentation inst = session.getInstrumentation();
        if (!session.tryLock()) {
            return;
        }
        int lock = session.getLock();
        try {
            TraceEntity.Node maxCostNode = threadBoundEntity.get().findMaxCostLeaf();
            boolean skipJDKTrace = this.getCommand().isSkipJDKTrace();

            String maxCostClass = maxCostNode.className;
            String maxCostMethod = maxCostNode.methodName;
            boolean classMatching = command.classNameMatcher.matching(maxCostClass);
            boolean methodMatching = command.methodNameMatcher.matching(maxCostMethod);
            if (classMatching && methodMatching) {
                //already enhanced
                return;
            }

            List<Matcher<String>> classMatcherList = new ArrayList<Matcher<String>>();
            classMatcherList.add(command.getClassNameMatcher());
            classMatcherList.add(new EqualsMatcher<String>(maxCostClass));
            GroupMatcher.Or<String> classMatcher = new GroupMatcher.Or<String>(classMatcherList);

            List<Matcher<String>> methodMatcherList = new ArrayList<Matcher<String>>();
            methodMatcherList.add(command.getMethodNameMatcher());
            methodMatcherList.add(new EqualsMatcher<String>(maxCostMethod));
            GroupMatcher.Or<String> methodMatcher = new GroupMatcher.Or<String>(methodMatcherList);

            EnhancerAffect effect = Enhancer.enhance(inst, lock, true, skipJDKTrace, classMatcher, methodMatcher);

            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                return;
            }
            smartTraceTime++;
            command.classNameMatcher = classMatcher;
            command.methodNameMatcher = methodMatcher;
            //Re registration
            TraceAdviceListener listener = new TraceAdviceListener(command, process, smartTraceTime);
            process.unregister();
            process.register(lock, listener);
        } catch (Throwable e) {
            LogUtil.getArthasLogger().warn("smart trace failed.", e);
            throw e;
        } finally {
            if (session.getLock() == lock) {
                process.session().unLock();
            }
        }
    }
}
