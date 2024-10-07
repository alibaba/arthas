package com.taobao.arthas.core.shell.term.impl;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.handlers.term.CloseHandlerWrapper;
import com.taobao.arthas.core.shell.handlers.term.DefaultTermStdinHandler;
import com.taobao.arthas.core.shell.handlers.term.EventHandler;
import com.taobao.arthas.core.shell.handlers.term.RequestHandler;
import com.taobao.arthas.core.shell.handlers.term.SizeHandlerWrapper;
import com.taobao.arthas.core.shell.handlers.term.StdinHandlerWrapper;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;
import com.taobao.arthas.core.util.Constants;
import com.taobao.arthas.core.util.FileUtils;

import io.termd.core.function.Consumer;
import io.termd.core.readline.Function;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.readline.functions.HistorySearchForward;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermImpl implements Term {

    private static final List<Function> readlineFunctions = Helper.loadServices(Function.class.getClassLoader(), Function.class);

    private Readline readline;
    private Consumer<int[]> echoHandler;
    private TtyConnection conn;
    private volatile Handler<String> stdinHandler;
    private List<io.termd.core.function.Function<String, String>> stdoutHandlerChain;
    private SignalHandler interruptHandler;
    private SignalHandler suspendHandler;
    private Session session;
    private boolean inReadline;

    public TermImpl(TtyConnection conn) {
        this(com.taobao.arthas.core.shell.term.impl.Helper.loadKeymap(), conn);
    }

    public TermImpl(Keymap keymap, TtyConnection conn) {
        this.conn = conn;
        readline = new Readline(keymap);
        readline.setHistory(FileUtils.loadCommandHistory(new File(Constants.CMD_HISTORY_FILE)));
        for (Function function : readlineFunctions) {
            /**
             * 防止没有鉴权时，查看历史命令
             * 
             * @see io.termd.core.readline.functions.HistorySearchForward
             */
            if (function.name().contains("history")) {
                FunctionInvocationHandler funcHandler = new FunctionInvocationHandler(this, function);
                function = (Function) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                        HistorySearchForward.class.getInterfaces(), funcHandler);

            }

            readline.addFunction(function);
        }

        echoHandler = new DefaultTermStdinHandler(this);
        conn.setStdinHandler(echoHandler);
        conn.setEventHandler(new EventHandler(this));
    }

    @Override
    public Term setSession(Session session) {
        this.session = session;
        return this;
    }

    public Session getSession() {
        return session;
    }

    @Override
    public void readline(String prompt, Handler<String> lineHandler) {
        if (conn.getStdinHandler() != echoHandler) {
            throw new IllegalStateException();
        }
        if (inReadline) {
            throw new IllegalStateException();
        }
        inReadline = true;
        readline.readline(conn, prompt, new RequestHandler(this, lineHandler));
    }

    public void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler) {
        if (conn.getStdinHandler() != echoHandler) {
            throw new IllegalStateException();
        }
        if (inReadline) {
            throw new IllegalStateException();
        }
        inReadline = true;
        readline.readline(conn, prompt, new RequestHandler(this, lineHandler), new CompletionHandler(completionHandler, session));
    }

    @Override
    public Term closeHandler(final Handler<Void> handler) {
        if (handler != null) {
            conn.setCloseHandler(new CloseHandlerWrapper(handler));
        } else {
            conn.setCloseHandler(null);
        }
        return this;
    }

    public long lastAccessedTime() {
        return conn.lastAccessedTime();
    }

    @Override
    public String type() {
        return conn.terminalType();
    }

    @Override
    public int width() {
        return conn.size() != null ? conn.size().x() : -1;
    }

    @Override
    public int height() {
        return conn.size() != null ? conn.size().y() : -1;
    }

    void checkPending() {
        if (stdinHandler != null && readline.hasEvent()) {
            stdinHandler.handle(Helper.fromCodePoints(readline.nextEvent().buffer().array()));
            checkPending();
        }
    }

    @Override
    public TermImpl resizehandler(Handler<Void> handler) {
        if (inReadline) {
            throw new IllegalStateException();
        }
        if (handler != null) {
            conn.setSizeHandler(new SizeHandlerWrapper(handler));
        } else {
            conn.setSizeHandler(null);
        }
        return this;
    }

    @Override
    public Term stdinHandler(final Handler<String> handler) {
        if (inReadline) {
            throw new IllegalStateException();
        }
        stdinHandler = handler;
        if (handler != null) {
            conn.setStdinHandler(new StdinHandlerWrapper(handler));
            checkPending();
        } else {
            conn.setStdinHandler(echoHandler);
        }
        return this;
    }

    @Override
    public Term stdoutHandler(io.termd.core.function.Function<String, String>  handler) {
        if (stdoutHandlerChain == null) {
            stdoutHandlerChain = new ArrayList<io.termd.core.function.Function<String, String>>();
        }
        stdoutHandlerChain.add(handler);
        return this;
    }

    @Override
    public Term write(String data) {
        if (stdoutHandlerChain != null) {
            for (io.termd.core.function.Function<String, String> function : stdoutHandlerChain) {
                data = function.apply(data);
            }
        }
        conn.write(data);
        return this;
    }

    public TermImpl interruptHandler(SignalHandler handler) {
        interruptHandler = handler;
        return this;
    }

    public TermImpl suspendHandler(SignalHandler handler) {
        suspendHandler = handler;
        return this;
    }

    public void close() {
        conn.close();
        FileUtils.saveCommandHistory(readline.getHistory(), new File(Constants.CMD_HISTORY_FILE));
    }

    public TermImpl echo(String text) {
        echo(Helper.toCodePoints(text));
        return this;
    }

    public void setInReadline(boolean inReadline) {
        this.inReadline = inReadline;
    }

    public Readline getReadline() {
        return readline;
    }

    public void handleIntr(Integer key) {
        if (interruptHandler == null || !interruptHandler.deliver(key)) {
            echo(key, '\n');
        }
    }

    public void handleEof(Integer key) {
        // Pseudo signal
        if (stdinHandler != null) {
            stdinHandler.handle(Helper.fromCodePoints(new int[]{key}));
        } else {
            echo(key);
            readline.queueEvent(new int[]{key});
        }
    }

    public void handleSusp(Integer key) {
        if (suspendHandler == null || !suspendHandler.deliver(key)) {
            echo(key, 'Z' - 64);
        }
    }

    public TtyConnection getConn() {
        return conn;
    }

    public void echo(int... codePoints) {
        Consumer<int[]> out = conn.stdoutHandler();
        for (int codePoint : codePoints) {
            if (codePoint < 32) {
                if (codePoint == '\t') {
                    out.accept(new int[]{'\t'});
                } else if (codePoint == '\b') {
                    out.accept(new int[]{'\b', ' ', '\b'});
                } else if (codePoint == '\r' || codePoint == '\n') {
                    out.accept(new int[]{'\n'});
                } else {
                    out.accept(new int[]{'^', codePoint + 64});
                }
            } else {
                if (codePoint == 127) {
                    out.accept(new int[]{'\b', ' ', '\b'});
                } else {
                    out.accept(new int[]{codePoint});
                }
            }
        }
    }
}
