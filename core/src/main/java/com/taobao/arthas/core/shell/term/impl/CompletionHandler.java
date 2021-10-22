package com.taobao.arthas.core.shell.term.impl;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.CliTokens;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;

import io.termd.core.function.Consumer;
import io.termd.core.readline.Completion;

import java.util.Collections;
import java.util.List;

/**
 * @author beiwei30 on 23/11/2016.
 */
class CompletionHandler implements Consumer<Completion> {
    private static final Logger logger = LoggerFactory.getLogger(CompletionHandler.class);
    private final Handler<com.taobao.arthas.core.shell.cli.Completion> completionHandler;
    private final Session session;

    public CompletionHandler(Handler<com.taobao.arthas.core.shell.cli.Completion> completionHandler, Session session) {
        this.completionHandler = completionHandler;
        this.session = session;
    }

    @Override
    public void accept(final Completion completion) {
        try {
            final String line = io.termd.core.util.Helper.fromCodePoints(completion.line());
            final List<CliToken> tokens = Collections.unmodifiableList(CliTokens.tokenize(line));
            com.taobao.arthas.core.shell.cli.Completion comp = new CompletionAdaptor(line, tokens, completion, session);
            completionHandler.handle(comp);
        } catch (Throwable t) {
            // t.printStackTrace();
            logger.error("completion error", t);
        }
    }
}
