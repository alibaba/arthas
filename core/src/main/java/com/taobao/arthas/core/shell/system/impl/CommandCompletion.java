package com.taobao.arthas.core.shell.system.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.session.Session;

import java.util.List;

/**
 * @author beiwei30 on 23/11/2016.
 */
class CommandCompletion implements Completion {
    private final Completion completion;
    private final String line;
    private final List<CliToken> newTokens;

    public CommandCompletion(Completion completion, String line, List<CliToken> newTokens) {
        this.completion = completion;
        this.line = line;
        this.newTokens = newTokens;
    }

    @Override
    public Session session() {
        return completion.session();
    }

    @Override
    public String rawLine() {
        return line;
    }

    @Override
    public List<CliToken> lineTokens() {
        return newTokens;
    }

    @Override
    public void complete(List<String> candidates) {
        completion.complete(candidates);
    }

    @Override
    public void complete(String value, boolean terminal) {
        completion.complete(value, terminal);
    }
}
