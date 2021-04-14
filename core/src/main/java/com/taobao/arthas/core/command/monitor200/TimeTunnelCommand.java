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
import com.taobao.arthas.core.command.model.MessageModel;
import com.taobao.arthas.core.command.model.RowAffectModel;
import com.taobao.arthas.core.command.model.TimeFragmentVO;
import com.taobao.arthas.core.command.model.TimeTunnelModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.cli.annotations.Argument;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.toHexString;
import static java.lang.String.format;

/**
 * 时光隧道命令<br/>
 * 参数w/d依赖于参数i所传递的记录编号<br/>
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
    // 时间隧道(时间碎片的集合)
    // TODO 并非线程安全？
    private static final Map<Integer, TimeFragment> timeFragmentMap = new LinkedHashMap<Integer, TimeFragment>();
    // 时间碎片序列生成器
    private static final AtomicInteger sequence = new AtomicInteger(1000);
    // TimeTunnel the method call
    private boolean isTimeTunnel = false;
    private String classPattern;
    private String methodPattern;
    private String conditionExpress;
    // list the TimeTunnel
    private boolean isList = false;
    private boolean isDeleteAll = false;
    // index of TimeTunnel
    private Integer index;
    // expand of TimeTunnel
    private Integer expand = 1;
    // upper size limit
    private Integer sizeLimit = 10 * 1024 * 1024;
    // watch the index TimeTunnel
    private String watchExpress = com.taobao.arthas.core.util.Constants.EMPTY_STRING;
    private String searchExpress = com.taobao.arthas.core.util.Constants.EMPTY_STRING;
    // play the index TimeTunnel
    private boolean isPlay = false;
    // delete the index TimeTunnel
    private boolean isDelete = false;
    private boolean isRegEx = false;
    private int numberOfLimit = 100;
    private int replayTimes = 1;
    private long replayInterval = 1000L;
    private static final Logger logger = LoggerFactory.getLogger(TimeTunnelCommand.class);

    @Argument(index = 0, argName = "class-pattern", required = false)
    @Description("Path and classname of Pattern Matching")
    public void setClassPattern(String classPattern) {
        this.classPattern = classPattern;
    }

    @Argument(index = 1, argName = "method-pattern", required = false)
    @Description("Method of Pattern Matching")
    public void setMethodPattern(String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Argument(index = 2, argName = "condition-express", required = false)
    @Description(Constants.CONDITION_EXPRESS)
    public void setConditionExpress(String conditionExpress) {
        this.conditionExpress = conditionExpress;
    }

    @Option(shortName = "t", longName = "time-tunnel", flag = true)
    @Description("Record the method invocation within time fragments")
    public void setTimeTunnel(boolean timeTunnel) {
        isTimeTunnel = timeTunnel;
    }

    @Option(shortName = "l", longName = "list", flag = true)
    @Description("List all the time fragments")
    public void setList(boolean list) {
        isList = list;
    }

    @Option(longName = "delete-all", flag = true)
    @Description("Delete all the time fragments")
    public void setDeleteAll(boolean deleteAll) {
        isDeleteAll = deleteAll;
    }

    @Option(shortName = "i", longName = "index")
    @Description("Display the detailed information from specified time fragment")
    public void setIndex(Integer index) {
        this.index = index;
    }

    @Option(shortName = "x", longName = "expand")
    @Description("Expand level of object (1 by default)")
    public void setExpand(Integer expand) {
        this.expand = expand;
    }

    @Option(shortName = "M", longName = "sizeLimit")
    @Description("Upper size limit in bytes for the result (10 * 1024 * 1024 by default)")
    public void setSizeLimit(Integer sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Option(shortName = "w", longName = "watch-express")
    @Description(value = "watch the time fragment by ognl express.\n" + Constants.EXPRESS_EXAMPLES)
    public void setWatchExpress(String watchExpress) {
        this.watchExpress = watchExpress;
    }

    @Option(shortName = "s", longName = "search-express")
    @Description("Search-expression, to search the time fragments by ognl express.\n" +
            "The structure of 'advice' like conditional expression")
    public void setSearchExpress(String searchExpress) {
        this.searchExpress = searchExpress;
    }

    @Option(shortName = "p", longName = "play", flag = true)
    @Description("Replay the time fragment specified by index")
    public void setPlay(boolean play) {
        isPlay = play;
    }

    @Option(shortName = "d", longName = "delete", flag = true)
    @Description("Delete time fragment specified by index")
    public void setDelete(boolean delete) {
        isDelete = delete;
    }

    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match (wildcard matching by default)")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    @Option(shortName = "n", longName = "limits")
    @Description("Threshold of execution times")
    public void setNumberOfLimit(int numberOfLimit) {
        this.numberOfLimit = numberOfLimit;
    }


    @Option(longName = "replay-times")
    @Description("execution times when play tt")
    public void setReplayTimes(int replayTimes) {
        this.replayTimes = replayTimes;
    }

    @Option(longName = "replay-interval")
    @Description("replay interval  for  play tt with option r greater than 1")
    public void setReplayInterval(int replayInterval) {
        this.replayInterval = replayInterval;
    }


    public boolean isRegEx() {
        return isRegEx;
    }

    public String getMethodPattern() {
        return methodPattern;
    }

    public String getClassPattern() {
        return classPattern;
    }

    public String getConditionExpress() {
        return conditionExpress;
    }

    public int getNumberOfLimit() {
        return numberOfLimit;
    }


    public int getReplayTimes() {
        return replayTimes;
    }

    public long getReplayInterval() {
        return replayInterval;
    }

    private boolean hasWatchExpress() {
        return !StringUtils.isEmpty(watchExpress);
    }

    private boolean hasSearchExpress() {
        return !StringUtils.isEmpty(searchExpress);
    }

    /**
     * 检查参数是否合法
     */
    private void checkArguments() {
        // 检查d/p参数是否有i参数配套
        if ((isDelete || isPlay) && null == index) {
            throw new IllegalArgumentException("Time fragment index is expected, please type -i to specify");
        }

        // 在t参数下class-pattern,method-pattern
        if (isTimeTunnel) {
            if (StringUtils.isEmpty(classPattern)) {
                throw new IllegalArgumentException("Class-pattern is expected, please type the wildcard expression to match");
            }
            if (StringUtils.isEmpty(methodPattern)) {
                throw new IllegalArgumentException("Method-pattern is expected, please type the wildcard expression to match");
            }
        }

        // 一个参数都没有是不行滴
        if (null == index && !isTimeTunnel && !isDeleteAll && StringUtils.isEmpty(watchExpress)
                && !isList && StringUtils.isEmpty(searchExpress)) {
            throw new IllegalArgumentException("Argument(s) is/are expected, type 'help tt' to read usage");
        }
    }

    /*
     * 记录时间片段
     */
    int putTimeTunnel(TimeFragment tt) {
        int indexOfSeq = sequence.getAndIncrement();
        timeFragmentMap.put(indexOfSeq, tt);
        return indexOfSeq;
    }

    @Override
    public void process(final CommandProcess process) {
        // 检查参数
        checkArguments();

        // ctrl-C support
        process.interruptHandler(new CommandInterruptHandler(process));
        // q exit support
        process.stdinHandler(new QExitHandler(process));

        if (isTimeTunnel) {
            enhance(process);
        } else if (isPlay) {
            processPlay(process);
        } else if (isList) {
            processList(process);
        } else if (isDeleteAll) {
            processDeleteAll(process);
        } else if (isDelete) {
            processDelete(process);
        } else if (hasSearchExpress()) {
            processSearch(process);
        } else if (index != null) {
            if (hasWatchExpress()) {
                processWatch(process);
            } else {
                processShow(process);
            }
        }
    }

    @Override
    protected Matcher getClassNameMatcher() {
        if (classNameMatcher == null) {
            classNameMatcher = SearchUtils.classNameMatcher(getClassPattern(), isRegEx());
        }
        return classNameMatcher;
    }

    @Override
    protected Matcher getClassNameExcludeMatcher() {
        if (classNameExcludeMatcher == null && getExcludeClassPattern() != null) {
            classNameExcludeMatcher = SearchUtils.classNameMatcher(getExcludeClassPattern(), isRegEx());
        }
        return classNameExcludeMatcher;
    }

    @Override
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new TimeTunnelAdviceListener(this, process, GlobalOptions.verbose || this.verbose);
    }

    // 展示指定记录
    private void processShow(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            TimeFragment tf = timeFragmentMap.get(index);
            if (null == tf) {
                process.end(1, format("Time fragment[%d] does not exist.", index));
                return;
            }

            TimeFragmentVO timeFragmentVO = createTimeFragmentVO(index, tf);
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

    // 查看记录信息
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
                    .setWatchValue(value)
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

    // do search timeFragmentMap
    private void processSearch(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            // 匹配的时间片段
            Map<Integer, TimeFragment> matchingTimeSegmentMap = new LinkedHashMap<Integer, TimeFragment>();
            for (Map.Entry<Integer, TimeFragment> entry : timeFragmentMap.entrySet()) {
                int index = entry.getKey();
                TimeFragment tf = entry.getValue();
                Advice advice = tf.getAdvice();

                // 搜索出匹配的时间片段
                if ((ExpressFactory.threadLocalExpress(advice)).is(searchExpress)) {
                    matchingTimeSegmentMap.put(index, tf);
                }
            }

            if (hasWatchExpress()) {
                // 执行watchExpress
                Map<Integer, Object> searchResults = new LinkedHashMap<Integer, Object>();
                for (Map.Entry<Integer, TimeFragment> entry : matchingTimeSegmentMap.entrySet()) {
                    Object value = ExpressFactory.threadLocalExpress(entry.getValue().getAdvice()).get(watchExpress);
                    searchResults.put(entry.getKey(), value);
                }

                TimeTunnelModel timeTunnelModel = new TimeTunnelModel()
                        .setWatchResults(searchResults)
                        .setExpand(expand)
                        .setSizeLimit(sizeLimit);
                process.appendResult(timeTunnelModel);
            } else {
                // 单纯的列表格
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

    // 删除指定记录
    private void processDelete(CommandProcess process) {
        RowAffect affect = new RowAffect();
        if (timeFragmentMap.remove(index) != null) {
            affect.rCnt(1);
        }
        process.appendResult(new MessageModel(format("Time fragment[%d] successfully deleted.", index)));
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    private void processDeleteAll(CommandProcess process) {
        int count = timeFragmentMap.size();
        RowAffect affect = new RowAffect(count);
        timeFragmentMap.clear();
        process.appendResult(new MessageModel("Time fragments are cleaned."));
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    private void processList(CommandProcess process) {
        RowAffect affect = new RowAffect();
        List<TimeFragmentVO> timeFragmentList = createTimeTunnelVOList(timeFragmentMap);
        process.appendResult(new TimeTunnelModel().setTimeFragmentList(timeFragmentList).setFirst(true));
        affect.rCnt(timeFragmentMap.size());
        process.appendResult(new RowAffectModel(affect));
        process.end();
    }

    private List<TimeFragmentVO> createTimeTunnelVOList(Map<Integer, TimeFragment> timeFragmentMap) {
        List<TimeFragmentVO> timeFragmentList = new ArrayList<TimeFragmentVO>(timeFragmentMap.size());
        for (Map.Entry<Integer, TimeFragment> entry : timeFragmentMap.entrySet()) {
            timeFragmentList.add(createTimeFragmentVO(entry.getKey(), entry.getValue()));
        }
        return timeFragmentList;
    }

    public static TimeFragmentVO createTimeFragmentVO(Integer index, TimeFragment tf) {
        Advice advice = tf.getAdvice();
        String object = advice.getTarget() == null
                ? "NULL"
                : "0x" + toHexString(advice.getTarget().hashCode());

        return new TimeFragmentVO()
                .setIndex(index)
                .setTimestamp(tf.getGmtCreate())
                .setCost(tf.getCost())
                .setParams(advice.getParams())
                .setReturn(advice.isAfterReturning())
                .setReturnObj(advice.getReturnObj())
                .setThrow(advice.isAfterThrowing())
                .setThrowExp(advice.getThrowExp())
                .setObject(object)
                .setClassName(advice.getClazz().getName())
                .setMethodName(advice.getMethod().getName());
    }

    /**
     * 重放指定记录
     */
    private void processPlay(CommandProcess process) {
        TimeFragment tf = timeFragmentMap.get(index);
        if (null == tf) {
            process.end(1, format("Time fragment[%d] does not exist.", index));
            return;
        }
        Advice advice = tf.getAdvice();
        ArthasMethod method = advice.getMethod();
        boolean accessible = advice.getMethod().isAccessible();
        try {
            if (!accessible) {
                method.setAccessible(true);
            }
            for (int i = 0; i < getReplayTimes(); i++) {
                if (i > 0) {
                    //wait for the next execution
                    Thread.sleep(getReplayInterval());
                    if (!process.isRunning()) {
                        return;
                    }
                }
                long beginTime = System.nanoTime();

                //copy from tt record
                TimeFragmentVO replayResult = createTimeFragmentVO(index, tf);
                replayResult.setTimestamp(new Date())
                        .setCost(0)
                        .setReturn(false)
                        .setReturnObj(null)
                        .setThrow(false)
                        .setThrowExp(null);

                try {
                    //execute successful
                    Object returnObj = method.invoke(advice.getTarget(), advice.getParams());
                    double cost = (System.nanoTime() - beginTime) / 1000000.0;
                    replayResult.setCost(cost)
                            .setReturn(true)
                            .setReturnObj(returnObj);
                } catch (Throwable t) {
                    //throw exp
                    double cost = (System.nanoTime() - beginTime) / 1000000.0;
                    replayResult.setCost(cost)
                            .setThrow(true)
                            .setThrowExp(t);
                }

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
            method.setAccessible(accessible);
        }
    }
}
