package com.taobao.arthas.core.shell.term.impl;

import com.taobao.arthas.core.shell.cli.CliToken;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.cli.CompletionUtils;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.util.StringUtils;

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
        String lastToken = tokens.isEmpty() ? null : tokens.get(tokens.size() - 1).value();
        if(StringUtils.isBlank(lastToken)) {
            lastToken = "";
        }
        if (candidates.size() > 1) {
            // complete common prefix
            String commonPrefix = CompletionUtils.findLongestCommonPrefix(candidates);
            if (commonPrefix.length() > 0) {
                if (!commonPrefix.equals(lastToken)) {
                    // only complete if the common prefix is longer than the last token
                    if (commonPrefix.length() > lastToken.length()) {
                        String strToComplete = commonPrefix.substring(lastToken.length());
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
