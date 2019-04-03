package com.taobao.arthas.core.command.monitor200;

import java.lang.instrument.UnmodifiableClassException;
import java.util.Set;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.handlers.shell.YContinueHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.logger.Logger;

/**
 * @author beiwei30 on 29/11/2016.
 */
public abstract class EnhancerCommand extends AnnotatedCommand {

    private static final Logger logger = LogUtil.getArthasLogger();
    private static final int WEARING_CLASS_SIZE = 10;
    private static final int WEARING_METHOD_SIZE = 50;
    public static final String[] EXPRESS_EXAMPLES = { "params", "returnObj", "throwExp", "target", "clazz", "method",
                                                       "{params,returnObj}", "params[0]" };

    protected Matcher<String> classNameMatcher;
    protected Matcher<String> methodNameMatcher;

    /**
     * 类名匹配
     *
     * @return 获取类名匹配
     */
    protected abstract Matcher<String> getClassNameMatcher();

    /**
     * 方法名匹配
     *
     * @return 获取方法名匹配
     */
    protected abstract Matcher<String> getMethodNameMatcher();

    /**
     * 获取监听器
     *
     * @return 返回监听器
     */
    protected abstract AdviceListener getAdviceListener(CommandProcess process);

    @Override
    public void process(final CommandProcess process) {
        // ctrl-C support
        process.interruptHandler(new CommandInterruptHandler(process));
        // q exit support
        process.stdinHandler(new QExitHandler(process));

        // start to enhance
        enhance(process);
    }

    @Override
    public void complete(Completion completion) {
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);

        if (argumentIndex == 1) { // class name
            if (!CompletionUtils.completeClassName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 2) { // method name
            if (!CompletionUtils.completeMethodName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 3) { // watch express
            completeArgument3(completion);
            return;
        }

        super.complete(completion);
    }

    protected void enhance(CommandProcess process) {
        Session session = process.session();
        if (!session.tryLock()) {
            process.write("someone else is enhancing classes, pls. wait.\n");
            process.end();
            return;
        }
        int lock = session.getLock();
        try {
            AdviceListener listener = getAdviceListener(process);
            if (listener == null) {
                warn(process, "advice listener is null");
                return;
            }
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }
            Set<Class<?>> enhanceClassSet = Enhancer.findEnhanceClass(session.getInstrumentation(), getClassNameMatcher());
            if (!safeEnhance(process, enhanceClassSet)) {
                return;
            }
            EnhancerAffect effect = Enhancer.enhance(process, lock,
                                                     listener instanceof InvokeTraceable,
                                                     skipJDKTrace,
                                                     getMethodNameMatcher(),
                                                     enhanceClassSet);
            if (effect == null) {
                process.end();
                return;
            }
            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // no class effected
                // might be method code too large
                process.write("No class or method is affected, try:\n"
                              + "1. sm CLASS_NAME METHOD_NAME to make sure the method you are tracing actually exists (it might be in your parent class).\n"
                              + "2. reset CLASS_NAME and try again, your method body might be too large.\n"
                              + "3. check arthas log: " + LogUtil.LOGGER_FILE + "\n"
                              + "4. visit https://github.com/alibaba/arthas/issues/47 for more details.\n");
                process.end();
                return;
            }

            // 这里做个补偿,如果在enhance期间,unLock被调用了,则补偿性放弃
            if (session.getLock() == lock) {
                // 注册通知监听器
                process.register(lock, listener);
                if (process.isForeground()) {
                    process.echoTips(Constants.Q_OR_CTRL_C_ABORT_MSG + "\n");
                }
            }

            process.write(effect + "\n");
        } catch (UnmodifiableClassException e) {
            logger.error(null, "error happens when enhancing class", e);
        } finally {
            if (session.getLock() == lock) {
                // enhance结束后解锁
                process.session().unLock();
            }
        }
    }

    protected void completeArgument3(Completion completion) {
        super.complete(completion);
    }

    private static void warn(CommandProcess process, String message) {
        logger.error(null, message);
        process.write("cannot operate the current command, pls. check arthas.log\n");
        if (process.isForeground()) {
            process.echoTips(Constants.Q_OR_CTRL_C_ABORT_MSG + "\n");
        }
    }

    private boolean safeEnhance(CommandProcess process, Set<Class<?>> enhanceClassSet) {
        try {
            if (enhanceClassSet.size() > WEARING_CLASS_SIZE) {
                process.write("Predicted affect class-cnt:" + enhanceClassSet.size() + ", are you sure to continue?(y/n):");
                process.stdinHandler(new YContinueHandler(process));
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    return false;
                }
            }
            if (!process.isRunning()) {
                return false;
            }
            int matchMethodCnt = 0;
            for (Class<?> clazz : enhanceClassSet) {
                matchMethodCnt += SearchUtils.searchClassMethod(clazz, getMethodNameMatcher()).size();
            }
            if (matchMethodCnt > WEARING_METHOD_SIZE) {
                process.write("Predicted affect method-cnt:" + matchMethodCnt + ", are you sure to continue?(y/n):");
                process.stdinHandler(new YContinueHandler(process));
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    return false;
                }
            }
        } catch (Throwable t) {
            logger.warn("safe enhance error", t);
            process.write("safe enhance error, exception message: " + t.getMessage()
                          + ", please check $HOME/logs/arthas/arthas.log for more details. \n");
            return false;
        }
        return process.isRunning();
    }
}
