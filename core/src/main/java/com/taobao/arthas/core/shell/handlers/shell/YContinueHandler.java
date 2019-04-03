package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * Date: 2019/4/2
 *
 * @author xuzhiyi
 */
public class YContinueHandler implements Handler<String> {

    private final CommandProcess process;

    public YContinueHandler(CommandProcess process) {
        this.process = process;
    }

    @Override
    public void handle(String event) {
        process.write(event + "\n");
        if ("y".equalsIgnoreCase(event)) {
            process.notice();
        } else {
            process.end();
        }
    }
}
