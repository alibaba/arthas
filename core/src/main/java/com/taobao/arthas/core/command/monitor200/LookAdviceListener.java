package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.advisor.AccessPoint;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.command.express.ExpressException;
import com.taobao.arthas.core.command.express.ExpressFactory;
import com.taobao.arthas.core.command.model.LookModel;
import com.taobao.arthas.core.command.model.ObjectVO;
import com.taobao.arthas.core.command.model.WatchModel;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.LogUtil;
import com.taobao.arthas.core.util.StringUtils;
import com.taobao.arthas.core.util.ThreadLocalWatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class LookAdviceListener extends AdviceListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(LookAdviceListener.class);
    private LookCommand command;
    private CommandProcess process;

    public LookAdviceListener(LookCommand command, CommandProcess process, boolean verbose) {
        this.command = command;
        this.process = process;
        super.setVerbose(verbose);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {

    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {

    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) {

    }

    @Override
    public void beforeLine(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, int line, Object[] vars, String[] varNames) throws Throwable {
        try {

            Map<String,Object> varMap = new HashMap<String,Object>(vars.length);
            for (int i = 0; i < vars.length; i++) {
                //不放入this
                if ("this".equals(varNames[i]))continue;
                varMap.put(varNames[i],vars[i]);
            }

            Advice advice = Advice.newForLooking(loader,clazz,method,target,args,varMap);
            boolean conditionResult = isConditionMet(command.getConditionExpress(), advice);
            if (this.isVerbose()) {
                process.write("Condition express: " + command.getConditionExpress() + " , result: " + conditionResult + "\n");
            }
            if (conditionResult) {
                Object value = getExpressionResult(command.getExpress(), advice);

                LookModel model = new LookModel();
                model.setTs(new Date());
                model.setValue(new ObjectVO(value, command.getExpand()));
                model.setSizeLimit(command.getSizeLimit());
                model.setClassName(advice.getClazz().getName());
                model.setMethodName(advice.getMethod().getName());
                model.setAccessPoint(AccessPoint.ACCESS_BEFORE_LINE.getKey() + ":" + line);

                process.appendResult(model);
                process.times().incrementAndGet();
                if (isLimitExceeded(command.getNumberOfLimit(), process.times().get())) {
                    abortProcess(process, command.getNumberOfLimit());
                }
            }
        } catch (Throwable e) {
            logger.warn("look failed.", e);
            process.end(-1, "look failed, condition is: " + command.getConditionExpress() + ", express is: "
                    + command.getExpress() + ", " + e.getMessage() + ", visit " + LogUtil.loggingFile()
                    + " for more details.");
        }
    }

    boolean isConditionMet(String conditionExpress, Advice advice) throws ExpressException {
        return StringUtils.isEmpty(conditionExpress)
                || ExpressFactory.threadLocalExpress(advice).is(conditionExpress);

    }

    Object getExpressionResult(String express, Advice advice) throws ExpressException {
        return ExpressFactory.threadLocalExpress(advice).get(express);
    }

    public LookCommand getCommand() {
        return command;
    }

    public void setCommand(LookCommand command) {
        this.command = command;
    }
}
