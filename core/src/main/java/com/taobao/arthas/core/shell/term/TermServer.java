package com.taobao.arthas.core.shell.term;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.config.Configure;
import com.taobao.arthas.core.shell.ShellServerOptions;
import com.taobao.arthas.core.shell.future.Future;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.impl.TelnetTermServer;

/**
 * A server for terminal based applications.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TermServer {

    /**
     * Create a term server for the Telnet protocol.
     *
     * @param configure
     * @return the term server
     */
    public static TermServer createTelnetTermServer(Configure configure, ShellServerOptions options) {
        int port = configure.getTelnetPort() != null ? configure.getTelnetPort() : ArthasConstants.TELNET_PORT;
        return new TelnetTermServer(configure.getIp(), port, options.getConnectionTimeout());
    }

    /**
     * Create a term server for the HTTP protocol, using an existing router.
     *
     * @return the term server
     */
    public static TermServer createHttpTermServer() {
        // TODO
        return null;
    }

    /**
     * Set the term handler that will receive incoming client connections. When a remote terminal connects
     * the {@code handler} will be called with the {@link Term} which can be used to interact with the remote
     * terminal.
     *
     * @param handler the term handler
     * @return this object
     */
    public abstract TermServer termHandler(Handler<Term> handler);

    /**
     * Bind the term server, the {@link #termHandler(Handler)} must be set before.
     *
     * @return this object
     */
    public TermServer listen() {
        return listen(null);
    }

    /**
     * Bind the term server, the {@link #termHandler(Handler)} must be set before.
     *
     * @param listenHandler the listen handler
     * @return this object
     */
    public abstract TermServer listen(Handler<Future<TermServer>> listenHandler);

    /**
     * The actual port the server is listening on. This is useful if you bound the server specifying 0 as port number
     * signifying an ephemeral port
     *
     * @return the actual port the server is listening on.
     */
    public abstract int actualPort();

    /**
     * Close the server. This will close any currently open connections. The close may not complete until after this
     * method has returned.
     */
    public abstract void close();

    /**
     * Like {@link #close} but supplying a handler that will be notified when close is complete.
     *
     * @param completionHandler the handler to be notified when the term server is closed
     */
    public abstract void close(Handler<Future<Void>> completionHandler);

}
