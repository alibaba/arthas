package com.taobao.arthas.core.shell.handlers.server;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellServerImpl;
import com.taobao.arthas.core.shell.term.Term;

/**
 * Term服务器终端处理器
 *
 * 该类实现了Handler接口，用于处理Term（终端）相关的事件。
 * 当有新的Term连接到ShellServer时，该处理器负责将Term交给ShellServer进行处理。
 * 这是Arthas终端服务的重要组成部分，负责建立终端与服务器之间的连接。
 *
 * @author beiwei30 on 23/11/2016.
 */
public class TermServerTermHandler implements Handler<Term> {

    /**
     * Shell服务器实现实例
     * 该属性持有对ShellServerImpl的引用，用于将接收到的Term对象转发给服务器进行处理
     */
    private ShellServerImpl shellServer;

    /**
     * 构造函数
     *
     * @param shellServer Shell服务器实现实例，不能为null
     *                    该参数会被保存到成员变量中，供handle方法使用
     */
    public TermServerTermHandler(ShellServerImpl shellServer) {
        this.shellServer = shellServer;
    }

    /**
     * 处理Term事件
     *
     * 该方法是Handler接口的实现，当有新的Term连接时会调用该方法。
     * 方法会将接收到的Term对象转发给ShellServer的handleTerm方法进行处理。
     * ShellServer会负责初始化Term、设置必要的处理器，并开始处理用户的输入。
     *
     * @param term 需要处理的Term对象，包含了终端连接的所有信息和操作接口
     */
    @Override
    public void handle(Term term) {
        shellServer.handleTerm(term);
    }
}
