package com.taobao.arthas.core.shell.term.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.session.Session;

import java.util.LinkedList;
import java.util.List;

/**
 * @author beiwei30 on 23/11/2016.
 */
class CompletionAdaptor implements Completion {
    private final Session session;
    private final String line;
    private final List<CliToken> tokens;
    private final io.termd.core.readline.Completion completion;

    public CompletionAdaptor(String line, List<CliToken> tokens, io.termd.core.readline.Completion completion,
                             Session session) {
        this.line = line;
        this.tokens = tokens;
        this.completion = completion;
        this.session = session;
    }

    @Override
    public Session session() {
        return session;
    }

    @Override
    public String rawLine() {
        return line;
    }

    @Override
    public List<CliToken> lineTokens() {
        return tokens;
    }

    @Override
    public void complete(List<String> candidates) {
        if (candidates.size() > 1) {
            // complete common prefix
            String commonPrefix = CompletionUtils.findLongestCommonPrefix(candidates);
            if (commonPrefix.length() > 0) {
                CliToken lastToken = tokens.get(tokens.size() - 1);
                if (!commonPrefix.equals(lastToken.value())) {
                    // only complete if the common prefix is longer than the last token
                    if (commonPrefix.length() > lastToken.value().length()) {
                        String strToComplete = commonPrefix.substring(lastToken.value().length());
                        completion.complete(io.termd.core.util.Helper.toCodePoints(strToComplete), false);
                        return;
                    }
                }
            }
        }
        if (candidates.size() > 0) {
            List<int[]> suggestions = new LinkedList<int[]>();
            for (String candidate : candidates) {
                suggestions.add(io.termd.core.util.Helper.toCodePoints(candidate));
            }
            completion.suggest(suggestions);
        } else {
            completion.end();
        }
    }

    @Override
    public void complete(String value, boolean terminal) {
        completion.complete(io.termd.core.util.Helper.toCodePoints(value), terminal);
    }
}
