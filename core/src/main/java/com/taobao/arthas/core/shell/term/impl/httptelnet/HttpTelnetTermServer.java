package com.taobao.arthas.core.shell.term.impl.httptelnet;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.distribution.ResultDistributor;
import com.taobao.arthas.core.shell.command.CommandProcess;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.impl.ShellImpl;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.Job;
import com.taobao.arthas.core.shell.system.JobListener;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.ProcessImpl;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.shell.term.TermServer;
import com.taobao.arthas.core.shell.term.impl.Helper;
import com.taobao.arthas.core.shell.term.impl.TermImpl;

import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

/**
 * both suport http/telnet
 * 
 * @author hengyunabc 2019-11-04
 *
 */
public class HttpTelnetTermServer extends TermServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpTelnetTermServer.class);

    private Handler<Term> termHandler;
    private NettyHttpTelnetTtyBootstrap bootstrap;
    private String hostIp;
    private int port;
    private long connectionTimeout;
    private EventExecutorGroup workerGroup;

    public HttpTelnetTermServer(String hostIp, int port, long connectionTimeout, EventExecutorGroup workerGroup) {
        this.hostIp = hostIp;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.workerGroup = workerGroup;
    }

    @Override
    public TermServer termHandler(Handler<Term> handler) {
        this.termHandler = handler;
        return this;
    }

    @Override
    public TermServer listen(Handler<Future<TermServer>> listenHandler) {
        // TODO: charset and inputrc from options
        bootstrap = new NettyHttpTelnetTtyBootstrap(workerGroup).setHost(hostIp).setPort(port);
        try {
            /**
             * #### 启动监听的时候，注册一个Consumer<TtyConnection>，
             * 当有连接到来时，{@link com.taobao.arthas.core.shell.handlers.server.TermServerTermHandler#handle(Term)}
             *                      -->{@link com.taobao.arthas.core.shell.impl.ShellServerImpl#handleTerm(Term)}
             *                          ->{@link ShellImpl#readline()}
             *                              ->{@link TermImpl#readline(String, Handler, Handler)}
             *                                  ->{@link io.termd.core.readline.Readline#readline(TtyConnection, String, Consumer)} //中间件
             *                                      ->{@link io.termd.core.readline.Readline.Interaction#Interaction(TtyConnection, String, Consumer, Consumer)} //中间件 往Interaction注册对应的Consumer<String>，最终中间件收到消息会回调该Consumer
             *                                          。。。
             *                                          对于arthas，该Consumer的实现是{@link com.taobao.arthas.core.shell.handlers.term.RequestHandler},所以连接上收到消息的处理逻辑
             *                                              -->{@link com.taobao.arthas.core.shell.handlers.term.RequestHandler#accept(String)}
             *                                                  ->{@link com.taobao.arthas.core.shell.handlers.shell.ShellLineHandler#handle(String)}
             *                                                      ->{@link com.taobao.arthas.core.shell.handlers.shell.ShellLineHandler#createJob(List)}
             *                                                          ->{@link com.taobao.arthas.core.shell.system.impl.JobControllerImpl#createJob(InternalCommandManager, List, Session, JobListener, Term, ResultDistributor)}
             *                                                              //这里完成最终的命令选择，然后注入Process中
             *                                                              ->{@link InternalCommandManager#getCommand(String)} //返回{@link com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl}
             *                                                      ->{@link com.taobao.arthas.core.shell.system.impl.JobImpl#run()}
             *                                                          ->{@link com.taobao.arthas.core.shell.system.impl.ProcessImpl#run(boolean)}
             *                                                              ->最终包装一个{@link ProcessImpl.CommandProcessTask} 提交到业务线程
             *                                                              。。。
             *                                                                  业务线程收到命令调度后执行逻辑
             *                                                                  {@link ProcessImpl.CommandProcessTask#run()}
             *                                                                      ->{@link com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl.ProcessHandler#handle(CommandProcess)}
             *                                                                          ->{@link com.taobao.arthas.core.shell.command.impl.AnnotatedCommandImpl#process(CommandProcess)}
             *                                                                              //根据AnnotatedCommandImpl#clazz属性，反射创建对应的命令实例，执行具体的处理逻辑
             *                                                                              ->{@link com.taobao.arthas.core.command.monitor200.ThreadCommand#process(CommandProcess)}
             *
             */
            bootstrap.start(new Consumer<TtyConnection>() {
                @Override
                public void accept(final TtyConnection conn) {
                    termHandler.handle(new TermImpl(Helper.loadKeymap(), conn));
                }
            }).get(connectionTimeout, TimeUnit.MILLISECONDS);
            listenHandler.handle(Future.<TermServer>succeededFuture());
        } catch (Throwable t) {
            logger.error("Error listening to port " + port, t);
            listenHandler.handle(Future.<TermServer>failedFuture(t));
        }
        return this;
    }

    @Override
    public int actualPort() {
        return bootstrap.getPort();
    }

    @Override
    public void close() {
        close(null);
    }

    @Override
    public void close(Handler<Future<Void>> completionHandler) {
        if (bootstrap != null) {
            bootstrap.stop();
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>succeededFuture());
            }
        } else {
            if (completionHandler != null) {
                completionHandler.handle(Future.<Void>failedFuture("telnet term server not started"));
            }
        }
    }
}
