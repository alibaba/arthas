package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.GlobalOptions;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.*;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;

/**
 * 时光隧道命令
 * <p>
 * TimeTunnel（tt）命令是Arthas的核心功能之一，它可以记录方法调用的完整上下文，
 * 让开发者能够"穿越回"某个时刻，重新查看或执行该方法。
 * </p>
 * <p>
 * 主要功能：
 * 1. 记录方法调用（-t）：记录匹配的方法调用，生成时间碎片
 * 2. 列出所有时间碎片（-l）
 * 3. 查看指定时间碎片（-i）：显示完整的调用信息
 * 4. 重放方法调用（-p）：使用原始参数重新执行方法
 * 5. 删除时间碎片（-d）
 * 6. 搜索时间碎片（-s）：根据条件表达式搜索
 * 7. 观察表达式（-w）：对指定时间碎片执行OGNL表达式
 * </p>
 * <p>
 * 注意：参数w/d依赖于参数i所传递的记录编号
 * </p>
 *
 * @author vlinux on 14/11/15.
 */
@Name("tt")
@Summary("Time Tunnel")
@Description(Constants.EXPRESS_DESCRIPTION + Constants.EXAMPLE +
        "  tt -t *StringUtils isEmpty\n" +
        "  tt -t *StringUtils isEmpty params[0].length==1\n" +
        "  tt -l\n" +
        "  tt -i 1000\n" +
        "  tt -i 1000 -w params[0]\n" +
        "  tt -i 1000 -p \n" +
        "  tt -i 1000 -p --replay-times 3 --replay-interval 3000\n" +
        "  tt -s '{params[0] > 1}' -w '{params}' \n" +
        "  tt --delete-all\n" +
        Constants.WIKI + Constants.WIKI_HOME + "tt")
public class TimeTunnelCommand extends EnhancerCommand {

    /**
     * 时间碎片存储映射表
     * <p>
     * 使用LinkedHashMap保持插入顺序，键为索引，值为时间碎片对象。
     * 存储所有记录的方法调用信息。
     * </p>
     * TODO 并非线程安全？在多线程环境下可能需要同步
     */
    private static final Map<Integer, TimeFragment> timeFragmentMap = new LinkedHashMap<Integer, TimeFragment>();

    /**
     * 时间碎片索引序列生成器
     * <p>
     * 从1000开始递增，为每个记录的时间碎片分配唯一索引。
     * 使用AtomicInteger保证线程安全。
     * </p>
     */
    private static final AtomicInteger sequence = new AtomicInteger(1000);

    /**
     * 是否开启时光隧道（记录方法调用）
     */
    private boolean isTimeTunnel = false;

    /**
     * 类名匹配模式
     */
    private String classPattern;

    /**
     * 方法名匹配模式
     */
    private String methodPattern;

    /**
     * 条件表达式
     * <p>
     * 用于过滤方法调用，只有满足条件的调用才会被记录。
     * </p>
     */
    private String conditionExpress;

    /**
     * 是否列出所有时间碎片
     */
    private boolean isList = false;

    /**
     * 是否删除所有时间碎片
     */
    private boolean isDeleteAll = false;

    /**
     * 时间碎片的索引
     * <p>
     * 用于指定要查看、重放或删除的时间碎片。
     * </p>
     */
    private Integer index;

    /**
     * 对象展开层级
     * <p>
     * 控制对象属性的展开深度，默认为1。
     * </p>
     */
    private Integer expand = 1;

    /**
     * 结果大小上限（字节）
     * <p>
     * 限制输出结果的大小，防止输出过多数据。
     * </p>
     */
    private Integer sizeLimit;

    /**
     * 观察表达式
     * <p>
     * 用于对指定时间碎片执行OGNL表达式，获取特定信息。
     * </p>
     */
    private String watchExpress = com.taobao.arthas.core.util.Constants.EMPTY_STRING;

