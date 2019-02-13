package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.SearchUtils;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.affect.RowAffect;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.view.ObjectView;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.LinkedHashMap;
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
        "  tt --delete-all\n" +
        Constants.WIKI + Constants.WIKI_HOME + "tt")
public class TimeTunnelCommand extends EnhancerCommand {
    // 时间隧道(时间碎片的集合)
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

    private boolean isNeedExpand() {
        return null != expand && expand > 0;
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
    protected Matcher getMethodNameMatcher() {
        if (methodNameMatcher == null) {
            methodNameMatcher = SearchUtils.classNameMatcher(getMethodPattern(), isRegEx());
        }
        return methodNameMatcher;
    }

    @Override
    protected AdviceListener getAdviceListener(CommandProcess process) {
        return new TimeTunnelAdviceListener(this, process);
    }

    // 展示指定记录
    private void processShow(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            TimeFragment tf = timeFragmentMap.get(index);
            if (null == tf) {
                process.write(format("Time fragment[%d] does not exist.", index)).write("\n");
                return;
            }

            Advice advice = tf.getAdvice();
            String className = advice.getClazz().getName();
            String methodName = advice.getMethod().getName();
            String objectAddress = advice.getTarget() == null ? "NULL" : "0x" + toHexString(advice.getTarget().hashCode());

            TableElement table = TimeTunnelTable.createDefaultTable();
            TimeTunnelTable.drawTimeTunnel(tf, index, table);
            TimeTunnelTable.drawMethod(advice, className, methodName, objectAddress, table);
            TimeTunnelTable.drawParameters(advice, table, isNeedExpand(), expand);
            TimeTunnelTable.drawReturnObj(advice, table, isNeedExpand(), expand, sizeLimit);
            TimeTunnelTable.drawThrowException(advice, table, isNeedExpand(), expand);

            process.write(RenderUtil.render(table, process.width()));
            affect.rCnt(1);
        } finally {
            process.write(affect.toString()).write("\n");
            process.end();
        }
    }

    // 查看记录信息
    private void processWatch(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            final TimeFragment tf = timeFragmentMap.get(index);
            if (null == tf) {
                process.write(format("Time fragment[%d] does not exist.", index)).write("\n");
                return;
            }

            Advice advice = tf.getAdvice();
            Object value = ExpressFactory.threadLocalExpress(advice).get(watchExpress);
            if (isNeedExpand()) {
                process.write(new ObjectView(value, expand, sizeLimit).draw()).write("\n");
            } else {
                process.write(StringUtils.objectToString(value)).write("\n");
            }

            affect.rCnt(1);
        } catch (ExpressException e) {
            LogUtil.getArthasLogger().warn("tt failed.", e);
            process.write(e.getMessage() + ", visit " + LogUtil.LOGGER_FILE + " for more detail\n");
        } finally {
            process.write(affect.toString()).write("\n");
            process.end();
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
                TableElement table = TimeTunnelTable.createDefaultTable();
                TimeTunnelTable.drawWatchTableHeader(table);
                TimeTunnelTable.drawWatchExpress(matchingTimeSegmentMap, table, watchExpress, isNeedExpand(), expand, sizeLimit);
                process.write(RenderUtil.render(table, process.width()));
            } else {
                // 单纯的列表格
                process.write(RenderUtil.render(TimeTunnelTable.drawTimeTunnelTable(matchingTimeSegmentMap), process.width()));
            }

            affect.rCnt(matchingTimeSegmentMap.size());
        } catch (ExpressException e) {
            LogUtil.getArthasLogger().warn("tt failed.", e);
            process.write(e.getMessage() + ", visit " + LogUtil.LOGGER_FILE + " for more detail\n");
        } finally {
            process.write(affect.toString()).write("\n");
            process.end();
        }
    }

    // 删除指定记录
    private void processDelete(CommandProcess process) {
        RowAffect affect = new RowAffect();
        if (timeFragmentMap.remove(index) != null) {
            affect.rCnt(1);
        }
        process.write(format("Time fragment[%d] successfully deleted.", index)).write("\n");
        process.write(affect.toString()).write("\n");
        process.end();
    }

    private void processDeleteAll(CommandProcess process) {
        int count = timeFragmentMap.size();
        RowAffect affect = new RowAffect(count);
        timeFragmentMap.clear();
        process.write("Time fragments are cleaned.\n");
        process.write(affect.toString()).write("\n");
        process.end();
    }

    private void processList(CommandProcess process) {
        RowAffect affect = new RowAffect();
        process.write(RenderUtil.render(TimeTunnelTable.drawTimeTunnelTable(timeFragmentMap), process.width()));
        affect.rCnt(timeFragmentMap.size());
        process.write(affect.toString()).write("\n");
        process.end();
    }

    // 重放指定记录
    private void processPlay(CommandProcess process) {
        RowAffect affect = new RowAffect();
        try {
            TimeFragment tf = timeFragmentMap.get(index);
            if (null == tf) {
                process.write(format("Time fragment[%d] does not exist.", index) + "\n");
                process.write(affect + "\n");
                process.end();
                return;
            }

            Advice advice = tf.getAdvice();
            String className = advice.getClazz().getName();
            String methodName = advice.getMethod().getName();
            String objectAddress = advice.getTarget() == null ? "NULL" : "0x" + toHexString(advice.getTarget().hashCode());



            ArthasMethod method = advice.getMethod();
            method.setAccessible(true);
            boolean accessible = advice.getMethod().isAccessible();
            for (int i = 0; i < getReplayTimes(); i++) {
//              wait for the next execution
                if (i > 0) {
                    try {
                        Thread.sleep(getReplayInterval());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                long beginTime = System.nanoTime();
                TableElement table = TimeTunnelTable.createDefaultTable();
                TimeTunnelTable.drawPlayHeader(className, methodName, objectAddress, index, table);
                TimeTunnelTable.drawParameters(advice, table, isNeedExpand(), expand);

                try {
                    Object returnObj = method.invoke(advice.getTarget(), advice.getParams());
                    double cost = (System.nanoTime() - beginTime) / 1000000.0;
                    TimeTunnelTable.drawPlayResult(table, returnObj, isNeedExpand(), expand, sizeLimit, cost);
                } catch (Throwable t) {
                    TimeTunnelTable.drawPlayException(table, t, isNeedExpand(), expand);
                }
                process.write(RenderUtil.render(table, process.width()))
                        .write(format("Time fragment[%d] successfully replayed.", index))
                        .write("\n");
                affect.rCnt(1);
                process.write(affect.toString()).write("\n");
            }
            method.setAccessible(accessible);

        } finally {
            process.end();
        }
    }
}
