package com.taobao.arthas.core.shell.term;

import com.taobao.arthas.core.shell.handlers.Handler;

/**
 * Provide interactions with the Shell TTY.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Tty {

    /**
     * @return the declared tty type, for instance {@literal vt100}, {@literal xterm-256},  etc... it can be null
     * when the tty does not have declared its type.
     */
    String type();

    /**
     * @return the current width, i.e the number of rows or {@literal -1} if unknown
     */
    int width();

    /**
     * @return the current height, i.e the number of columns or {@literal -1} if unknown
     */
    int height();

    /**
     * Set a stream handler on the standard input to read the data.
     *
     * @param handler the standard input
     * @return this object
     */
    Tty stdinHandler(Handler<String> handler);

    /**
     * Write data to the standard output.
     *
     * @param data the data to write
     * @return this object
     */
    Tty write(String data);

    /**
     * Set a resize handler, the handler is called when the tty size changes.
     *
     * @param handler the resize handler
     * @return this object
     */
    Tty resizehandler(Handler<Void> handler);

}
