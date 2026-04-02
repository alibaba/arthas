package com.taobao.arthas.core.command.monitor200;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.AdviceWeaver;
import com.taobao.arthas.core.advisor.Enhancer;
import com.taobao.arthas.core.advisor.InvokeTraceable;
import com.taobao.arthas.core.command.model.EnhancerModel;
import com.taobao.arthas.core.command.model.EnhancerModelFactory;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.EnhancerAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.Ansi;
import com.taobao.middleware.cli.annotations.DefaultValue;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Option;

/**
 * 增强命令的抽象基类
 * 提供类增强、方法增强的通用功能，包括类匹配、方法匹配、监听器管理等
 * 子类需要实现具体的匹配器和监听器获取逻辑
 *
 * @author beiwei30 on 29/11/2016.
 */
public abstract class EnhancerCommand extends AnnotatedCommand {

    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(EnhancerCommand.class);

    // 空列表常量，用于返回空结果
    protected static final List<String> EMPTY = Collections.emptyList();

    // 表达式示例数组，用于命令提示
    public static final String[] EXPRESS_EXAMPLES = { "params", "returnObj", "throwExp", "target", "clazz", "method",
                                                       "{params,returnObj}", "params[0]" };

    // 排除类名模式
    private String excludeClassPattern;

    // 类名匹配器
    protected Matcher classNameMatcher;

    // 排除类名匹配器
    protected Matcher classNameExcludeMatcher;

    // 方法名匹配器
    protected Matcher methodNameMatcher;

    // 监听器ID，用于获取已存在的监听器
    protected long listenerId;

    // 是否输出详细信息
    protected boolean verbose;

    // 最大匹配类数量限制
    protected int maxNumOfMatchedClass;

    // 超时时间（秒）
    protected Long timeout;

    // 是否启用懒加载模式
    protected boolean lazy = false;

    /**
     * 指定 classloader hash，只增强该 classloader 加载的类。
     * 用于在多ClassLoader环境下精确定位要增强的类
     */
    protected String hashCode;

    /**
     * 设置排除类名模式
     * 用于排除不需要增强的类
     *
     * @param excludeClassPattern 排除类名模式，支持使用'.'或'/'作为分隔符
     */
    @Option(longName = "exclude-class-pattern")
    @Description("exclude class name pattern, use either '.' or '/' as separator")
    public void setExcludeClassPattern(String excludeClassPattern) {
        this.excludeClassPattern = excludeClassPattern;
    }

    /**
     * 设置类加载器哈希码
     * 只增强指定类加载器加载的类，用于精确定位
     *
     * @param hashCode 类加载器的哈希码
     */
    @Option(longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        this.hashCode = hashCode;
    }

    /**
     * 获取类加载器哈希码
     *
     * @return 类加载器哈希码
     */
    public String getHashCode() {
        return hashCode;
    }

    /**
     * 设置监听器ID
     * 用于指定已存在的监听器，而不是创建新的
     *
     * @param listenerId 监听器ID
     */
    @Option(longName = "listenerId")
    @Description("The special listenerId")
    public void setListenerId(long listenerId) {
        this.listenerId = listenerId;
    }

