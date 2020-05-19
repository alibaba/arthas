package com.taobao.arthas.core.command.monitor200;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.alibaba.fastjson.JSONObject;
import com.taobao.arthas.core.advisor.ArthasMethod;
import com.taobao.arthas.core.advisor.ReflectAdviceListenerAdapter;
import com.taobao.arthas.core.advisor.VariableStore;
import com.taobao.arthas.core.shell.command.CommandProcess;

public class WatchVariableListener extends ReflectAdviceListenerAdapter implements VariableStore {

    private static final String VAR_IN_PARAMS = "in_params";
    private static final String VAR_EXCEPTION = "exception";
    private static final String VAR_RETURN = "return";

    private static final Logger logger = LoggerFactory.getLogger(WatchAdviceListener.class);
    private WatchVariableCommand command;
    private CommandProcess process;

    private ThreadLocal<JSONObject> variableJSON = new ThreadLocal<JSONObject>();

    public WatchVariableListener(WatchVariableCommand command, CommandProcess process) {
        this.command = command;
        this.process = process;
    }


    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args) throws Throwable {
        handle(0,VAR_IN_PARAMS,args);
    }


    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Object returnObject) throws Throwable {

        handle(Integer.MAX_VALUE,VAR_RETURN,returnObject);

    }


    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args, Throwable throwable) throws Throwable {

        handle(Integer.MAX_VALUE,VAR_EXCEPTION,throwable.getStackTrace());
    }

    @Override
    public void variableStored(int line, String varName, Object varValue) {

        handle(line,varName,varValue);
    }

    public void handle(int line, String varName, Object varValue){
        if (command.shouldFinishBefore(line)){
            finish();
        }
        if (command.variableListContains(varName)){
            saveVariable(varName,varValue);
        }
        if (command.shouldFinishAfter(line)){
            finish();
        }
    }


    private void saveVariable(String key, Object args) {
        if (variableJSON.get() == null){
            variableJSON.set(new JSONObject());
        }
        variableJSON.get().put(key, args);
    }

    private void finish() {
        try {
            process.write(variableJSON.get() == null ? "" : variableJSON.get().toJSONString());
        } catch (Exception e) {
            logger.warn("watch failed.", e);
            process.write("watch failed, condition is: " + e.getMessage());
        } finally {
            process.write("\n");
            variableJSON.remove();
            process.end();
        }
    }
}
