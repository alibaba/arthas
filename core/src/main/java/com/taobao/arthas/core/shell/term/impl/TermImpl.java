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
 * 终端实现类
 *
 * 提供了终端的具体实现，包括命令行读取、历史记录、信号处理等功能
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TermImpl implements Term {

    /**
     * 加载所有的readline函数，用于扩展命令行编辑功能
     */
    private static final List<Function> readlineFunctions = Helper.loadServices(Function.class.getClassLoader(), Function.class);

    /**
     * Readline实例，提供命令行编辑和历史记录功能
     */
    private Readline readline;

    /**
     * 回显处理器，用于处理终端输出
     */
    private Consumer<int[]> echoHandler;

    /**
     * Tty连接对象，代表与终端的底层连接
     */
    private TtyConnection conn;

    /**
     * 标准输入处理器，使用volatile确保多线程可见性
     */
    private volatile Handler<String> stdinHandler;

    /**
     * 标准输出处理器链，用于对输出进行多层处理
     */
    private List<io.termd.core.function.Function<String, String>> stdoutHandlerChain;

    /**
     * 中断信号处理器（如Ctrl+C）
     */
    private SignalHandler interruptHandler;

    /**
     * 挂起信号处理器（如Ctrl+Z）
     */
    private SignalHandler suspendHandler;

    /**
     * 关联的会话对象
     */
    private Session session;

    /**
     * 标记是否正在执行readline操作
     */
    private boolean inReadline;

    /**
     * 构造函数，使用默认的键盘映射
     *
     * @param conn Tty连接对象
     */
    public TermImpl(TtyConnection conn) {
        this(com.taobao.arthas.core.shell.term.impl.Helper.loadKeymap(), conn);
    }

    /**
     * 构造函数，使用指定的键盘映射
     *
     * @param keymap 键盘映射配置
     * @param conn Tty连接对象
     */
    public TermImpl(Keymap keymap, TtyConnection conn) {
        this.conn = conn;
        // 创建Readline实例并设置键盘映射
        readline = new Readline(keymap);
        // 加载命令历史记录
        readline.setHistory(FileUtils.loadCommandHistory(new File(Constants.CMD_HISTORY_FILE)));
        // 遍历并添加所有readline函数
        for (Function function : readlineFunctions) {
            /**
             * 防止没有鉴权时，查看历史命令
             * 通过动态代理对历史命令相关功能进行权限控制
             *
             * @see io.termd.core.readline.functions.HistorySearchForward
             */
            if (function.name().contains("history")) {
                // 创建动态代理处理器，用于权限检查
                FunctionInvocationHandler funcHandler = new FunctionInvocationHandler(this, function);
                // 创建代理对象
                function = (Function) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                        HistorySearchForward.class.getInterfaces(), funcHandler);

            }

            // 将函数添加到readline中
            readline.addFunction(function);
        }

        // 创建默认的回显处理器
        echoHandler = new DefaultTermStdinHandler(this);
        // 设置标准输入处理器
        conn.setStdinHandler(echoHandler);
        // 设置事件处理器
        conn.setEventHandler(new EventHandler(this));
    }

    /**
     * 设置会话对象
     *
     * @param session 要设置的会话对象
     * @return 返回当前实例，支持链式调用
     */
    @Override
    public Term setSession(Session session) {
        this.session = session;
        return this;
    }

    /**
     * 获取当前会话对象
     *
     * @return 当前会话对象
     */
    public Session getSession() {
        return session;
    }

    /**
     * 读取一行用户输入
     *
     * @param prompt 显示的提示符
     * @param lineHandler 处理输入行的处理器
     * @throws IllegalStateException 如果当前状态不允许读取（如正在读取中或标准输入处理器不正确）
     */
    @Override
    public void readline(String prompt, Handler<String> lineHandler) {
        // 检查标准输入处理器是否正确
        if (conn.getStdinHandler() != echoHandler) {
            throw new IllegalStateException();
        }
        // 检查是否已经在读取状态
        if (inReadline) {
            throw new IllegalStateException();
        }
        // 设置读取标志
        inReadline = true;
        // 调用readline读取输入
        readline.readline(conn, prompt, new RequestHandler(this, lineHandler));
    }

    /**
     * 读取一行用户输入，并提供自动完成功能
     *
     * @param prompt 显示的提示符
     * @param lineHandler 处理输入行的处理器
     * @param completionHandler 自动完成处理器
     * @throws IllegalStateException 如果当前状态不允许读取
     */
    public void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler) {
        // 检查标准输入处理器是否正确
        if (conn.getStdinHandler() != echoHandler) {
            throw new IllegalStateException();
        }
        // 检查是否已经在读取状态
        if (inReadline) {
            throw new IllegalStateException();
        }
        // 设置读取标志
        inReadline = true;
        // 调用readline读取输入，并传入自动完成处理器
        readline.readline(conn, prompt, new RequestHandler(this, lineHandler), new CompletionHandler(completionHandler, session));
    }

    /**
     * 设置终端关闭处理器
     *
     * @param handler 关闭事件处理器
     * @return 返回当前实例，支持链式调用
     */
    @Override
    public Term closeHandler(final Handler<Void> handler) {
        if (handler != null) {
            // 包装并设置关闭处理器
            conn.setCloseHandler(new CloseHandlerWrapper(handler));
        } else {
            // 清除关闭处理器
            conn.setCloseHandler(null);
        }
        return this;
    }

    /**
     * 获取最后一次访问时间
     *
     * @return 最后一次访问的时间戳
     */
    public long lastAccessedTime() {
        return conn.lastAccessedTime();
    }

    /**
     * 获取终端类型
     *
     * @return 终端类型字符串
     */
    @Override
    public String type() {
        return conn.terminalType();
    }

    /**
     * 获取终端宽度（字符数）
     *
     * @return 终端宽度，如果无法获取则返回-1
     */
    @Override
    public int width() {
        return conn.size() != null ? conn.size().x() : -1;
    }

    /**
     * 获取终端高度（行数）
     *
     * @return 终端高度，如果无法获取则返回-1
     */
    @Override
    public int height() {
        return conn.size() != null ? conn.size().y() : -1;
    }

    /**
     * 检查并处理待处理的事件
     *
     * 如果有标准输入处理器且readline有待处理的事件，则递归处理所有待处理事件
     */
    void checkPending() {
        if (stdinHandler != null && readline.hasEvent()) {
            // 将事件转换为字符串并传递给处理器
            stdinHandler.handle(Helper.fromCodePoints(readline.nextEvent().buffer().array()));
            // 递归检查并处理下一个待处理事件
            checkPending();
        }
    }

    /**
     * 设置终端大小调整处理器
     *
     * @param handler 大小调整事件处理器
     * @return 返回当前实例，支持链式调用
     * @throws IllegalStateException 如果正在执行readline操作
     */
    @Override
    public TermImpl resizehandler(Handler<Void> handler) {
        if (inReadline) {
            throw new IllegalStateException();
        }
        if (handler != null) {
            // 包装并设置大小调整处理器
            conn.setSizeHandler(new SizeHandlerWrapper(handler));
        } else {
            // 清除大小调整处理器
            conn.setSizeHandler(null);
        }
        return this;
    }

    /**
     * 设置标准输入处理器
     *
     * @param handler 标准输入处理器
     * @return 返回当前实例，支持链式调用
     * @throws IllegalStateException 如果正在执行readline操作
     */
    @Override
    public Term stdinHandler(final Handler<String> handler) {
        if (inReadline) {
            throw new IllegalStateException();
        }
        // 保存处理器引用
        stdinHandler = handler;
        if (handler != null) {
            // 包装并设置标准输入处理器
            conn.setStdinHandler(new StdinHandlerWrapper(handler));
            // 检查是否有待处理的事件
            checkPending();
        } else {
            // 恢复默认的回显处理器
            conn.setStdinHandler(echoHandler);
        }
        return this;
    }

    /**
     * 设置标准输出处理器
     *
     * @param handler 标准输出处理器，用于处理输出的转换
     * @return 返回当前实例，支持链式调用
     */
    @Override
    public Term stdoutHandler(io.termd.core.function.Function<String, String>  handler) {
        // 初始化输出处理器链
        if (stdoutHandlerChain == null) {
            stdoutHandlerChain = new ArrayList<io.termd.core.function.Function<String, String>>();
        }
        // 将处理器添加到链中
        stdoutHandlerChain.add(handler);
        return this;
    }

    /**
     * 向终端写入数据
     *
     * 数据会经过所有标准输出处理器的处理后再输出
     *
     * @param data 要写入的数据
     * @return 返回当前实例，支持链式调用
     */
    @Override
    public Term write(String data) {
        // 如果有输出处理器链，则依次处理数据
        if (stdoutHandlerChain != null) {
            for (io.termd.core.function.Function<String, String> function : stdoutHandlerChain) {
                data = function.apply(data);
            }
        }
        // 将处理后的数据写入连接
        conn.write(data);
        return this;
    }

    /**
     * 设置中断信号处理器
     *
     * @param handler 中断信号处理器
     * @return 返回当前实例，支持链式调用
     */
    public TermImpl interruptHandler(SignalHandler handler) {
        interruptHandler = handler;
        return this;
    }

    /**
     * 设置挂起信号处理器
     *
     * @param handler 挂起信号处理器
     * @return 返回当前实例，支持链式调用
     */
    public TermImpl suspendHandler(SignalHandler handler) {
        suspendHandler = handler;
        return this;
    }

    /**
     * 关闭终端连接
     *
     * 关闭连接前会保存命令历史记录到文件
     */
    public void close() {
        // 关闭连接
        conn.close();
        // 保存命令历史记录
        FileUtils.saveCommandHistory(readline.getHistory(), new File(Constants.CMD_HISTORY_FILE));
    }

    /**
     * 在终端中回显文本
     *
     * @param text 要回显的文本
     * @return 返回当前实例，支持链式调用
     */
    public TermImpl echo(String text) {
        // 将文本转换为码点数组并回显
        echo(Helper.toCodePoints(text));
        return this;
    }

    /**
     * 设置是否正在执行readline操作
     *
     * @param inReadline 是否在readline状态
     */
    public void setInReadline(boolean inReadline) {
        this.inReadline = inReadline;
    }

    /**
     * 获取Readline实例
     *
     * @return Readline实例
     */
    public Readline getReadline() {
        return readline;
    }

    /**
     * 处理中断信号（如Ctrl+C）
     *
     * @param key 中断信号的按键码
     */
    public void handleIntr(Integer key) {
        // 如果没有中断处理器或处理器返回false，则回显中断字符和换行
        if (interruptHandler == null || !interruptHandler.deliver(key)) {
            echo(key, '\n');
        }
    }

    /**
     * 处理文件结束信号（EOF，如Ctrl+D）
     *
     * @param key EOF信号的按键码
     */
    public void handleEof(Integer key) {
        // 伪信号
        // 如果有标准输入处理器，则传递给处理器
        if (stdinHandler != null) {
            stdinHandler.handle(Helper.fromCodePoints(new int[]{key}));
        } else {
            // 否则回显字符并将事件加入队列
            echo(key);
            readline.queueEvent(new int[]{key});
        }
    }

    /**
     * 处理挂起信号（如Ctrl+Z）
     *
     * @param key 挂起信号的按键码
     */
    public void handleSusp(Integer key) {
        // 如果没有挂起处理器或处理器返回false，则回显挂起字符
        if (suspendHandler == null || !suspendHandler.deliver(key)) {
            echo(key, 'Z' - 64);
        }
    }

    /**
     * 获取Tty连接对象
     *
     * @return Tty连接对象
     */
    public TtyConnection getConn() {
        return conn;
    }

    /**
     * 回显码点到终端
     *
     * 对控制字符进行特殊处理：
     * - 小于32的控制字符用^符号表示（如Ctrl+C显示为^C）
     * - 制表符、退格符、回车符等特殊字符保持原样
     * - 127（DEL）显示为退格
     *
     * @param codePoints 要回显的Unicode码点数组
     */
    public void echo(int... codePoints) {
        // 获取标准输出处理器
        Consumer<int[]> out = conn.stdoutHandler();
        for (int codePoint : codePoints) {
            if (codePoint < 32) {
                // 处理控制字符（ASCII 0-31）
                if (codePoint == '\t') {
                    // 制表符，直接输出
                    out.accept(new int[]{'\t'});
                } else if (codePoint == '\b') {
                    // 退格符，输出退格、空格、退格以清除字符
                    out.accept(new int[]{'\b', ' ', '\b'});
                } else if (codePoint == '\r' || codePoint == '\n') {
                    // 回车或换行符，统一输出换行
                    out.accept(new int[]{'\n'});
                } else {
                    // 其他控制字符，用^符号表示（如Ctrl+A显示为^A）
                    out.accept(new int[]{'^', codePoint + 64});
                }
            } else {
                // 处理可打印字符和删除符
                if (codePoint == 127) {
                    // DEL字符，输出退格清除
                    out.accept(new int[]{'\b', ' ', '\b'});
                } else {
                    // 普通可打印字符，直接输出
                    out.accept(new int[]{codePoint});
                }
            }
        }
    }
}
