package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;

/**
 * 会话关闭处理器
 *
 * 当某个Shell会话关闭时，该处理器负责从Shell服务器中移除该会话。
 * 这确保了服务器能够正确跟踪当前活动的会话，并及时清理已关闭会话的相关资源。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class SessionClosedHandler implements Handler<Future<Void>> {
    /**
     * Shell服务器实例
     * 用于管理和维护所有活动的Shell会话
     */
    private ShellServerImpl shellServer;

    /**
     * 被关闭的Shell会话实例
     * 使用final修饰，确保在处理过程中引用不会被修改
     */
    private final ShellImpl session;

    /**
     * 构造函数
     *
     * @param shellServer Shell服务器实例，用于管理会话
     * @param session 被关闭的Shell会话实例
     */
    public SessionClosedHandler(ShellServerImpl shellServer, ShellImpl session) {
        this.shellServer = shellServer;
        this.session = session;
    }

    /**
     * 处理会话关闭事件
     *
     * 当会话关闭时调用此方法，从Shell服务器中移除该会话。
     * 这确保了服务器的会话列表保持最新状态。
     *
     * @param ar 表示会话关闭结果的异步对象
     */
    @Override
    public void handle(Future<Void> ar) {
        // 从Shell服务器中移除已关闭的会话
        shellServer.removeSession(session);
    }
}
