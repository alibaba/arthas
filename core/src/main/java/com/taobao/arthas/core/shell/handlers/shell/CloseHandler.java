package com.taobao.arthas.core.shell.handlers.shell;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;

/**
 * 关闭处理器
 *
 * 该类实现了Handler接口，用于处理Shell的关闭事件。
 * 当接收到关闭信号时，该处理器负责：
 * 1. 关闭作业控制器（JobController），停止所有正在运行的后台任务
 * 2. 通知Shell的关闭完成处理器（FutureHandler），触发后续的清理操作
 *
 * 这是Arthas Shell生命周期管理的重要组成部分，确保资源能够被正确释放。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class CloseHandler implements Handler<Void> {

    /**
     * Shell实现实例
     * 该属性持有对ShellImpl的引用，用于访问Shell的作业控制器和关闭处理器
     */
    private ShellImpl shell;

    /**
     * 构造函数
     *
     * @param shell Shell实现实例，不能为null
     *              该参数会被保存到成员变量中，供handle方法使用
     */
    public CloseHandler(ShellImpl shell) {
        this.shell = shell;
    }

    /**
     * 处理关闭事件
     *
     * 该方法是Handler接口的实现，当接收到关闭信号时会调用该方法。
     * 方法执行以下操作：
     * 1. 获取Shell的作业控制器（JobController）
     * 2. 调用作业控制器的close方法，停止所有正在运行的后台任务
     * 3. 传入Shell的关闭完成处理器（FutureHandler），当关闭完成后会被回调
     *
     * @param event 关闭事件，该参数为Void类型，实际上不包含任何数据
     */
    @Override
    public void handle(Void event) {
        shell.jobController().close(shell.closedFutureHandler());
    }
}
