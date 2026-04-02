package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.command.Constants;
import com.taobao.arthas.core.command.model.MBeanAttributeVO;
import com.taobao.arthas.core.command.model.MBeanModel;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.command.AnnotatedCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;
import com.taobao.arthas.core.shell.handlers.shell.QExitHandler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.TokenUtils;
import com.taobao.arthas.core.util.matcher.Matcher;
import com.taobao.arthas.core.util.matcher.RegexMatcher;
import com.taobao.arthas.core.util.matcher.WildcardMatcher;
import com.taobao.middleware.cli.annotations.Argument;
import com.taobao.middleware.cli.annotations.Description;
import com.taobao.middleware.cli.annotations.Name;
import com.taobao.middleware.cli.annotations.Option;
import com.taobao.middleware.cli.annotations.Summary;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

/**
 * MBean信息查看命令
 * 用于查看和监控JVM的MBean（Managed Bean）信息
 *
 * Date: 2019/4/18
 *
 * @author xuzhiyi
 */
@Name("mbean")
@Summary("Display the mbean information")
@Description("\nExamples:\n" +
        "  mbean\n" +
        "  mbean -m java.lang:type=Threading\n" +
        "  mbean java.lang:type=Threading\n" +
        "  mbean java.lang:type=Threading *Count\n" +
        "  mbean java.lang:type=MemoryPool,name=PS\\ Old\\ Gen\n" +
        "  mbean java.lang:type=MemoryPool,name=*\n" +
        "  mbean java.lang:type=MemoryPool,name=* Usage\n" +
        "  mbean -E java.lang:type=Threading PeakThreadCount|ThreadCount|DaemonThreadCount\n" +
        "  mbean -i 1000 java.lang:type=Threading *Count\n" +
        Constants.WIKI + Constants.WIKI_HOME + "mbean")
public class MBeanCommand extends AnnotatedCommand {

    private static final Logger logger = LoggerFactory.getLogger(MBeanCommand.class);

    /** MBean的ObjectName模式 */
    private String name;
    /** 属性名称模式 */
    private String attribute;
    /** 是否使用正则表达式匹配 */
    private boolean isRegEx = false;
    /** 监控间隔（毫秒） */
    private long interval = 0;
    /** 是否显示元数据 */
    private boolean metaData;
    /** 执行次数限制 */
    private int numOfExecutions = 100;
    /** 定时器，用于周期性执行监控 */
    private Timer timer;
    /** 当前已执行的次数 */
    private long count = 0;

    /**
     * 设置MBean的ObjectName模式
     * @param name ObjectName模式，格式为：domain: key-property-list
     */
    @Argument(argName = "name-pattern", index = 0, required = false)
    @Description("ObjectName pattern, see javax.management.ObjectName for more detail. \n" +
            "It looks like this: \n" +
            "  domain: key-property-list\n" +
            "For example: \n" +
            "  java.lang:name=G1 Old Gen,type=MemoryPool\n" +
            "  java.lang:name=*,type=MemoryPool")
    public void setNamePattern(String name) {
        this.name = name;
    }

    /**
     * 设置属性名称模式
     * @param attribute 属性名称模式
     */
    @Argument(argName = "attribute-pattern", index = 1, required = false)
    @Description("Attribute name pattern.")
    public void setAttributePattern(String attribute) {
        this.attribute = attribute;
    }

    /**
     * 设置监控间隔时间
     * @param interval 两次执行之间的间隔时间（毫秒）
     */
    @Option(shortName = "i", longName = "interval")
    @Description("The interval (in ms) between two executions.")
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * 设置是否使用正则表达式匹配
     * @param regEx true表示使用正则表达式，false表示使用通配符（默认）
     */
    @Option(shortName = "E", longName = "regex", flag = true)
    @Description("Enable regular expression to match attribute name (wildcard matching by default).")
    public void setRegEx(boolean regEx) {
        isRegEx = regEx;
    }

    /**
     * 设置是否显示元数据
     * @param metaData true表示显示MBean的元数据信息
     */
    @Option(shortName = "m", longName = "metadata", flag = true)
    @Description("Show metadata of mbean.")
    public void setMetaData(boolean metaData) {
        this.metaData = metaData;
    }

