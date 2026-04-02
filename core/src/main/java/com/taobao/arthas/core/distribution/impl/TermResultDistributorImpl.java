package com.taobao.arthas.core.distribution.impl;

import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.command.view.ResultView;
import com.taobao.arthas.core.command.view.ResultViewResolver;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.shell.command.CommandProcess;

/**
 * Term/Tty 结果分发器实现类
 *
 * 负责将命令执行结果直接输出到终端（TTY）
 * 使用 ResultView 来渲染不同类型的 ResultModel
 * 适用于命令行终端场景，不支持多个消费者
 *
 * @author gongdewei 2020-03-26
 */
public class TermResultDistributorImpl implements ResultDistributor {

    // 命令进程对象，用于获取输出流
    private final CommandProcess commandProcess;

    // 结果视图解析器，用于将 ResultModel 转换为 ResultView
    private final ResultViewResolver resultViewResolver;

    // 输出锁，保证线程安全的输出
    private final Object outputLock = new Object();

    /**
     * 构造函数
     *
     * 创建终端结果分发器
     *
     * @param commandProcess 命令进程对象
     * @param resultViewResolver 结果视图解析器
     */
    public TermResultDistributorImpl(CommandProcess commandProcess, ResultViewResolver resultViewResolver) {
        this.commandProcess = commandProcess;
        this.resultViewResolver = resultViewResolver;
    }

    /**
     * 添加结果并输出到终端
     *
     * 将结果模型解析为对应的视图，然后渲染到终端
     * 使用同步锁保证输出的线程安全
     *
     * @param model 要输出的结果模型
     */
    @Override
    public void appendResult(ResultModel model) {
        // 解析获取对应的结果视图
        ResultView resultView = resultViewResolver.getResultView(model);
        if (resultView != null) {
            // 使用锁保证线程安全
            synchronized (outputLock) {
                // 将结果渲染到终端
                resultView.draw(commandProcess, model);
            }
        }
    }

    /**
     * 关闭分发器
     *
     * 终端分发器不需要清理操作
     */
    @Override
    public void close() {
    }

}