    /**
     * 搜索表达式
     * <p>
     * 用于搜索满足条件的时间碎片。
     * </p>
     */
    private String searchExpress = com.taobao.arthas.core.util.Constants.EMPTY_STRING;

    /**
     * 是否重放（重新执行）时间碎片
     */
    private boolean isPlay = false;

    /**
     * 是否删除指定时间碎片
     */
    private boolean isDelete = false;

    /**
     * 是否使用正则表达式匹配
     * <p>
     * 默认使用通配符匹配，设置为true后使用正则表达式。
     * </p>
     */
    private boolean isRegEx = false;

    /**
     * 执行次数限制
     * <p>
     * 限制记录的方法调用次数，默认为100。
     * </p>
     */
    private int numberOfLimit = 100;

    /**
     * 重放次数
     * <p>
     * 重放时间碎片时执行的次数，默认为1。
     * </p>
     */
    private int replayTimes = 1;

    /**
     * 重放间隔（毫秒）
     * <p>
     * 当重放次数大于1时，每次重放之间的间隔时间，默认为1000毫秒。
     * </p>
     */
    private long replayInterval = 1000L;

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(TimeTunnelCommand.class);

    /**
     * 设置类名匹配模式
     *
     * @param classPattern 类名匹配模式
     */
    @Argument(index = 0, argName = "class-pattern", required = false)
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    /**
     * 设置方法名匹配模式
     *
     * @param methodPattern 方法名匹配模式
     */
    @Argument(index = 1, argName = "method-pattern", required = false)
    @Description("Method of Pattern Matching")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    /**
     * 设置条件表达式
     *
     * @param conditionExpress 条件表达式
     */
    @Argument(index = 2, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    /**
     * 设置是否开启时光隧道
     *
     * @param timeTunnel 是否开启时光隧道
     */
    @Option(shortName = "t", longName = "time-tunnel", flag = true)
    @Description("Record the method invocation within time fragments")
    public void setTimeTunnel(boolean timeTunnel) {
        isTimeTunnel = timeTunnel;
    }

    /**
     * 设置是否列出所有时间碎片
     *
     * @param list 是否列出所有时间碎片
     */
    @Option(shortName = "l", longName = "list", flag = true)
    @Description("List all the time fragments")
    public void setList(boolean list) {
        isList = list;
    }

    /**
     * 设置是否删除所有时间碎片
     *
     * @param deleteAll 是否删除所有时间碎片
     */
    @Option(longName = "delete-all", flag = true)
    @Description("Delete all the time fragments")
    public void setDeleteAll(boolean deleteAll) {
        isDeleteAll = deleteAll;
    }

    /**
     * 设置时间碎片索引
     *
     * @param index 时间碎片索引
     */
    @Option(shortName = "i", longName = "index")
    @Description("Display the detailed information from specified time fragment")
    public void setIndex(Integer index) {
        this.index = index;
    }

    /**
     * 设置对象展开层级
     *
     * @param expand 展开层级
     */
    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    /**
     * 设置结果大小上限
     *
     * @param sizeLimit 大小上限（字节）
     */
    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (must be greater than 0, default value comes from options object-size-limit)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    /**
     * 设置类加载器哈希码
     *
     * @param hashCode 类加载器哈希码
     */
    @Override
    @Option(shortName = "c", longName = "classloader")
    @Description("The hash code of the special class's classLoader")
    public void setHashCode(String hashCode) {
        super.setHashCode(hashCode);
    }

    /**
     * 设置观察表达式
     *
     * @param watchExpress 观察表达式
     */
    @Option(shortName = "w", longName = "watch-express")
    @Description(value = "watch the time fragment by ognl express.\n" + Constants.EXPRESS_EXAMPLES)
    public void setWatchExpress(String watchExpress) {
        this.watchExpress = watchExpress;
    }

    /**
     * 设置搜索表达式
     *
     * @param searchExpress 搜索表达式
     */
    @Option(shortName = "s", longName = "search-express")
    @Description("Search-expression, to search the time fragments by ognl express.\n" +
            "The structure of 'advice' like conditional expression")
    public void setSearchExpress(String searchExpress) {
        this.searchExpress = searchExpress;
    }

    /**
     * 设置是否重放时间碎片
     *
     * @param play 是否重放
     */
    @Option(shortName = "p", longName = "play", flag = true)
    @Description("Replay the time fragment specified by index")
    public void setPlay(boolean play) {
        isPlay = play;
    }

    /**
     * 设置是否删除时间碎片
     *
     * @param delete 是否删除
     */
    @Option(shortName = "d", longName = "delete", flag = true)
    @Description("Delete time fragment specified by index")
    public void setDelete(boolean delete) {
        isDelete = delete;
    }

    /**
     * 设置是否使用正则表达式
     *
     * @param regEx 是否使用正则表达式
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置执行次数限制
     *
     * @param numberOfLimit 限制次数
     */
    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times, default value 100")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }


    /**
     * 设置重放次数
     *
     * @param replayTimes 重放次数
     */
    @Option(longName = "replay-times")
    @Description("execution times when play tt")
    public void setReplayTimes(int replayTimes) {
        this.replayTimes = replayTimes;
    }

    /**
     * 设置重放间隔
     *
     * @param replayInterval 重放间隔（毫秒）
     */
    @Option(longName = "replay-interval")
    @Description("replay interval  for  play tt with option r greater than 1")
    public void setReplayInterval(int replayInterval) {
        this.replayInterval = replayInterval;
    }


    /**
     * 获取是否使用正则表达式
     *
     * @return 是否使用正则表达式
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    /**
     * 获取方法名匹配模式
     *
     * @return 方法名匹配模式
     */
    public String getMethodPattern() {
        return methodPattern;
    }

    /**
     * 获取类名匹配模式
     *
     * @return 类名匹配模式
     */
    public String getClassPattern() {
        return classPattern;
    }

    /**
     * 获取条件表达式
     *
     * @return 条件表达式
     */
    public String getConditionExpress() {
        return conditionExpress;
    }

    /**
     * 获取执行次数限制
     *
     * @return 限制次数
     */
    public int getNumberOfLimit() {
        return numberOfLimit;
    }

    /**
     * 获取重放次数
     *
     * @return 重放次数
     */
    public int getReplayTimes() {
        return replayTimes;
    }

    /**
     * 获取重放间隔
     *
     * @return 重放间隔（毫秒）
     */
    public long getReplayInterval() {
        return replayInterval;
    }

    /**
     * 获取对象展开层级
     *
     * @return 展开层级
     */
    public Integer getExpand() {
        return expand;
    }

    /**
     * 判断是否有观察表达式
     *
     * @return 如果有观察表达式返回true，否则返回false
     */
    private boolean hasWatchExpress() {
        return !StringUtils.isEmpty(watchExpress);
    }

    /**
     * 判断是否有搜索表达式
     *
     * @return 如果有搜索表达式返回true，否则返回false
     */
    private boolean hasSearchExpress() {
        return !StringUtils.isEmpty(searchExpress);
    }

    /**
     * 验证大小限制参数
     *
     * @param sizeLimit 大小限制值
     * @return 如果验证失败返回错误信息，否则返回null
     */
    static String validateSizeLimit(Integer sizeLimit) {
        if (sizeLimit != null && sizeLimit.intValue() <= 0) {
            return "sizeLimit must be greater than 0.";
        }
        return null;
    }

    /**
     * 检查参数是否合法
     * <p>
     * 验证命令参数的组合是否合法，包括：
     * 1. 大小限制必须大于0
     * 2. 删除和重放操作必须有索引参数
     * 3. 开启时光隧道必须有类名和方法名
     * 4. 至少要有一个有效参数
     * </p>
     *
     * @throws IllegalArgumentException 如果参数不合法
     */
    private void checkArguments() {
        // 验证大小限制参数
        String validateError = validateSizeLimit(sizeLimit);
        if (validateError != null) {
            throw new IllegalArgumentException(validateError);
        }

        // 检查删除或重放参数是否有索引参数配套
        if ((isDelete || isPlay) && null == index) {
            throw new IllegalArgumentException("Time fragment index is expected, please type -i to specify");
        }

        // 在开启时光隧道时，必须有类名和方法名
        if (isTimeTunnel) {
            if (StringUtils.isEmpty(classPattern)) {
                throw new IllegalArgumentException("Class-pattern is expected, please type the wildcard expression to match");
            }
            if (StringUtils.isEmpty(methodPattern)) {
                throw new IllegalArgumentException("Method-pattern is expected, please type the wildcard expression to match");
            }
        }

        // 至少要有一个有效参数（不能所有参数都是空的）
        if (null == index && !isTimeTunnel && !isDeleteAll && StringUtils.isEmpty(watchExpress)
                && !isList && StringUtils.isEmpty(searchExpress)) {
            throw new IllegalArgumentException("Argument(s) is/are expected, type 'help tt' to read usage");
        }
    }

    /**
     * 记录时间片段
     * <p>
     * 将时间碎片保存到存储映射表中，并分配一个唯一的索引。
     * </p>
     *
     * @param tt 时间碎片对象
     * @return 分配的索引
     */
    int putTimeTunnel(TimeFragment tt) {
        // 获取并递增序列号
        int indexOfSeq = sequence.getAndIncrement();
        // 保存时间碎片
        timeFragmentMap.put(indexOfSeq, tt);
        return indexOfSeq;
    }

    /**
     * 处理命令
     * <p>
     * 根据不同的参数组合执行相应的操作：
     * - -t: 开启时光隧道，记录方法调用
     * - -p: 重放指定时间碎片
     * - -l: 列出所有时间碎片
     * - --delete-all: 删除所有时间碎片
     * - -d: 删除指定时间碎片
     * - -s: 搜索时间碎片
     * - -i: 查看指定时间碎片或执行观察表达式
     * </p>
     *
     * @param process 命令处理进程
     */
    @Override
    public void process(final CommandProcess process) {
        // 检查参数合法性
        checkArguments();

        // 注册Ctrl-C中断处理器
        process.interruptHandler(new CommandInterruptHandler(process));
        // 注册q退出处理器
        process.stdinHandler(new QExitHandler(process));

        // 根据参数类型执行相应操作
        if (isTimeTunnel) {
            // 开启时光隧道，增强类以拦截方法调用
            enhance(process);
        } else if (isPlay) {
            // 重放指定时间碎片
            processPlay(process);
        } else if (isList) {
            // 列出所有时间碎片
            processList(process);
        } else if (isDeleteAll) {
            // 删除所有时间碎片
            processDeleteAll(process);
        } else if (isDelete) {
            // 删除指定时间碎片
            processDelete(process);
        } else if (hasSearchExpress()) {
            // 搜索时间碎片
            processSearch(process);
        } else if (index != null) {
            // 有索引参数
            if (hasWatchExpress()) {
                // 执行观察表达式
                processWatch(process);
            } else {
                // 查看时间碎片详情
                processShow(process);
            }
        }
    }

    /**
     * 获取类名匹配器
     * <p>
     * 使用延迟初始化，根据配置创建类名匹配器（支持通配符或正则表达式）。
     * </p>
     *
     * @return 类名匹配器
     */
    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    /**
     * 获取类名排除匹配器
     * <p>
     * 用于排除指定的类，使用延迟初始化。
     * </p>
     *
     * @return 类名排除匹配器
     */
    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    /**
     * 获取方法名匹配器
     * <p>
     * 使用延迟初始化，根据配置创建方法名匹配器（支持通配符或正则表达式）。
     * </p>
     *
     * @return 方法名匹配器
     */
    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    /**
     * 获取建议监听器
     * <p>
     * 创建TimeTunnelAdviceListener用于拦截方法调用。
     * </p>
     *
     * @param process 命令处理进程
     * @return 建议监听器
     */
    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new TimeTunnelAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    /**
     * 展示指定的时间碎片详情
     * <p>
     * 根据索引获取时间碎片，并将其详细信息输出到客户端。
     * </p>
     *
     * @param process 命令处理进程
     */
    private void processShow(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            // 根据索引获取时间碎片
            TimeFragment tf = timeFragmentMap.get(index);
            if (null == tf) {
                // 时间碎片不存在
                process.end(1, format("Time fragment[%d] does not exist.", index));
                return;
            }

            // 创建时间碎片视图对象
            TimeFragmentVO timeFragmentVO = createTimeFragmentVO(index, tf, expand);
            // 创建TimeTunnel模型对象并输出
            TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                    .setTimeFragment(timeFragmentVO)
                    .setExpand(expand)
                    .setSizeLimit(sizeLimit);
            process.appendResult(timeTunnelModel);
            affect.rCnt(1);
            process.appendResult(new RowAffectModel(affect));
            process.end();
        } catch (Throwable e) {
            logger.warn("tt failed.", e);
            process.end(1, e.getMessage() + ", visit " + LogUtil.loggingFile() + " for more detail");
        }
    }

