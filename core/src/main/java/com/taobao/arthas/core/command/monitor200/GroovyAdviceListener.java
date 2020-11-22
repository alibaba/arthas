package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.advisor.AdviceListenerAdapter;
import com.taobao.arthas.core.command.ScriptSupportCommand;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.advisor.Advice;
import com.taobao.arthas.core.advisor.ArthasMethod;

/**
 * Groovy support has been completed dropped in Arthas 3.0 because of severer memory leak.
 * @author beiwei30 on 01/12/2016.
 */
@Deprecated
public class GroovyAdviceListener extends AdviceListenerAdapter {
    private ScriptSupportCommand.ScriptListener scriptListener;
    private ScriptSupportCommand.Output output;

    public GroovyAdviceListener(ScriptSupportCommand.ScriptListener scriptListener, CommandProcess process) {
        this.scriptListener = scriptListener;
        this.output = new CommandProcessAdaptor(process);
    }

    @Override
    public void create() {
        scriptListener.create(output);
    }

    @Override
    public void destroy() {
        scriptListener.destroy(output);
    }

    @Override
    public void before(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args)
            throws Throwable {
        scriptListener.before(output, Advice.newForBefore(loader, clazz, method, target, args));
    }

    @Override
    public void afterReturning(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                               Object returnObject) throws Throwable {
        scriptListener.afterReturning(output, Advice.newForAfterRetuning(loader, clazz, method, target, args, returnObject));
    }

    @Override
    public void afterThrowing(ClassLoader loader, Class<?> clazz, ArthasMethod method, Object target, Object[] args,
                              Throwable throwable) throws Throwable {
        scriptListener.afterThrowing(output, Advice.newForAfterThrowing(loader, clazz, method, target, args, throwable));
    }

    private static class CommandProcessAdaptor implements ScriptSupportCommand.Output {
        private CommandProcess process;

        public CommandProcessAdaptor(CommandProcess process) {
            this.process = process;
        }

        @Override
        public ScriptSupportCommand.Output print(String string) {
            process.write(string);
            return this;
        }

        @Override
        public ScriptSupportCommand.Output println(String string) {
            process.write(string).write("\n");
            return this;
        }

        @Override
        public ScriptSupportCommand.Output finish() {
            process.end();
            return this;
        }
    }
}
