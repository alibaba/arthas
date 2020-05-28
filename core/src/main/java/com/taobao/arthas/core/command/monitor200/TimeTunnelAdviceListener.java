package com.taobao.arthas.core.command.monitor200;

import static com.taobao.arthas.core.command.monitor200.TimeTunnelTable.createTable;
import static com.taobao.arthas.core.command.monitor200.TimeTunnelTable.fillTableHeader;
import static com.taobao.arthas.core.command.monitor200.TimeTunnelTable.fillTableRow;

import java.util.Date;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

/**
 * @author beiwei30 on 30/11/2016.
 * @author hengyunabc 2020-05-20
 */
public class TimeTunnelAdviceListener extends AdviceListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(TimeTunnelAdviceListener.class);
    private final ThreadLocal<ObjectStack> argsRef = new ThreadLocal<ObjectStack>() {
        @Override
        protected ObjectStack initialValue() {
            return new ObjectStack(512);
        }
    };

    private TimeTunnelCommand command;
    private CommandProcess process;

    // 第一次启动标记
    private volatile boolean isFirst = true;

    // 方法执行时间戳
    private final ThreadLocalWatch threadLocalWatch = new ThreadLocalWatch();

    public TimeTunnelAdviceListener(TimeTunnelCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        argsRef.get().push(args);
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        //取出入参时的 args，因为在函数执行过程中 args可能被修改
        args = (Object[]) argsRef.get().pop();
        afterFinishing(Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject));
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        //取出入参时的 args，因为在函数执行过程中 args可能被修改
        args = (Object[]) argsRef.get().pop();
        afterFinishing(Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    private void afterFinishing(Advice advice) {
        double cost = threadLocalWatch.costInMillis();
        TimeFragment timeTunnel = new TimeFragment(advice, new Date(), cost);

        boolean match = false;
        try {
            match = isConditionMet(command.getConditionExpress(), advice, cost);
        } catch (ExpressException e) {
            logger.warn("tt failed.", e);
            process.write("tt failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                          + ", visit " + LogUtil.loggingFile() + " for more details.\n");
            process.end();
        }

        if (!match) {
            return;
        }

        int index = command.putTimeTunnel(timeTunnel);
        TableElement table = createTable();

        if (isFirst) {
            isFirst = false;

            // 填充表格头部
            fillTableHeader(table);
        }

        // 填充表格内容
        fillTableRow(table, index, timeTunnel);

        // TODO: concurrency issues for process.write
        process.write(RenderUtil.render(table, process.width()));
        process.times().incrementAndGet();
        if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
            abortProcess(process, command.getNumberOfLimit());
        }
    }

    /**
     * 
     * <pre>
     * 一个特殊的stack，为了追求效率，避免扩容。
     * 因为这个stack的push/pop 并不一定成对调用，比如可能push执行了，但是后面的流程被中断了，pop没有被执行。
     * 如果不固定大小，一直增长的话，极端情况下可能应用有内存问题。
     * 如果到达容量，pos会重置，循环存储数据。所以使用这个Stack如果在极端情况下统计的数据会不准确，只用于monitor/watch等命令的计时。
     * 
     * </pre>
     * 
     * @author hengyunabc 2020-05-20
     *
     */
    static class ObjectStack {
        private Object[] array;
        private int pos = 0;
        private int cap;

        public ObjectStack(int maxSize) {
            array = new Object[maxSize];
            cap = array.length;
        }

        public int size() {
            return pos;
        }

        public void push(Object value) {
            if (pos < cap) {
                array[pos++] = value;
            } else {
                // if array is full, reset pos
                pos = 0;
                array[pos++] = value;
            }
        }

        public Object pop() {
            if (pos > 0) {
                pos--;
                Object object = array[pos];
                array[pos] = null;
                return object;
            } else {
                pos = cap;
                pos--;
                Object object = array[pos];
                array[pos] = null;
                return object;
            }
        }
    }
}