    /**
     * 设置命令执行次数限制
     * @param numOfExecutions 命令执行的总次数
     */
    @Option(shortName = "n", longName = "number-of-execution")
    @Description("The number of times this command will be executed.")
    public void setNumOfExecutions(int numOfExecutions) {
        this.numOfExecutions = numOfExecutions;
    }

    /**
     * 获取MBean名称
     * @return MBean名称
     */
    public String getName() {
        return name;
    }

    /**
     * 是否使用正则表达式
     * @return true表示使用正则表达式
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    /**
     * 是否显示元数据
     * @return true表示显示元数据
     */
    public boolean isMetaData() {
        return metaData;
    }

    /**
     * 获取监控间隔时间
     * @return 间隔时间（毫秒）
     */
    public long getInterval() {
        return interval;
    }

    /**
     * 获取执行次数限制
     * @return 执行次数限制
     */
    public int getNumOfExecutions() {
        return numOfExecutions;
    }

    /**
     * 处理命令执行
     * 根据参数判断执行哪种操作：列出MBean、显示元数据或监控属性
     * @param process 命令处理进程
     */
    @Override
    public void process(CommandProcess process) {
        //每个分支调用process.end()结束执行
        if (StringUtils.isEmpty(getName())) {
            // 如果没有指定MBean名称，则列出所有MBean
            listMBean(process);
        } else if (isMetaData()) {
            // 如果指定了显示元数据，则列出MBean的元数据信息
            listMetaData(process);
        } else {
            // 否则监控MBean的属性值
            listAttribute(process);
        }
    }

    /**
     * 列出所有匹配的MBean名称
     * @param process 命令处理进程
     */
    private void listMBean(CommandProcess process) {
        // 查询匹配的MBean对象名称
        Set<ObjectName> objectNames = queryObjectNames();
        List<String> mbeanNames = new ArrayList<String>(objectNames.size());
        for (ObjectName objectName : objectNames) {
            mbeanNames.add(objectName.toString());
        }
        // 将结果添加到进程并结束
        process.appendResult(new MBeanModel(mbeanNames));
        process.end();
    }

