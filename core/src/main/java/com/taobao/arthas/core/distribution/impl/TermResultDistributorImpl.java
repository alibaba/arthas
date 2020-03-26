package com.taobao.arthas.core.distribution.impl;

import com.taobao.arthas.core.command.result.ExecResult;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Term/Tty Result Distributor
 * @author gongdewei 2020-03-26
 */
public class TermResultDistributorImpl implements ResultDistributor {

    private final CommandProcess commandProcess;

    public TermResultDistributorImpl(CommandProcess commandProcess) {
        this.commandProcess = commandProcess;
    }

    @Override
    public void appendResult(ExecResult result) {
        result.writeToTty(commandProcess);
    }

}
