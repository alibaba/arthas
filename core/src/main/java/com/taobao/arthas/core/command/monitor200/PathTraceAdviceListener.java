package com.taobao.arthas.core.command.monitor200;

import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * @author ralf0131 2017-01-05 13:59.
 */
public class PathTraceAdviceListener extends AbstractTraceAdviceListener {

    public PathTraceAdviceListener(TraceCommand command, CommandProcess process) {
        super(command, process);
    }
}