    /**
     * 对指定的时间碎片执行观察表达式
     * <p>
     * 根据索引获取时间碎片，对其Advice对象执行OGNL表达式，并将结果输出到客户端。
     * </p>
     *
     * @param process 命令处理进程
     */
    private void processWatch(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            final TimeFragment tf = timeFragmentMap.get(index);
            if (null == tf) {
                process.end(1, format("Time fragment[%d] does not exist.", index));
                return;
            }

            Advice advice = tf.getAdvice();

			Object value = ExpressFactory.unpooledExpress(advice.getLoader()).bind(advice).get(watchExpress);
            TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                    .setWatchValue(new ObjectVO(value, expand))
                    .setExpand(expand)
                    .setSizeLimit(sizeLimit);
            process.appendResult(timeTunnelModel);

            affect.rCnt(1);
            process.appendResult(new RowAffectModel(affect));
            process.end();
        } catch (ExpressException e) {
            logger.warn("tt failed.", e);
            process.end(1, e.getMessage() + ", visit " + LogUtil.loggingFile() + " for more detail");
        }
    }

    /**
     * 搜索时间碎片
     * <p>
     * 根据搜索表达式遍历所有时间碎片，找出满足条件的记录。
     * 如果同时指定了观察表达式，则对每个匹配的记录执行观察表达式。
     * </p>
     *
     * @param process 命令处理进程
     */
    private void processSearch(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            // 存储匹配的时间碎片
            Map<Integer, TimeFragment> matchingTimeSegmentMap = new LinkedHashMap<Integer, TimeFragment>();
            // 遍历所有时间碎片
            for (Map.Entry<Integer, TimeFragment> entry : timeFragmentMap.entrySet()) {
                int index = entry.getKey();
                TimeFragment tf = entry.getValue();
                Advice advice = tf.getAdvice();

                // 判断是否满足搜索条件
                if ((ExpressFactory.threadLocalExpress(advice)).is(searchExpress)) {
                    matchingTimeSegmentMap.put(index, tf);
                }
            }

            if (hasWatchExpress()) {
                // 如果有观察表达式，对每个匹配的时间碎片执行观察表达式
                Map<Integer, ObjectVO> searchResults = new LinkedHashMap<Integer, ObjectVO>();
                for (Map.Entry<Integer, TimeFragment> entry : matchingTimeSegmentMap.entrySet()) {
                    Object value = ExpressFactory.threadLocalExpress(entry.getValue().getAdvice()).get(watchExpress);
                    searchResults.put(entry.getKey(), new ObjectVO(value, expand));
                }

                // 输出观察结果
                TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                        .setWatchResults(searchResults)
                        .setExpand(expand)
                        .setSizeLimit(sizeLimit);
                process.appendResult(timeTunnelModel);
            } else {
                // 没有观察表达式，单纯列出匹配的时间碎片
                List<TimeFragmentVO> timeFragmentList = createTimeTunnelVOList(matchingTimeSegmentMap);
                process.appendResult(new TimeTunnelModel().setTimeFragmentList(timeFragmentList).setFirst(true));
            }

            affect.rCnt(matchingTimeSegmentMap.size());
            process.appendResult(new RowAffectModel(affect));
            process.end();
        } catch (ExpressException e) {
            logger.warn("tt failed.", e);
            process.end(1, e.getMessage() + ", visit " + LogUtil.loggingFile() + " for more detail");
        }
    }

    /**
     * 删除指定的时间碎片
     * <p>
     * 根据索引删除时间碎片。
     * </p>
     *
     * @param process 命令处理进程
     */
    private void processDelete(CommandProcess process) {
        RowAffect affect = new RowAffect();
        if (timeFragmentMap.remove(index) != null) {
            affect.rCnt(1);
        }
        process.appendResult(new MessageModel(format("Time fragment[%d] successfully deleted.", index)));
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    /**
     * 删除所有时间碎片
     * <p>
     * 清空时间碎片存储映射表。
     * </p>
     *
     * @param process 命令处理进程
     */
    private void processDeleteAll(CommandProcess process) {
        int count = timeFragmentMap.size();
        RowAffect affect = new RowAffect(count);
        // 清空映射表
        timeFragmentMap.clear();
        process.appendResult(new MessageModel("Time fragments are cleaned."));
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    /**
     * 列出所有时间碎片
     * <p>
     * 将所有已记录的时间碎片以表格形式输出。
     * </p>
     *
     * @param process 命令处理进程
     */
    private void processList(CommandProcess process) {
        RowAffect affect = new RowAffect();
        // 创建时间碎片视图列表
        List<TimeFragmentVO> timeFragmentList = createTimeTunnelVOList(timeFragmentMap);
        process.appendResult(new TimeTunnelModel().setTimeFragmentList(timeFragmentList).setFirst(true));
        affect.rCnt(timeFragmentMap.size());
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    /**
     * 创建时间碎片视图列表
     * <p>
     * 将时间碎片映射表转换为视图对象列表。
     * </p>
     *
     * @param timeFragmentMap 时间碎片映射表
     * @return 时间碎片视图列表
     */
    private List<TimeFragmentVO> createTimeTunnelVOList(Map<Integer, TimeFragment> timeFragmentMap) {
        List<TimeFragmentVO> timeFragmentList = new ArrayList<TimeFragmentVO>(timeFragmentMap.size());
        for (Map.Entry<Integer, TimeFragment> entry : timeFragmentMap.entrySet()) {
            timeFragmentList.add(createTimeFragmentVO(entry.getKey(), entry.getValue(), expand));
        }
        return timeFragmentList;
    }

    /**
     * 创建时间碎片视图对象
     * <p>
     * 将时间碎片和索引转换为视图对象，用于输出到客户端。
     * 视图对象包含了时间碎片的所有关键信息。
     * </p>
     *
     * @param index  时间碎片索引
     * @param tf     时间碎片对象
     * @param expand 对象展开层级
     * @return 时间碎片视图对象
     */
    public static TimeFragmentVO createTimeFragmentVO(Integer index, TimeFragment tf, Integer expand) {
        Advice advice = tf.getAdvice();
        // 计算目标对象的哈希码字符串（16进制格式）
        String object = advice.getTarget() == null
                ? "NULL"
                : "0x" + toHexString(advice.getTarget().hashCode());

        // 构建并返回时间碎片视图对象
        return new TimeFragmentVO()
                .setIndex(index)                    // 索引
                .setTimestamp(tf.getGmtCreate())    // 创建时间
                .setCost(tf.getCost())              // 执行耗时
                .setParams(ObjectVO.array(advice.getParams(), expand))  // 方法参数
                .setReturn(advice.isAfterReturning())  // 是否正常返回
                .setReturnObj(new ObjectVO(advice.getReturnObj(), expand))  // 返回值
                .setThrow(advice.isAfterThrowing())    // 是否抛出异常
                .setThrowExp(new ObjectVO(advice.getThrowExp(), expand))  // 异常对象
                .setObject(object)                    // 目标对象哈希码
                .setClassName(advice.getClazz().getName())  // 类名
                .setMethodName(advice.getMethod().getName());  // 方法名
    }

    /**
     * 重放指定的时间碎片
     * <p>
     * 使用原始参数重新执行记录的方法调用，可以重复执行多次。
     * 主要功能：
     * 1. 获取时间碎片中的方法和参数
     * 2. 设置方法可访问性（如果需要）
     * 3. 重复执行指定次数
     * 4. 记录每次执行的结果和耗时
     * 5. 恢复方法的可访问性
     * </p>
     *
     * @param process 命令处理进程
     */
    private void processPlay(CommandProcess process) {
        // 根据索引获取时间碎片
        TimeFragment tf = timeFragmentMap.get(index);
        if (null == tf) {
            // 时间碎片不存在
            process.end(1, format("Time fragment[%d] does not exist.", index));
            return;
        }
        // 获取Advice对象
        Advice advice = tf.getAdvice();
        // 获取方法对象
        ArthasMethod method = advice.getMethod();
        // 保存方法当前的可访问性
        boolean accessible = advice.getMethod().isAccessible();
        try {
            // 如果方法不可访问，设置为可访问
            if (!accessible) {
                method.setAccessible(true);
            }
            // 根据重放次数循环执行
            for (int i = 0; i < getReplayTimes(); i++) {
                if (i > 0) {
                    // 如果不是第一次，等待指定的间隔时间
                    Thread.sleep(getReplayInterval());
                    // 检查进程是否还在运行
                    if (!process.isRunning()) {
                        return;
                    }
                }
                // 记录开始时间
                long beginTime = System.nanoTime();

                // 创建重放结果对象（从tt记录中复制基础信息）
                TimeFragmentVO replayResult = createTimeFragmentVO(index, tf, expand);
                // 设置新的时间戳和初始状态
                replayResult.setTimestamp(LocalDateTime.now())
                        .setCost(0)
                        .setReturn(false)
                        .setReturnObj(null)
                        .setThrow(false)
                        .setThrowExp(null);

                try {
                    // 执行方法：使用原始目标对象和参数
                    Object returnObj = method.invoke(advice.getTarget(), advice.getParams());
                    // 计算执行耗时（毫秒）
                    double cost = (System.nanoTime() - beginTime) / 1000000.0;
                    // 执行成功，更新结果
                    replayResult.setCost(cost)
                            .setReturn(true)
                            .setReturnObj(new ObjectVO(returnObj, expand));
                } catch (Throwable t) {
                    // 执行失败（抛出异常）
                    double cost = (System.nanoTime() - beginTime) / 1000000.0;
                    replayResult.setCost(cost)
                            .setThrow(true)
                            .setThrowExp(new ObjectVO(t, expand));
                }

                // 输出重放结果
                TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                        .setReplayResult(replayResult)
                        .setReplayNo(i + 1)
                        .setExpand(expand)
                        .setSizeLimit(sizeLimit);
                process.appendResult(timeTunnelModel);
            }
            process.end();
        } catch (Throwable t) {
            logger.warn("tt replay failed.", t);
            process.end(-1, "tt replay failed");
        } finally {
            // 恢复方法原有的可访问性
            method.setAccessible(accessible);
        }
    }
}
