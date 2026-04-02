package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.model.TimeFragmentVO;
import com.taobao.arthas.core.command.model.TimeTunnelModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 时光隧道建议监听器
 * <p>
 * 该监听器用于拦截方法调用，记录每次调用的完整上下文信息（时间碎片）。
 * 它是TimeTunnel命令的核心组件，负责在方法调用前后收集信息，并创建时间碎片记录。
 * </p>
 * <p>
 * 主要功能：
 * 1. 在方法执行前保存参数快照
 * 2. 计算方法执行耗时
 * 3. 根据条件表达式过滤调用
 * 4. 将符合条件的调用记录为时间碎片
 * 5. 控制记录数量，避免内存溢出
 * </p>
 *
 * @author beiwei30 on 30/11/2016.
 * @author hengyunabc 2020-05-20
 */
public class TimeTunnelAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TimeTunnelAdviceListener.class);

    /**
     * 参数栈的大小
     * <p>
     * 定义了每个线程最多可以保存的参数快照数量。
     * 对于嵌套调用的场景，需要保存多层参数。
     * </p>
     */
    private static final int ARGS_STACK_SIZE = 512;

    /**
     * 线程本地参数栈
     * <p>
     * 使用JDK的Object[]实现一个固定大小的环形栈（只存储业务对象），
     * 避免把ArthasClassLoader加载的ObjectStack放进业务线程的ThreadLocalMap里，
     * 导致stop/detach后ArthasClassLoader无法被GC回收。
     * </p>
     * <p>
     * 数据结构约定：
     * - store[0] 存储一个int[1]数组，该数组的第一个元素表示当前位置（pos，范围0..cap）
     * - store[1..cap] 存储实际的参数数组（Object[]）
     * </p>
     * <p>
     * 采用环形栈的设计是为了避免在深度嵌套调用时参数栈溢出，
     * 当栈满时会从头开始覆盖旧的参数记录。
     * </p>
     */
    private final ThreadLocal<Object[]> argsRef = ThreadLocal.withInitial(() -> {
        // 创建存储数组，大小为ARGS_STACK_SIZE + 1（+1是因为索引0用于存储位置信息）
        Object[] store = new Object[ARGS_STACK_SIZE + 1];
        // 索引0处存储位置信息（一个int数组，包含当前位置）
        store[0] = new int[1];
        return store;
    });

    /**
     * 关联的TimeTunnel命令对象
     * <p>
     * 用于访问命令配置，如条件表达式、限制数量等。
     * </p>
     */
    private TimeTunnelCommand command;

    /**
     * 命令处理进程
     * <p>
     * 用于输出结果和控制命令执行。
     * </p>
     */
    private CommandProcess process;

    /**
     * 第一次启动标记
     * <p>
     * 用于标识是否是第一次输出结果，第一次输出时需要设置特殊标志。
     * </p>
     */
    private volatile boolean isFirst = true;

    /**
     * 方法执行计时器
     * <p>
     * 使用ThreadLocal实现，每个线程都有自己独立的计时器，
     * 用于精确测量方法执行的耗时。
     * </p>
     */
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    /**
     * 构造时光隧道建议监听器
     *
     * @param command 关联的TimeTunnel命令对象
     * @param process 命令处理进程
     * @param verbose 是否输出详细日志
     */
    public TimeTunnelAdviceListener(TimeTunnelCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    /**
     * 方法执行前的回调
     * <p>
     * 在目标方法执行前被调用，主要功能：
     * 1. 保存方法参数的快照（原始参数）
     * 2. 开始计时
     * </p>
     * <p>
     * 保存参数快照是为了避免在方法执行过程中参数被修改，
     * 导致记录的参数与实际调用时不一致。
     * </p>
     *
     * @param loader  类加载器
     * @param clazz   目标类
     * @param method  目标方法
     * @param target  目标对象
     * @param args    方法参数
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        // 将参数快照压入栈
        pushArgs(args);
        // 开始计时
        threadLocalWatch.start();
    }

    /**
     * 方法正常返回后的回调
     * <p>
     * 在目标方法正常执行完成后被调用，主要功能：
     * 1. 从栈中弹出保存的参数快照
     * 2. 创建Advice对象（包含返回值）
     * 3. 调用afterFinishing进行后续处理
     * </p>
     *
     * @param loader       类加载器
     * @param clazz        目标类
     * @param method       目标方法
     * @param target       目标对象
     * @param args         方法参数（此时可能已被修改，所以需要从栈中取原始参数）
     * @param returnObject 方法返回值
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        // 取出方法入参时保存的参数快照
        // 因为在函数执行过程中args可能被修改，所以需要使用原始参数
        args = popArgs();
        // 创建Advice对象并调用afterFinishing
        afterFinishing(Advice.newForAfterReturning(loader, clazz, method, target, args, returnObject));
    }

    /**
     * 方法抛出异常后的回调
     * <p>
     * 在目标方法执行过程中抛出异常时被调用，主要功能：
     * 1. 从栈中弹出保存的参数快照
     * 2. 创建Advice对象（包含抛出的异常）
     * 3. 调用afterFinishing进行后续处理
     * </p>
     *
     * @param loader    类加载器
     * @param clazz     目标类
     * @param method    目标方法
     * @param target    目标对象
     * @param args      方法参数（此时可能已被修改，所以需要从栈中取原始参数）
     * @param throwable 抛出的异常
     */
    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        // 取出方法入参时保存的参数快照
        // 因为在函数执行过程中args可能被修改，所以需要使用原始参数
        args = popArgs();
        // 创建Advice对象并调用afterFinishing
        afterFinishing(Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    /**
     * 将参数数组压入栈
     * <p>
     * 使用环形栈结构存储参数快照：
     * - 如果栈未满，位置指针向后移动
     * - 如果栈已满，位置指针回到栈底（覆盖最老的记录）
     * </p>
     *
     * @param args 方法参数数组
     */
    private void pushArgs(Object[] args) {
        // 获取当前线程的存储数组
        Object[] store = argsRef.get();
        // 获取位置信息（存储在索引0处）
        int[] posHolder = (int[]) store[0];

        // 计算栈的容量（总长度-1，因为索引0用于存储位置信息）
        int cap = store.length - 1;
        // 获取当前位置
        int pos = posHolder[0];
        if (pos < cap) {
            // 栈未满，位置指针后移
            pos++;
        } else {
            // 栈已满，位置指针回到栈底（环形覆盖）
            pos = 1;
        }
        // 存储参数数组
        store[pos] = args;
        // 更新位置信息
        posHolder[0] = pos;
    }

    /**
     * 从栈中弹出参数数组
     * <p>
     * 取出最近一次压入的参数快照，并移动位置指针。
     * </p>
     *
     * @return 保存的参数数组
     */
    private Object[] popArgs() {
        // 获取当前线程的存储数组
        Object[] store = argsRef.get();
        // 获取位置信息
        int[] posHolder = (int[]) store[0];

        // 计算栈的容量
        int cap = store.length - 1;
        // 获取当前位置
        int pos = posHolder[0];
        if (pos > 0) {
            // 栈不为空，取出当前位置的参数
            Object[] args = (Object[]) store[pos];
            // 清空该位置，帮助GC
            store[pos] = null;
            // 位置指针前移
            posHolder[0] = pos - 1;
            return args;
        }

        // 栈为空（pos==0），从栈顶取出（环形栈的情况）
        pos = cap;
        Object[] args = (Object[]) store[pos];
        // 清空该位置
        store[pos] = null;
        // 更新位置信息
        posHolder[0] = pos - 1;
        return args;
    }

    /**
     * 方法执行完成后的处理
     * <p>
     * 这是核心处理逻辑，主要功能：
     * 1. 计算方法执行耗时
     * 2. 创建时间碎片对象
     * 3. 判断是否满足条件表达式
     * 4. 将满足条件的调用保存到时间隧道中
     * 5. 输出结果到客户端
     * 6. 检查是否超过限制数量
     * </p>
     *
     * @param advice 方法调用的上下文信息
     */
    private void afterFinishing(Advice advice) {
        // 计算方法执行耗时（毫秒）
        double cost = threadLocalWatch.costInMillis();
        // 创建时间碎片对象，记录当前时间、耗时和调用上下文
        TimeFragment timeTunnel = new TimeFragment(advice, LocalDateTime.now(), cost);

        // 判断是否满足条件表达式
        boolean match = false;
        try {
            match = isConditionMet(command.getConditionExpress(), advice, cost);
            // 如果是verbose模式，输出条件判断结果
            if (this.isVerbose()) {
                process.write("Condition express: " + command.getConditionExpress() + " , result: " + match + "\n");
            }
        } catch (ExpressException e) {
            // 条件表达式执行失败，记录日志并结束命令
            logger.warn("tt failed.", e);
            process.end(-1, "tt failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                          + ", visit " + LogUtil.loggingFile() + " for more details.");
        }

        // 如果不满足条件，直接返回，不保存此调用
        if (!match) {
            return;
        }

        // 将时间碎片保存到TimeTunnel命令中，获取分配的索引
        int index = command.putTimeTunnel(timeTunnel);

        // 创建时间碎片视图对象（用于输出）
        TimeFragmentVO timeFragmentVO = TimeTunnelCommand.createTimeFragmentVO(index, timeTunnel, command.getExpand());
        // 创建TimeTunnel模型对象
        TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                .setTimeFragmentList(Collections.singletonList(timeFragmentVO))
                .setFirst(isFirst);
        // 将结果追加到输出
        process.appendResult(timeTunnelModel);

        // 如果是第一次输出，更新标志
        if (isFirst) {
            isFirst = false;
        }

        // 增加执行次数计数
        process.times().incrementAndGet();
        // 检查是否超过限制数量
        if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
            // 超过限制，终止命令
            abortProcess(process, command.getNumberOfLimit());
        }
    }
}
