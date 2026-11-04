package com.taobao.arthas.core.shell.term;

import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import io.termd.core.function.Function;

/**
 * The terminal.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Term extends Tty {

    @Override
    Term resizehandler(Handler<Void> handler);

    @Override
    Term stdinHandler(Handler<String> handler);

    Term stdoutHandler(Function<String, String> handler);

    @Override
    Term write(String data);

    /**
     * @return the last time this term received input
     */
    long lastAccessedTime();

    /**
     * Echo some text in the terminal, escaped if necessary.<p/>
     *
     * @param text the text to echo
     * @return a reference to this, so the API can be used fluently
     */
    Term echo(String text);

    /**
     * Associate the term with a session.
     *
     * @param session the session to set
     * @return a reference to this, so the API can be used fluently
     */
    Term setSession(Session session);

    /**
     * Set an interrupt signal handler on the term.
     *
     * @param handler the interrupt handler
     * @return a reference to this, so the API can be used fluently
     */
    Term interruptHandler(SignalHandler handler);

    /**
     * Set a suspend signal handler on the term.
     *
     * @param handler the suspend handler
     * @return a reference to this, so the API can be used fluently
     */
    Term suspendHandler(SignalHandler handler);

    /**
     * Prompt the user a line of text.
     *
     * @param prompt the displayed prompt
     * @param lineHandler the line handler called with the line
     */
    void readline(String prompt, Handler<String> lineHandler);

    /**
     * Prompt the user a line of text, providing a completion handler to handle user's completion.
     *
     * @param prompt the displayed prompt
     * @param lineHandler the line handler called with the line
     * @param completionHandler the completion handler
     */
    void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler);

    /**
     * Set a handler that will be called when the terminal is closed.
     *
     * @param handler the handler
     * @return a reference to this, so the API can be used fluently
     */
    Term closeHandler(Handler<Void> handler);

    /**
     * Close the connection to terminal.
     */
    void close();
}
