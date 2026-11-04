package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.shell.session.Session;

import java.util.List;

/**
 * The completion object
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Completion {

    /**
     * @return the shell current session, useful for accessing data like the current path for file completion, etc...
     */
    Session session();

    /**
     * @return the current line being completed in raw format, i.e without any char escape performed
     */
    String rawLine();

    /**
     * @return the current line being completed as preparsed tokens
     */
    List<CliToken> lineTokens();

    /**
     * End the completion with a list of candidates, these candidates will be displayed by the shell on the console.
     *
     * @param candidates the candidates
     */
    void complete(List<String> candidates);

    /**
     * End the completion with a value that will be inserted to complete the line.
     *
     * @param value the value to complete with
     * @param terminal true if the value is terminal, i.e can be further completed
     */
    void complete(String value, boolean terminal);

}
