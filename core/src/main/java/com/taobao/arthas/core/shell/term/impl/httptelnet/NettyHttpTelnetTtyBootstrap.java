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
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author hengyunabc 2019-11-05
 */
public class NettyHttpTelnetTtyBootstrap {

    private final NettyHttpTelnetBootstrap httpTelnetTtyBootstrap;
    private boolean outBinary;
    private boolean inBinary;
    private Charset charset = Charset.forName("UTF-8");

    public NettyHttpTelnetTtyBootstrap(EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
        this.httpTelnetTtyBootstrap = new NettyHttpTelnetBootstrap(workerGroup, httpSessionManager);
    }

    public String getHost() {
        return httpTelnetTtyBootstrap.getHost();
    }

    public NettyHttpTelnetTtyBootstrap setHost(String host) {
        httpTelnetTtyBootstrap.setHost(host);
        return this;
    }

    public int getPort() {
        return httpTelnetTtyBootstrap.getPort();
    }

    public NettyHttpTelnetTtyBootstrap setPort(int port) {
        httpTelnetTtyBootstrap.setPort(port);
        return this;
    }

    public boolean isOutBinary() {
        return outBinary;
    }

    /**
     * Enable or disable the TELNET BINARY option on output.
     *
     * @param outBinary
     *            true to require the client to receive binary
     * @return this object
     */
    public NettyHttpTelnetTtyBootstrap setOutBinary(boolean outBinary) {
        this.outBinary = outBinary;
        return this;
    }

    public boolean isInBinary() {
        return inBinary;
    }

    /**
     * Enable or disable the TELNET BINARY option on input.
     *
     * @param inBinary
     *            true to require the client to emit binary
     * @return this object
     */
    public NettyHttpTelnetTtyBootstrap setInBinary(boolean inBinary) {
        this.inBinary = inBinary;
        return this;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public CompletableFuture<?> start(Consumer<TtyConnection> factory) {
        CompletableFuture<?> fut = new CompletableFuture();
        start(factory, Helper.startedHandler(fut));
        return fut;
    }

    public CompletableFuture<?> stop() {
        CompletableFuture<?> fut = new CompletableFuture();
        stop(Helper.stoppedHandler(fut));
        return fut;
    }

    public void start(final Consumer<TtyConnection> factory, Consumer<Throwable> doneHandler) {
        httpTelnetTtyBootstrap.start(new Supplier<TelnetHandler>() {
            @Override
            public TelnetHandler get() {
                return new TelnetTtyConnection(inBinary, outBinary, charset, factory);
            }
        }, factory, doneHandler);
    }

    public void stop(Consumer<Throwable> doneHandler) {
        httpTelnetTtyBootstrap.stop(doneHandler);
    }
}
