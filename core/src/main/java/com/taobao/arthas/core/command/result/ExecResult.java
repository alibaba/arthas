package com.taobao.arthas.core.command.result;

import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Command execute result
 * @author gongdewei 2020-03-26
 */
public abstract class ExecResult {

    /**
     * Command type (name)
     * @return
     */
    public abstract String getType();

    /**
     * Write command execute result to tty / term
     * @param process
     */
    public final void writeToTty(CommandProcess process){
        this.write(process);
    }

    protected abstract void write(CommandProcess process);

    /**
     * write str and append a new line
     * @param process
     * @param str
     */
    protected void writeln(CommandProcess process, String str) {
        process.write(str).write("\n");
    }
}
