package com.taobao.arthas.core.shell.term.impl.httptelnet;

import java.nio.charset.Charset;

import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.function.Supplier;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.CompletableFuture;
import io.termd.core.util.Helper;

/**
 * 基于Netty的HTTP Telnet Tty引导类
 *
 * 提供了对Tty连接的支持，封装了NettyHttpTelnetBootstrap，
 * 添加了对Telnet二进制选项和字符集的配置功能
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-11-05
 */
public class NettyHttpTelnetTtyBootstrap {

    // 底层的HTTP Telnet引导类，负责实际的服务器启动和停止
    private final NettyHttpTelnetBootstrap httpTelnetTtyBootstrap;

    // 是否启用输出时的Telnet BINARY选项
    // BINARY选项表示传输的是二进制数据，不需要进行字符转换
    private boolean outBinary;

    // 是否启用输入时的Telnet BINARY选项
    private boolean inBinary;

    // 字符编码，默认为UTF-8
    private Charset charset = Charset.forName("UTF-8");

    /**
     * 构造函数
     *
     * @param workerGroup 工作线程组，用于执行业务逻辑
     * @param httpSessionManager HTTP会话管理器，用于管理HTTP会话
     */
    public NettyHttpTelnetTtyBootstrap(EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
        // 创建底层的HTTP Telnet引导类
        this.httpTelnetTtyBootstrap = new NettyHttpTelnetBootstrap(workerGroup, httpSessionManager);
    }

    /**
     * 获取监听的主机地址
     *
     * @return 主机地址
     */
    public String getHost() {
        return httpTelnetTtyBootstrap.getHost();
    }

    /**
     * 设置监听的主机地址
     *
     * @param host 主机地址
     * @return 当前对象，支持链式调用
     */
    public NettyHttpTelnetTtyBootstrap setHost(String host) {
        httpTelnetTtyBootstrap.setHost(host);
        return this;
    }

    /**
     * 获取监听的端口号
     *
     * @return 端口号
     */
    public int getPort() {
        return httpTelnetTtyBootstrap.getPort();
    }

    /**
     * 设置监听的端口号
     *
     * @param port 端口号
     * @return 当前对象，支持链式调用
     */
    public NettyHttpTelnetTtyBootstrap setPort(int port) {
        httpTelnetTtyBootstrap.setPort(port);
        return this;
    }

    /**
     * 获取输出是否启用二进制模式
     *
     * @return 如果启用输出二进制模式返回true，否则返回false
     */
    public boolean isOutBinary() {
        return outBinary;
    }

    /**
     * 设置输出时是否启用Telnet BINARY选项
     *
     * BINARY选项用于控制数据传输方式：
     * - true: 客户端必须接收二进制数据，不进行字符转换
     * - false: 客户端接收文本数据，可能进行字符转换
     *
     * @param outBinary true表示客户端必须接收二进制数据
     * @return 当前对象，支持链式调用
     */
    public NettyHttpTelnetTtyBootstrap setOutBinary(boolean outBinary) {
        this.outBinary = outBinary;
        return this;
    }

    /**
     * 获取输入是否启用二进制模式
     *
     * @return 如果启用输入二进制模式返回true，否则返回false
     */
    public boolean isInBinary() {
        return inBinary;
    }

    /**
     * 设置输入时是否启用Telnet BINARY选项
     *
     * BINARY选项用于控制数据传输方式：
     * - true: 客户端必须发送二进制数据，不进行字符转换
     * - false: 客户端发送文本数据，可能进行字符转换
     *
     * @param inBinary true表示客户端必须发送二进制数据
     * @return 当前对象，支持链式调用
     */
    public NettyHttpTelnetTtyBootstrap setInBinary(boolean inBinary) {
        this.inBinary = inBinary;
        return this;
    }

    /**
     * 获取字符编码
     *
     * @return 字符编码
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * 设置字符编码
     *
     * @param charset 字符编码
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /**
     * 异步启动服务器
     *
     * 创建并启动服务器，返回一个CompletableFuture用于等待启动完成
     *
     * @param factory Tty连接处理器工厂
     * @return CompletableFuture，用于等待启动完成
     */
    public CompletableFuture<?> start(Consumer<TtyConnection> factory) {
        // 创建一个CompletableFuture用于异步操作
        CompletableFuture<?> fut = new CompletableFuture();

        // 调用带回调的start方法，使用Helper工具类创建回调处理器
        start(factory, Helper.startedHandler(fut));

        // 返回Future，调用者可以通过它等待启动完成
        return fut;
    }

    /**
     * 异步停止服务器
     *
     * 停止服务器，返回一个CompletableFuture用于等待停止完成
     *
     * @return CompletableFuture，用于等待停止完成
     */
    public CompletableFuture<?> stop() {
        // 创建一个CompletableFuture用于异步操作
        CompletableFuture<?> fut = new CompletableFuture();

        // 调用带回调的stop方法，使用Helper工具类创建回调处理器
        stop(Helper.stoppedHandler(fut));

        // 返回Future，调用者可以通过它等待停止完成
        return fut;
    }

    /**
     * 启动服务器（带回调版本）
     *
     * 创建并启动服务器，启动完成后调用回调处理器
     * 会创建TelnetTtyConnection来处理Tty连接
     *
     * @param factory Tty连接处理器工厂，用于创建Tty连接
     * @param doneHandler 完成后的回调处理器，用于处理启动结果
     */
    public void start(final Consumer<TtyConnection> factory, Consumer<Throwable> doneHandler) {
        // 调用底层HTTP Telnet引导类的start方法
        // 创建一个TelnetHandler工厂，该工厂创建TelnetTtyConnection
        httpTelnetTtyBootstrap.start(new Supplier<TelnetHandler>() {
            @Override
            public TelnetHandler get() {
                // 创建TelnetTtyConnection，配置输入输出二进制模式和字符集
                // TelnetTtyConnection会将Tty连接适配为Telnet协议
                return new TelnetTtyConnection(inBinary, outBinary, charset, factory);
            }
        }, factory, doneHandler);
    }

    /**
     * 停止服务器（带回调版本）
     *
     * 停止服务器，停止完成后调用回调处理器
     *
     * @param doneHandler 完成后的回调处理器，用于处理停止结果
     */
    public void stop(Consumer<Throwable> doneHandler) {
        // 调用底层HTTP Telnet引导类的stop方法
        httpTelnetTtyBootstrap.stop(doneHandler);
    }
}
