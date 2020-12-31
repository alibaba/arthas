package com.taobao.arthas.core.distribution.impl;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.view.ResultView;
import com.taobao.arthas.core.command.view.ResultViewResolver;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Term/Tty Result Distributor
 *
 * @author gongdewei 2020-03-26
 */
public class TermResultDistributorImpl implements ResultDistributor {

    private final CommandProcess commandProcess;
    private final ResultViewResolver resultViewResolver;

    public TermResultDistributorImpl(CommandProcess commandProcess, ResultViewResolver resultViewResolver) {
        this.commandProcess = commandProcess;
        this.resultViewResolver = resultViewResolver;
    }

    @Override
    public void appendResult(ResultModel model) {
        ResultView resultView = resultViewResolver.getResultView(model);
        if (resultView != null) {
            resultView.draw(commandProcess, model);
        }
    }

    @Override
    public void close() {
    }

}
