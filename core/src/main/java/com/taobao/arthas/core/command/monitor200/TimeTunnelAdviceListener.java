package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.ReflectAdviceListenerAdapter;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.ThreadLocalWatch;
import com.taobao.text.ui.TableElement;
import com.taobao.text.util.RenderUtil;

import java.util.Date;

import static com.taobao.arthas.core.command.monitor200.TimeTunnelTable.createTable;
import static com.taobao.arthas.core.command.monitor200.TimeTunnelTable.fillTableHeader;
import static com.taobao.arthas.core.command.monitor200.TimeTunnelTable.fillTableRow;

/**
 * @author beiwei30 on 30/11/2016.
 */
public class TimeTunnelAdviceListener extends ReflectAdviceListenerAdapter {

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
        threadLocalWatch.start();
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        afterFinishing(Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject));
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {
        afterFinishing(Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    private void afterFinishing(Advice advice) {
        double cost = threadLocalWatch.costInMillis();
        TimeFragment timeTunnel = new TimeFragment(advice, new Date(), cost);

        // reset the timestamp
        threadLocalWatch.clear();

        boolean match = false;
        try {
            match = isConditionMet(command.getConditionExpress(), advice, cost);
        } catch (ExpressException e) {
            LogUtil.getArthasLogger().warn("tt failed.", e);
            process.write("tt failed, condition is: " + command.getConditionExpress() + ", " + e.getMessage()
                          + ", visit " + LogUtil.LOGGER_FILE + " for more details.\n");
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
}
