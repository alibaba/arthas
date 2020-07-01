package com.taobao.arthas.core.shell.command;

import com.taobao.arthas.core.advisor.AdviceListener;
import com.taobao.arthas.core.command.model.ResultModel;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.Tty;
import com.taobao.middleware.cli.CommandLine;

import java.lang.instrument.ClassFileTransformer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The command process provides interaction with the process of the command.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface CommandProcess extends Tty {
    /**
     * @return the unparsed arguments tokens
     */
    List<CliToken> argsTokens();

    /**
     * @return the actual string arguments of the command
     */
    List<String> args();

    /**
     * @return the command line object or null
     */
    CommandLine commandLine();

    /**
     * @return the shell session
     */
    Session session();

    /**
     * @return true if the command is running in foreground
     */
    boolean isForeground();

    CommandProcess stdinHandler(Handler<String> handler);

    /**
     * Set an interrupt handler, this handler is called when the command is interrupted, for instance user
     * press <code>Ctrl-C</code>.
     *
     * @param handler the interrupt handler
     * @return this command
     */
    CommandProcess interruptHandler(Handler<Void> handler);

    /**
     * Set a suspend handler, this handler is called when the command is suspended, for instance user
     * press <code>Ctrl-Z</code>.
     *
     * @param handler the interrupt handler
     * @return this command
     */
    CommandProcess suspendHandler(Handler<Void> handler);

    /**
     * Set a resume handler, this handler is called when the command is resumed, for instance user
     * types <code>bg</code> or <code>fg</code> to resume the command.
     *
     * @param handler the interrupt handler
     * @return this command
     */
    CommandProcess resumeHandler(Handler<Void> handler);

    /**
     * Set an end handler, this handler is called when the command is ended, for instance the command is running
     * and the shell closes.
     *
     * @param handler the end handler
     * @return a reference to this, so the API can be used fluently
     */
    CommandProcess endHandler(Handler<Void> handler);

    /**
     * Write some text to the standard output.
     *
     * @param data the text
     * @return a reference to this, so the API can be used fluently
     */
    CommandProcess write(String data);

    /**
     * Set a background handler, this handler is called when the command is running and put to background.
     *
     * @param handler the background handler
     * @return this command
     */
    CommandProcess backgroundHandler(Handler<Void> handler);

    /**
     * Set a foreground handler, this handler is called when the command is running and put to foreground.
     *
     * @param handler the foreground handler
     * @return this command
     */
    CommandProcess foregroundHandler(Handler<Void> handler);

    @Override
    CommandProcess resizehandler(Handler<Void> handler);

    /**
     * End the process with the exit status {@literal 0}
     */
    void end();

    /**
     * End the process.
     *
     * @param status the exit status.
     */
    void end(int status);

    /**
     * End the process.
     *
     * @param status the exit status.
     */
    void end(int status, String message);


    /**
     * Register listener
     *
     * @param listener
     */
    void register(AdviceListener listener, ClassFileTransformer transformer);

    /**
     * Unregister listener
     */
    void unregister();

    /**
     * Execution times
     *
     * @return execution times
     */
    AtomicInteger times();

    /**
     * Resume process
     */
    void resume();

    /**
     * Suspend process
     */
    void suspend();

    /**
     * echo tips
     *
     * @param tips process tips
     */
    void echoTips(String tips);

    /**
     * Get cache file location
     *
     * @return
     */
    String cacheLocation();

    /**
     * Whether the process is running
     */
    boolean isRunning();

    /**
     * Append the phased result to queue
     * @param result a phased result of the command
     */
    void appendResult(ResultModel result);

}
