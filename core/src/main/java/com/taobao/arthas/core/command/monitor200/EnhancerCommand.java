package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.CLI;
import com.taobao.middleware.cli.annotations.CLIConfigurator;
import com.taobao.middleware.logger.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author beiwei30 on 29/11/2016.
 */
public abstract class EnhancerCommand extends AnnotatedCommand {

    private static final Logger logger = LogUtil.getArthasLogger();
    private static final int SIZE_LIMIT = 50;
    private static final int MINIMAL_COMPLETE_SIZE = 3;
    protected static final List<String> EMPTY = Collections.emptyList();
    private static final String[] EXPRESS_EXAMPLES = { "params", "returnObj", "throwExp", "target", "clazz", "method",
                                                       "{params,returnObj}", "params[0]" };

    protected Matcher classNameMatcher;
    protected Matcher methodNameMatcher;

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

    @Override
    public void process(final CommandProcess process) {
        // ctrl-C support
        process.interruptHandler(new CommandInterruptHandler(process));

        // start to enhance
        enhance(process);
    }

    @Override
    public void complete(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        CliToken lastToken = tokens.get(tokens.size() - 1);

        CompleteContext completeContext = getCompleteContext(completion);
        if (completeContext == null) {
            completeDefault(completion, lastToken);
            return;
        }

        switch (completeContext.getState()) {
            case INIT:
                if (completeClassName(completion)) {
                    completeContext.setState(CompleteContext.CompleteState.CLASS_COMPLETED);
                }
                break;
            case CLASS_COMPLETED:
                if (completeMethodName(completion)) {
                    completeContext.setState(CompleteContext.CompleteState.METHOD_COMPLETED);
                }
                break;
            case METHOD_COMPLETED:
                if (completeExpress(completion)) {
                    completeContext.setState(CompleteContext.CompleteState.EXPRESS_COMPLETED);
                }
                break;
            case EXPRESS_COMPLETED:
                if (completeConditionExpress(completion)) {
                    completeContext.setState(CompleteContext.CompleteState.CONDITION_EXPRESS_COMPLETED);
                }
                break;
            case CONDITION_EXPRESS_COMPLETED:
                completion.complete(EMPTY);
        }
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
            Instrumentation inst = session.getInstrumentation();
            AdviceListener listener = getAdviceListener(process);
            if (listener == null) {
                warn(process, "advice listener is null");
                return;
            }
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }

