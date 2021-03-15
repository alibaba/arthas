package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.handlers.command.CommandInterruptHandler;

import java.util.Timer;

/**
 * @author wangdong.
 */
public class GcInfoInterruptHandler extends CommandInterruptHandler {

    private volatile Timer timer;

    public GcInfoInterruptHandler(CommandProcess process, Timer timer) {
        super(process);
        this.timer = timer;
    }

    @Override
    public void handle(Void event) {
        timer.cancel();
        super.handle(event);
    }
}