    /**
     * 设置是否输出详细信息
     *
     * @param verbose 是否输出详细信息
     */
    @Option(shortName = "v", longName = "verbose", flag = true)
    @Description("Enables print verbose information, default value false.")
    public void setVerbosee(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * 设置最大匹配类数量
     * 防止匹配过多类导致性能问题
     *
     * @param maxNumOfMatchedClass 最大匹配类数量
     */
    @Option(shortName = "m", longName = "maxMatch")
    @DefaultValue("50")
    @Description("The maximum of matched class.")
    public void setMaxNumOfMatchedClass(int maxNumOfMatchedClass) {
        this.maxNumOfMatchedClass = maxNumOfMatchedClass;
    }

    /**
     * 设置超时时间
     * 命令执行超过指定时间后自动退出
     *
     * @param timeout 超时时间（秒）
     */
    @Option(longName = "timeout")
    @Description("Timeout value in seconds for the command to exit automatically.")
    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    /**
     * 获取超时时间
     *
     * @return 超时时间（秒）
     */
    public Long getTimeout() {
        return timeout;
    }

    /**
     * 设置是否启用懒加载模式
     * 在懒加载模式下，会在类加载时才进行增强，而不是立即增强
     * 适用于尚未加载的类
     *
     * @param lazy 是否启用懒加载模式
     */
    @Option(shortName = "L", longName = "lazy", flag = true)
    @Description("Enable lazy mode to enhance classes when they are loaded. Useful when the class is not loaded yet.")
    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * 判断是否启用懒加载模式
     *
     * @return 是否启用懒加载模式
     */
    public boolean isLazy() {
        return lazy;
    }

    /**
     * 获取类名匹配器
     * 子类需要实现此方法，提供具体的类名匹配逻辑
     *
     * @return 类名匹配器
     */
    protected abstract Matcher getClassNameMatcher();

    /**
     * 获取排除类名匹配器
     * 子类需要实现此方法，提供具体的排除类名匹配逻辑
     *
     * @return 排除类名匹配器
     */
    protected abstract Matcher getClassNameExcludeMatcher();

    /**
     * 获取方法名匹配器
     * 子类需要实现此方法，提供具体的方法名匹配逻辑
     *
     * @return 方法名匹配器
     */
    protected abstract Matcher getMethodNameMatcher();

    /**
     * 获取监听器
     * 子类需要实现此方法，创建并返回具体的监听器
     *
     * @param process 命令进程
     * @return 监听器
     */
    protected abstract AdviceListener getAdviceListener(CommandProcess process);

    /**
     * 根据监听器ID获取监听器
     * 如果指定了listenerId，则尝试获取已存在的监听器
     * 否则调用getAdviceListener创建新监听器
     *
     * @param process 命令进程
     * @return 监听器
     */
    AdviceListener getAdviceListenerWithId(CommandProcess process) {
        // 如果指定了监听器ID
        if (listenerId != 0) {
            // 尝试从AdviceWeaver中获取已存在的监听器
            AdviceListener listener = AdviceWeaver.listener(listenerId);
            if (listener != null) {
                return listener;
            }
        }
        // 如果没有指定ID或获取失败，则创建新监听器
        return getAdviceListener(process);
    }

    /**
     * 处理命令执行
     * 设置中断处理器和退出处理器，然后开始增强流程
     *
     * @param process 命令进程
     */
    @Override
    public void process(final CommandProcess process) {
        // 设置Ctrl+C中断处理器
        process.interruptHandler(new CommandInterruptHandler(process));

        // 设置Q退出处理器（输入q可以退出）
        process.stdinHandler(new QExitHandler(process));

        // 开始增强流程
        enhance(process);
    }

    /**
     * 命令自动补全
     * 根据参数位置提供不同的补全建议
     *
     * @param completion 补全上下文
     */
    @Override
    public void complete(Completion completion) {
        // 检测当前是第几个参数
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);

        if (argumentIndex == 1) { // 第一个参数：类名
            // 尝试补全类名
            if (!CompletionUtils.completeClassName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 2) { // 第二个参数：方法名
            // 尝试补全方法名
            if (!CompletionUtils.completeMethodName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 3) { // 第三个参数：表达式
            // 补全第三个参数（表达式）
            completeArgument3(completion);
            return;
        }

        // 其他情况使用默认补全逻辑
        super.complete(completion);
    }

    /**
     * 执行增强操作
     * 这是增强命令的核心方法，负责：
     * 1. 获取会话锁，保证同一时间只有一个增强操作
     * 2. 创建增强器和监听器
     * 3. 执行类增强
     * 4. 处理增强结果
     * 5. 设置超时任务
     *
     * @param process 命令进程
     */
    protected void enhance(CommandProcess process) {
        // 获取当前会话
        Session session = process.session();

        // 尝试获取会话锁，防止并发增强
        if (!session.tryLock()) {
            String msg = "someone else is enhancing classes, pls. wait.";
            process.appendResult(EnhancerModelFactory.create(null, false, msg));
            process.end(-1, msg);
            return;
        }

        EnhancerAffect effect = null;
        // 记录锁的版本号，用于后续检查锁是否被释放
        int lock = session.getLock();
        try {
            // 获取Java Instrumentation实例
            Instrumentation inst = session.getInstrumentation();

            // 获取监听器（可能是已存在的，也可能是新创建的）
            AdviceListener listener = getAdviceListenerWithId(process);
            if (listener == null) {
                logger.error("advice listener is null");
                String msg = "advice listener is null, check arthas log";
                process.appendResult(EnhancerModelFactory.create(effect, false, msg));
                process.end(-1, msg);
                return;
            }

            // 检查是否需要跳过JDK类的跟踪
            boolean skipJDKTrace = false;
            if(listener instanceof AbstractTraceAdviceListener) {
                skipJDKTrace = ((AbstractTraceAdviceListener) listener).getCommand().isSkipJDKTrace();
            }

            // 创建增强器，配置各种匹配器和选项
            Enhancer enhancer = new Enhancer(listener, listener instanceof InvokeTraceable, skipJDKTrace,
                    getClassNameMatcher(), getClassNameExcludeMatcher(), getMethodNameMatcher(), this.lazy, this.hashCode);

            // 注册通知监听器到进程
            process.register(listener, enhancer);

            // 执行增强操作
            effect = enhancer.enhance(inst, this.maxNumOfMatchedClass);

            // 检查增强过程中是否发生异常
            if (effect.getThrowable() != null) {
                String msg = "error happens when enhancing class: "+effect.getThrowable().getMessage();
                process.appendResult(EnhancerModelFactory.create(effect, false, msg));
                process.end(1, msg + ", check arthas log: " + LogUtil.loggingFile());
                return;
            }

            // 检查是否有类或方法被增强
            if (effect.cCnt() == 0 || effect.mCnt() == 0) {
                // 没有类被增强

                // 如果超过限制，输出错误信息
                if (!StringUtils.isEmpty(effect.getOverLimitMsg())) {
                    process.appendResult(EnhancerModelFactory.create(effect, false));
                    process.end(-1);
                    return;
                }

                // 懒加载模式：即使没有匹配的类也不立即结束，等待类加载
                if (this.lazy) {
                    String lazyMsg = "Lazy mode is enabled, waiting for class to be loaded. Press Q or Ctrl+C to abort.\n"
                            + "When the target class is loaded, it will be automatically enhanced.";
                    process.write(lazyMsg + "\n");
                } else {
                    // 非懒加载模式，输出详细的提示信息

                    // 可能是方法体过大
                    process.appendResult(EnhancerModelFactory.create(effect, false, "No class or method is affected"));

                    // 构建各种提示命令（使用绿色高亮）
                    String smCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("sm CLASS_NAME METHOD_NAME").reset().toString();
                    String optionsCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("options unsafe true").reset().toString();
                    String javaPackage = Ansi.ansi().fg(Ansi.Color.GREEN).a("java.*").reset().toString();
                    String resetCommand = Ansi.ansi().fg(Ansi.Color.GREEN).a("reset CLASS_NAME").reset().toString();
                    String logStr = Ansi.ansi().fg(Ansi.Color.GREEN).a(LogUtil.loggingFile()).reset().toString();
                    String issueStr = Ansi.ansi().fg(Ansi.Color.GREEN).a("https://github.com/alibaba/arthas/issues/47").reset().toString();

                    // 输出详细的排查建议
                    String msg = "No class or method is affected, try:\n"
                            + "1. Execute `" + smCommand + "` to make sure the method you are tracing actually exists (it might be in your parent class).\n"
                            + "2. Execute `" + optionsCommand + "`, if you want to enhance the classes under the `" + javaPackage + "` package.\n"
                            + "3. Execute `" + resetCommand + "` and try again, your method body might be too large.\n"
                            + "4. Match the constructor, use `<init>`, for example: `watch demo.MathGame <init>`\n"
                            + "5. Check arthas log: " + logStr + "\n"
                            + "6. Visit " + issueStr + " for more details.\n"
                            + "7. If the class is not loaded yet, try to use `--lazy` or `-L` option to enable lazy mode.";
                    process.end(-1, msg);
                    return;
                }
            }

            // 这里做个补偿,如果在enhance期间,unLock被调用了,则补偿性放弃
            // 检查锁的版本号是否变化，如果变化说明锁已被释放
            if (session.getLock() == lock) {
                // 如果是前台进程，输出提示信息
                if (process.isForeground()) {
                    process.echoTips(Constants.Q_OR_CTRL_C_ABORT_MSG + "\n");
                }
            }

            // 输出增强结果
            process.appendResult(EnhancerModelFactory.create(effect, true));

            // 设置超时任务（如果配置了超时）
            scheduleTimeoutTask(process);

            // 异步执行，在AdviceListener中结束命令
        } catch (Throwable e) {
            // 捕获所有异常，输出错误信息
            String msg = "error happens when enhancing class: "+e.getMessage();
            logger.error(msg, e);
            process.appendResult(EnhancerModelFactory.create(effect, false, msg));
            process.end(-1, msg);
        } finally {
            // 检查锁的版本号，如果未变化则释放锁
            if (session.getLock() == lock) {
                // enhance结束后解锁
                process.session().unLock();
            }
        }
    }

    /**
     * 补全第三个参数（表达式）
     * 子类可以重写此方法提供自定义的补全逻辑
     *
     * @param completion 补全上下文
     */
    protected void completeArgument3(Completion completion) {
        super.complete(completion);
    }

    /**
     * 获取排除类名模式
     *
     * @return 排除类名模式
     */
    public String getExcludeClassPattern() {
        return excludeClassPattern;
    }

    /**
     * 调度超时任务
     * 如果配置了超时时间，则在指定时间后自动结束命令执行
     * 如果命令正常结束，则取消超时任务
     *
     * @param process 命令进程
     */
    private void scheduleTimeoutTask(final CommandProcess process) {
        // 如果没有配置超时或超时值无效，则不调度任务
        if (timeout == null || timeout <= 0) {
            return;
        }

        // 使用调度执行器创建延时任务
        final ScheduledFuture<?> timeoutFuture = ArthasBootstrap.getInstance().getScheduledExecutorService()
                .schedule(new Runnable() {
                    @Override
                    public void run() {
                        // 检查进程是否仍在运行
                        if (process.isRunning()) {
                            // 输出超时提示信息
                            process.write("Command execution timeout after " + timeout + " seconds.\n");
                            // 结束进程
                            process.end();
                        }
                    }
                }, timeout, TimeUnit.SECONDS);

        // 设置进程结束处理器，如果进程正常结束则取消超时任务
        // Cancel the timeout task if the process ends normally
        process.endHandler(new com.taobao.arthas.core.shell.handlers.Handler<Void>() {
            @Override
            public void handle(Void event) {
                // 取消超时任务，不中断正在执行的任务（如果正在执行）
                timeoutFuture.cancel(false);
            }
        });
    }
}