            EnhancerAffect effect = Enhancer.enhance(inst, lock, listener instanceof InvokeTraceable,
                    skipJDKTrace, getClassNameMatcher(), getMethodNameMatcher());

            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // no class effected
                // might be method code too large
                process.write("No class or method is affected, try:\n"
                              + "1. sm CLASS_NAME METHOD_NAME to make sure the method you are tracing actually exists (it might be in your parent class).\n"
                              + "2. reset CLASS_NAME and try again, your method body might be too large.\n"
                              + "3. visit https://github.com/alibaba/arthas/issues/47 for more details.\n");
                process.end();
                return;
            }

            // 这里做个补偿,如果在enhance期间,unLock被调用了,则补偿性放弃
            if (session.getLock() == lock) {
                // 注册通知监听器
                process.register(lock, listener);
                if (process.isForeground()) {
                    process.echoTips(Constants.ABORT_MSG + "\n");
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

    /**
     * @return true if the class name is successfully completed
     */
    protected boolean completeClassName(Completion completion) {
        CliToken lastToken = completion.lineTokens().get(completion.lineTokens().size() - 1);
        if (lastToken.value().length() >= MINIMAL_COMPLETE_SIZE) {
            // complete class name
            Set<Class<?>> results = SearchUtils.searchClassOnly(completion.session().getInstrumentation(),
                                                                "*" + lastToken.value() + "*", SIZE_LIMIT);
            if (results.size() >= SIZE_LIMIT) {
                Iterator<Class<?>> it = results.iterator();
                List<String> res = new ArrayList<String>(SIZE_LIMIT);
                while (it.hasNext()) {
                    res.add(it.next().getName());
                }
                res.add("and possibly more...");
                completion.complete(res);
            } else if (results.size() == 1) {
                Class<?> clazz = results.iterator().next();
                completion.complete(clazz.getName().substring(lastToken.value().length()), true);
                return true;
            } else {
                List<String> res = new ArrayList<String>(results.size());
                for (Class clazz : results) {
                    res.add(clazz.getName());
                }
                completion.complete(res);
            }
        } else {
            // forget to call completion.complete will cause terminal to stuck.
            completion.complete(Collections.singletonList("Too many classes to display, "
                                                          + "please try to input at least 3 characters to get auto complete working."));
        }
        return false;
    }

    protected boolean completeMethodName(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        CliToken lastToken = completion.lineTokens().get(tokens.size() - 1);

        // retrieve the class name
        String className;
        if (" ".equals(lastToken.value())) {
            // tokens = { " ", "CLASS_NAME", " "}
            className = tokens.get(tokens.size() - 2).value();
        } else {
            // tokens = { " ", "CLASS_NAME", " ", "PARTIAL_METHOD_NAME"}
            className = tokens.get(tokens.size() - 3).value();
        }

        Set<Class<?>> results = SearchUtils.searchClassOnly(completion.session().getInstrumentation(), className, 2);
        if (results.isEmpty() || results.size() > 1) {
            // no class found or multiple class found
            completion.complete(EMPTY);
            return false;
        }

        Class<?> clazz = results.iterator().next();

        List<String> res = new ArrayList<String>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (" ".equals(lastToken.value())) {
                res.add(method.getName());
            } else if (method.getName().contains(lastToken.value())) {
                res.add(method.getName());
            }
        }

        if (res.size() == 1) {
            completion.complete(res.get(0).substring(lastToken.value().length()), true);
            return true;
        } else {
            completion.complete(res);
            return false;
        }
    }

    protected boolean completeExpress(Completion completion) {
        return CompletionUtils.complete(completion, Arrays.asList(EXPRESS_EXAMPLES));
    }

    protected boolean completeConditionExpress(Completion completion) {
        completion.complete(EMPTY);
        return true;
    }

    protected void completeDefault(Completion completion, CliToken lastToken) {
        CLI cli = CLIConfigurator.define(this.getClass());
        List<com.taobao.middleware.cli.Option> options = cli.getOptions();
        if (lastToken == null || lastToken.isBlank()) {
            // complete usage
            CompletionUtils.completeUsage(completion, cli);
        } else if (lastToken.value().startsWith("--")) {
            // complete long option
            CompletionUtils.completeLongOption(completion, lastToken, options);
        } else if (lastToken.value().startsWith("-")) {
            // complete short option
            CompletionUtils.completeShortOption(completion, lastToken, options);
        } else {
            completion.complete(EMPTY);
        }
    }

    private CompleteContext getCompleteContext(Completion completion) {
        CompleteContext completeContext = new CompleteContext();
        List<CliToken> tokens = completion.lineTokens();
        CliToken lastToken = tokens.get(tokens.size() - 1);

        if (lastToken.value().startsWith("-") || lastToken.value().startsWith("--")) {
            // this is the default case
            return null;
        }

        int tokenCount = 0;

        for (CliToken token : tokens) {
            if (" ".equals(token.value()) || token.value().startsWith("-") || token.value().startsWith("--")) {
                // filter irrelevant tokens
                continue;
            }
            tokenCount++;
        }

        for (CompleteContext.CompleteState state : CompleteContext.CompleteState.values()) {
            if (tokenCount == state.ordinal() || tokenCount == state.ordinal() + 1 && !" ".equals(lastToken.value())) {
                completeContext.setState(state);
                return completeContext;
            }
        }

        return completeContext;
    }

    private static void warn(CommandProcess process, String message) {
        logger.error(null, message);
        process.write("cannot operate the current command, pls. check arthas.log\n");
        if (process.isForeground()) {
            process.echoTips(Constants.ABORT_MSG + "\n");
        }
    }

}
