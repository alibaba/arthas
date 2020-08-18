package com.taobao.arthas.core.command.monitor200;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Collections;
import java.util.List;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Option;

/**
 * @author beiwei30 on 29/11/2016.
 */
public abstract class EnhancerCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(EnhancerCommand.class);
    protected static final List<String> EMPTY = Collections.emptyList();
    public static final String[] EXPRESS_EXAMPLES = { "params", "returnObj", "throwExp", "target", "clazz", "method",
                                                       "{params,returnObj}", "params[0]" };

    protected Matcher classNameMatcher;
    protected Matcher methodNameMatcher;

    protected long listenerId;

    protected boolean verbose;

    @Option(longName = "listenerId")
    @Description("The special listenerId")
    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    @Option(shortName = "v", longName = "verbose", flag = true)
    @Description("Enables print verbose information, default value false.")
    public void setVerbosee(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * 类名匹配
     *
     * @return 获取类名匹配
     */
    protected abstract Matcher getClassNameMatcher();

    /**
     * 方法名匹配
     *
     * @return 获取方法名匹配
     */
    protected abstract Matcher getMethodNameMatcher();

    /**
     * 获取监听器
     *
     * @return 返回监听器
     */
    protected abstract AdviceListener getAdviceListener(CommandProcess process);

    AdviceListener getAdviceListenerWithId(CommandProcess process) {
        if (listenerId != 0) {
            AdviceListener listener = AdviceWeaver.listener(listenerId);
            if (listener != null) {
                return listener;
            }
        }
        return getAdviceListener(process);
    }
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
            String msg = "someone else is enhancing classes, pls. wait.";
            process.appendResult(new EnhancerModel(null, false, msg));
            process.end(-1, msg);
            return;
        }
        EnhancerAffect effect = null;
        int lock = session.getLock();
        try {
            Instrumentation inst = session.getInstrumentation();
            AdviceListener listener = getAdviceListenerWithId(process);
            if (listener == null) {
                logger.error("advice listener is null");
                String msg = "advice listener is null, check arthas log";
                process.appendResult(new EnhancerModel(effect, false, msg));
                process.end(-1, msg);
                return;
            }
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }

            Enhancer enhancer = new Enhancer(listener, listener instanceof InvokeTraceable, skipJDKTrace, getClassNameMatcher(), getMethodNameMatcher());
            // 注册通知监听器
            process.register(listener, enhancer);
            effect = enhancer.enhance(inst);

            if (effect.getThrowable() != null) {
                String msg = "error happens when enhancing class: "+effect.getThrowable().getMessage();
                process.appendResult(new EnhancerModel(effect, false, msg));
                process.end(1, msg + ", check arthas log: " + LogUtil.loggingFile());
                return;
            }

            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // no class effected
                // might be method code too large
                process.appendResult(new EnhancerModel(effect, false, "No class or method is affected"));
                String msg = "No class or method is affected, try:\n"
                        + "1. sm CLASS_NAME METHOD_NAME to make sure the method you are tracing actually exists (it might be in your parent class).\n"
                        + "2. reset CLASS_NAME and try again, your method body might be too large.\n"
                        + "3. check arthas log: " + LogUtil.loggingFile() + "\n"
                        + "4. visit https://github.com/alibaba/arthas/issues/47 for more details.";
                process.end(-1, msg);
                return;
            }

            // 这里做个补偿,如果在enhance期间,unLock被调用了,则补偿性放弃
            if (session.getLock() == lock) {
                if (process.isForeground()) {
                    process.echoTips(Constants.Q_OR_CTRL_C_ABORT_MSG + "\n");
                }
            }

            process.appendResult(new EnhancerModel(effect, true));

            //异步执行，在AdviceListener中结束
        } catch (Throwable e) {
            String msg = "error happens when enhancing class: "+e.getMessage();
            logger.error(msg, e);
            process.appendResult(new EnhancerModel(effect, false, msg));
            process.end(-1, msg);
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
}