    /**
     * 监控MBean属性值
     * 创建定时任务，周期性地获取并显示MBean属性值
     * @param process 命令处理进程
     */
    private void listAttribute(final CommandProcess process) {
        Session session = process.session();
        // 创建守护线程定时器
        timer = new Timer("Timer-for-arthas-mbean-" + session.getSessionId(), true);

        // 设置中断处理器，支持Ctrl-C中断命令
        process.interruptHandler(new MBeanInterruptHandler(process, timer));

        // 通过handle回调，在suspend和end时停止timer，resume时重启timer
        Handler<Void> stopHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                stop();
            }
        };

        Handler<Void> restartHandler = new Handler<Void>() {
            @Override
            public void handle(Void event) {
                restart(process);
            }
        };
        process.suspendHandler(stopHandler);
        process.resumeHandler(restartHandler);
        process.endHandler(stopHandler);
        // 支持q退出命令
        process.stdinHandler(new QExitHandler(process));

        // 启动定时器
        if (getInterval() > 0) {
            // 如果设置了间隔时间，则周期性执行
            timer.scheduleAtFixedRate(new MBeanTimerTask(process), 0, getInterval());
        } else {
            // 否则只执行一次
            timer.schedule(new MBeanTimerTask(process), 0);
        }

        //异步执行，这里不能调用process.end()，在timer task中结束命令执行
    }

    /**
     * 停止定时器
     */
    public synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    /**
     * 重启定时器
     * @param process 命令处理进程
     */
    public synchronized void restart(CommandProcess process) {
        if (timer == null) {
            Session session = process.session();
            timer = new Timer("Timer-for-arthas-mbean-" + session.getSessionId(), true);
            timer.scheduleAtFixedRate(new MBeanTimerTask(process), 0, getInterval());
        }
    }

    /**
     * 列出MBean的元数据信息
     * @param process 命令处理进程
     */
    private void listMetaData(CommandProcess process) {
        Set<ObjectName> objectNames = queryObjectNames();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            MBeanModel mbeanModel = new MBeanModel();
            Map<String, MBeanInfo> mbeanMetaData = new LinkedHashMap<String, MBeanInfo>();
            mbeanModel.setMbeanMetadata(mbeanMetaData);
            // 获取每个MBean的元数据信息
            for (ObjectName objectName : objectNames) {
                MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(objectName);
                mbeanMetaData.put(objectName.toString(), mBeanInfo);
            }
            process.appendResult(mbeanModel);
            process.end();
        } catch (Throwable e) {
            logger.warn("listMetaData error", e);
            process.end(1, "list mbean metadata error");
        }
    }

    /**
     * 自动完成功能
     * 根据用户输入提供MBean名称和属性名的自动补全建议
     * @param completion 补全上下文
     */
    @Override
    public void complete(Completion completion) {
        int argumentIndex = CompletionUtils.detectArgumentIndex(completion);
        if (argumentIndex == 1) {
            // 第一个参数：补全MBean名称
            if (!completeBeanName(completion)) {
                super.complete(completion);
            }
            return;
        } else if (argumentIndex == 2) {
            // 第二个参数：补全属性名称
            if (!completeAttributeName(completion)) {
                super.complete(completion);
            }
            return;
        }
        super.complete(completion);
    }

    /**
     * 补全MBean名称
     * 根据用户已输入的部分，提供匹配的MBean名称建议
     * @param completion 补全上下文
     * @return true表示成功补全，false表示需要使用默认补全
     */
    private boolean completeBeanName(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String lastToken = TokenUtils.getLast(tokens).value();

        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        // 如果是选项参数，不进行补全
        if (lastToken.startsWith("-") || lastToken.startsWith("--")) {
            return false;
        }

        // 查询所有匹配的MBean名称
        Set<ObjectName> objectNames = queryObjectNames();
        Set<String> names = new HashSet<String>();

        if (objectNames == null) {
            return false;
        }
        // 遍历所有MBean，找出匹配的名称
        for (ObjectName objectName : objectNames) {
            String name = objectName.toString();
            if (name.startsWith(lastToken)) {
                // 尝试在'.'或':'处截断，提供分级补全
                int index = name.indexOf('.', lastToken.length());
                if (index > 0) {
                    names.add(name.substring(0, index + 1));
                    continue;
                }
                index = name.indexOf(':', lastToken.length());
                if (index > 0) {
                    names.add(name.substring(0, index + 1));
                    continue;
                }
                names.add(name);
            }
        }
        // 如果只有一个补全项且以'.'或':'结尾，直接追加而不显示列表
        String next = names.iterator().next();
        if (names.size() == 1 && (next.endsWith(".") || next.endsWith(":"))) {
            completion.complete(next.substring(lastToken.length()), false);
            return true;
        } else {
            return CompletionUtils.complete(completion, names);
        }
    }

    /**
     * 补全属性名称
     * 根据已指定的MBean名称，提供该MBean的属性名称补全建议
     * @param completion 补全上下文
     * @return true表示成功补全，false表示需要使用默认补全
     */
    private boolean completeAttributeName(Completion completion) {
        List<CliToken> tokens = completion.lineTokens();
        String lastToken = TokenUtils.getLast(tokens).value();

        if (StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }

        // 获取MBean服务器
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        // 获取用户输入的MBean名称
        String beanName = TokenUtils.retrievePreviousArg(tokens, lastToken);
        Set<ObjectName> objectNames = null;
        try {
            // 查询匹配的MBean
            objectNames = platformMBeanServer.queryNames(new ObjectName(beanName), null);
        } catch (MalformedObjectNameException e) {
            logger.warn("queryNames error", e);
        }
        if (objectNames == null || objectNames.size() == 0) {
            return false;
        }
        try {
            // 获取MBean的信息
            MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(objectNames.iterator().next());
            List<String> attributeNames = new ArrayList<String>();
            MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
            // 遍历所有属性，找出匹配的属性名
            for (MBeanAttributeInfo attribute : attributes) {
                if (StringUtils.isBlank(lastToken)) {
                    attributeNames.add(attribute.getName());
                } else if (attribute.getName().startsWith(lastToken)) {
                    attributeNames.add(attribute.getName());
                }
            }
            return CompletionUtils.complete(completion, attributeNames);
        } catch (Throwable e) {
            logger.warn("getMBeanInfo error", e);
        }
        return false;
    }

    /**
     * 查询匹配的MBean对象名称
     * @return 匹配的ObjectName集合
     */
    private Set<ObjectName> queryObjectNames() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objectNames = new HashSet<ObjectName>();
        try {
            // 如果没有指定名称，则查询所有MBean
            if (StringUtils.isEmpty(name)) {
                name = "*:*";
            }
            objectNames = platformMBeanServer.queryNames(new ObjectName(name), null);
        } catch (MalformedObjectNameException e) {
            logger.warn("queryObjectNames error", e);
        }
        return objectNames;
    }

    /**
     * 获取属性名称匹配器
     * 根据配置返回正则表达式匹配器或通配符匹配器
     * @return 属性名称匹配器
     */
    private Matcher<String> getAttributeMatcher() {
        if (StringUtils.isEmpty(attribute)) {
            // 如果没有指定属性模式，则匹配所有属性
            attribute = isRegEx ? ".*" : "*";
        }
        return isRegEx ? new RegexMatcher(attribute) : new WildcardMatcher(attribute);
    }


    /**
     * MBean命令中断处理器
     * 处理用户中断命令（Ctrl-C）的情况
     */
    public static class MBeanInterruptHandler extends CommandInterruptHandler {

        /** 需要取消的定时器 */
        private volatile Timer timer;

        /**
         * 构造函数
         * @param process 命令处理进程
         * @param timer 需要管理的定时器
         */
        public MBeanInterruptHandler(CommandProcess process, Timer timer) {
            super(process);
            this.timer = timer;
        }

        /**
         * 处理中断事件
         * 取消定时器并调用父类的中断处理
         * @param event 事件对象
         */
        @Override
        public void handle(Void event) {
            timer.cancel();
            super.handle(event);
        }
    }

    /**
     * MBean定时任务
     * 周期性地获取MBean属性值并输出
     */
    private class MBeanTimerTask extends TimerTask {

        /** 命令处理进程 */
        private CommandProcess process;

        /**
         * 构造函数
         * @param process 命令处理进程
         */
        public MBeanTimerTask(CommandProcess process) {
            this.process = process;
        }

        /**
         * 定时任务执行方法
         * 获取MBean属性值并输出结果
         */
        @Override
        public void run() {
            // 检查是否超过执行次数限制
            if (count >= getNumOfExecutions()) {
                // 停止定时器
                timer.cancel();
                timer.purge();
                process.end(-1, "Process ends after " + getNumOfExecutions() + " time(s).");
                return;
            }

            try {
                // 创建结果模型
                MBeanModel mBeanModel = new MBeanModel();
                Map<String, List<MBeanAttributeVO>> mbeanAttributeMap = new LinkedHashMap<String, List<MBeanAttributeVO>>();
                mBeanModel.setMbeanAttribute(mbeanAttributeMap);

                // 获取MBean服务器
                MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
                Set<ObjectName> objectNames = queryObjectNames();
                // 遍历所有匹配的MBean
                for (ObjectName objectName : objectNames) {
                    List<MBeanAttributeVO> attributeVOs = null;
                    // 获取MBean信息
                    MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(objectName);
                    MBeanAttributeInfo[] attributes = mBeanInfo.getAttributes();
                    // 遍历所有属性
                    for (MBeanAttributeInfo attribute : attributes) {
                        String attributeName = attribute.getName();
                        // 检查属性名是否匹配
                        if (!getAttributeMatcher().matching(attributeName)) {
                            continue;
                        }

                        // 创建属性值对象列表
                        if (attributeVOs == null) {
                            attributeVOs = new ArrayList<MBeanAttributeVO>();
                            mbeanAttributeMap.put(objectName.toString(), attributeVOs);
                        }

                        // 检查属性是否可读
                        if (!attribute.isReadable()) {
                            attributeVOs.add(new MBeanAttributeVO(attributeName, null, "Unavailable"));
                        } else {
                            try {
                                // 获取属性值
                                Object attributeObj = platformMBeanServer.getAttribute(objectName, attributeName);
                                attributeVOs.add(createMBeanAttributeVO(attributeName, attributeObj));
                            } catch (Throwable e) {
                                logger.error("read mbean attribute failed: objectName={}, attributeName={}", objectName, attributeName, e);
                                String errorStr;
                                Throwable cause = e.getCause();
                                // 根据异常类型设置错误信息
                                if (cause instanceof UnsupportedOperationException) {
                                    errorStr = "Unsupported";
                                } else {
                                    errorStr = "Failure";
                                }
                                attributeVOs.add(new MBeanAttributeVO(attributeName, null, errorStr));
                            }
                        }
                    }
                }
                // 将结果添加到进程
                process.appendResult(mBeanModel);
            } catch (Throwable e) {
                logger.warn("read mbean error", e);
                stop();
                process.end(1, "read mbean error.");
                return;
            }

            // 增加执行次数
            count++;
            process.times().incrementAndGet();
            // 如果没有设置间隔时间，则只执行一次
            if (getInterval() <= 0) {
                stop();
                process.end();
            }
        }
    }

    /**
     * 创建MBean属性值对象
     * 将原始属性值转换为适合显示的格式
     * @param attributeName 属性名称
     * @param originAttrValue 原始属性值
     * @return MBean属性值对象
     */
    private MBeanAttributeVO createMBeanAttributeVO(String attributeName, Object originAttrValue) {
        Object attrValue = convertAttrValue(attributeName, originAttrValue);

        return new MBeanAttributeVO(attributeName, attrValue);
    }

    /**
     * 转换属性值为适合显示的格式
     * 处理各种类型的MBean属性值，包括复合数据和表格数据
     * @param attributeName 属性名称
     * @param originAttrValue 原始属性值
     * @return 转换后的属性值
     */
    private Object convertAttrValue(String attributeName, Object originAttrValue) {
        Object attrValue = originAttrValue;

        try {
            // 处理ObjectName类型，转换为字符串
            if (originAttrValue instanceof ObjectName) {
                attrValue = String.valueOf(originAttrValue);
            } else if (attrValue instanceof CompositeData) {
                // 处理复合数据类型，例如：mbean java.lang:type=MemoryPool,name=*
                CompositeData compositeData = (CompositeData) attrValue;
                attrValue = convertCompositeData(attributeName, compositeData);
            } else if (attrValue instanceof CompositeData[]) {
                // 处理复合数据数组类型，例如：mbean com.sun.management:type=HotSpotDiagnostic
                CompositeData[] compositeDataArray = (CompositeData[]) attrValue;
                List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>(compositeDataArray.length);
                for (CompositeData compositeData : compositeDataArray) {
                    dataList.add(convertCompositeData(attributeName, compositeData));
                }
                attrValue = dataList;
            } else if (attrValue instanceof TabularData) {
                // 处理表格数据类型，例如：mbean java.lang:type=GarbageCollector,name=*
                TabularData tabularData = (TabularData) attrValue;
                Collection<CompositeData> compositeDataList = (Collection<CompositeData>) tabularData.values();
                List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>(compositeDataList.size());
                for (CompositeData compositeData : compositeDataList) {
                    dataList.add(convertCompositeData(attributeName, compositeData));
                }
                attrValue = dataList;
            }
        } catch (Throwable e) {
            logger.error("convert mbean attribute error, attribute: {}={}", attributeName, originAttrValue, e);
            // 转换失败时，返回字符串形式
            attrValue = String.valueOf(originAttrValue);
        }
        return attrValue;
    }

    /**
     * 转换复合数据为Map
     * 将JMX的CompositeData对象转换为LinkedHashMap
     * @param attributeName 属性名称
     * @param compositeData 复合数据对象
     * @return 转换后的Map
     */
    private Map<String, Object> convertCompositeData(String attributeName, CompositeData compositeData) {
        // 获取所有键
        Set<String> keySet = compositeData.getCompositeType().keySet();
        String[] keys = keySet.toArray(new String[0]);
        // 获取所有值
        Object[] values = compositeData.getAll(keys);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        // 递归转换每个值
        for (int i = 0; i < keys.length; i++) {
            data.put(keys[i], convertAttrValue(attributeName + "." + keys[i], values[i]));
        }
        return data;
    }
}