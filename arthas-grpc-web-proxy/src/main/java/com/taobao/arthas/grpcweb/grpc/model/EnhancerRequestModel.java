package com.taobao.arthas.grpcweb.grpc.model;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.command.monitor200.AbstractTraceAdviceListener;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.Ansi;
import com.taobao.arthas.grpcweb.grpc.observer.ArthasStreamObserver;
import com.taobao.arthas.grpcweb.grpc.service.advisor.Enhancer;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;

public abstract class EnhancerRequestModel {

    private static final Logger logger = LoggerFactory.getLogger(EnhancerRequestModel.class);
    protected static final List<String> EMPTY = Collections.emptyList();
    public static final String[] EXPRESS_EXAMPLES = { "params", "returnObj", "throwExp", "target", "clazz", "method",
            "{params,returnObj}", "params[0]" };
    protected String excludeClassPattern;

    protected Matcher classNameMatcher;
    protected Matcher classNameExcludeMatcher;
    protected Matcher methodNameMatcher;

    protected long jobId;
    protected long listenerId;

    protected boolean verbose;

    protected int maxNumOfMatchedClass;

    /**
     * 类名匹配
     *
     * @return 获取类名匹配
     */
    protected abstract Matcher getClassNameMatcher();

    /**
     * 排除类名匹配
     */
    protected abstract Matcher getClassNameExcludeMatcher();

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
    protected abstract AdviceListener getAdviceListener(ArthasStreamObserver arthasStreamObserver);


    public void enhance(ArthasStreamObserver arthasStreamObserver) {
        EnhancerAffect effect = null;
        try {
            Instrumentation inst = arthasStreamObserver.getInstrumentation();
            AdviceListener listener = getAdviceListener(arthasStreamObserver);
            if (listener == null) {
                logger.error("advice listener is null");
                String msg = "advice listener is null, check arthas log";
//                arthasStreamObserver.appendResult(new EnhancerModel(effect, false, msg));
                arthasStreamObserver.end(-1, msg);
                return;
            }
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }

            Enhancer enhancer = new Enhancer(listener, listener instanceof InvokeTraceable, skipJDKTrace, getClassNameMatcher(), getClassNameExcludeMatcher(), getMethodNameMatcher());
            // 注册通知监听器
            arthasStreamObserver.register(listener, enhancer);
            effect = enhancer.enhance(inst, this.maxNumOfMatchedClass);
            if (effect.getThrowable() != null) {
                String msg = "error happens when enhancing class: "+effect.getThrowable().getMessage();
//                arthasStreamObserver.appendResult(new EnhancerModel(effect, false, msg));
                arthasStreamObserver.end(-1, msg + ", check arthas log: " + LogUtil.loggingFile());
                return;
            }

            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // no class effected
                if (!StringUtils.isEmpty(effect.getOverLimitMsg())) {
                    String msg = "no class effected";
//                    arthasStreamObserver.appendResult(new EnhancerModel(effect, false));
                    arthasStreamObserver.end(-1, msg);
                    return;
                }
                // might be method code too large
//                arthasStreamObserver.appendResult(new EnhancerModel(effect, false, "No class or method is affected"));

                String smCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("sm CLASS_NAME METHOD_NAME").reset().toString();
                String optionsCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("options unsafe true").reset().toString();
                String javaPackage = Ansi.ansi().fg(Ansi.Color.GREEN).a("java.*").reset().toString();
                String resetCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("reset CLASS_NAME").reset().toString();
                String logStr = Ansi.ansi().fg(Ansi.Color.GREEN).a(LogUtil.loggingFile()).reset().toString();
                String issueStr = Ansi.ansi().fg(Ansi.Color.GREEN).a("https://github.com/alibaba/arthas/issues/47").reset().toString();
                String msg = "No class or method is affected, try:\n"
                        + "1. Execute `" + smCommand + "` to make sure the method you are tracing actually exists (it might be in your parent class).\n"
                        + "2. Execute `" + optionsCommand + "`, if you want to enhance the classes under the `" + javaPackage + "` package.\n"
                        + "3. Execute `" + resetCommand + "` and try again, your method body might be too large.\n"
                        + "4. Match the constructor, use `<init>`, for example: `watch demo.MathGame <init>`\n"
                        + "5. Check arthas log: " + logStr + "\n"
                        + "6. Visit " + issueStr + " for more details.";
                arthasStreamObserver.end(-1, msg);
                return;
            }
            arthasStreamObserver.appendResult(new EnhancerModel(effect, true));

            //异步执行，在RpcAdviceListener中结束
        } catch (Throwable e) {
            String msg = "error happens when enhancing class: "+e.getMessage();
            logger.error(msg, e);
//            arthasStreamObserver.appendResult(new EnhancerModel(effect, false, msg));
            arthasStreamObserver.end(-1, msg);
        }
    }

}
