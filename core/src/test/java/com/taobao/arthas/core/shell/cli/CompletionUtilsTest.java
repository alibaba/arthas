package com.taobao.arthas.core.shell.cli;

import com.taobao.arthas.core.command.klass100.JadCommand;
import com.taobao.arthas.core.shell.cli.impl.CliTokenImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CompletionUtilsTest {

    @Test
    void testDetectArgumentIndex() {

        List<CliToken> tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad --lineNumber true demo.MathGame"), "jad");
        assertEquals(1, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));

        tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad demo.MathGame"), "jad");
        assertEquals(1, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));

        tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad -d /root --lineNumber true demo.MathGame"), "jad");
        assertEquals(1, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));

        tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad -d /root --lineNumber true -E demo.MathGame"), "jad");
        assertEquals(1, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));

        tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad -d /root --lineNumber true -E -c xxx demo.MathGame"), "jad");
        assertEquals(1, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));

        tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad --source-only -d /root --lineNumber true -E -c xxx demo.MathGame"), "jad");
        assertEquals(1, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));

        tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad --source-only -d /root --lineNumber true -E -c xxx demo.MathGame "), "jad");
        assertEquals(2, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));

        tokens = removeCommandHeadTokens(CliTokenImpl.tokenize("jad --source-only   --lineNumber true -E -c xxx demo.MathGame -d /root pri"), "jad");
        assertEquals(2, CompletionUtils.detectArgumentIndex(tokens, JadCommand.class));


    }

    private static List<CliToken> removeCommandHeadTokens(List<CliToken> tokens, String cmd) {
        int idx = 0;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).isText() && !Objects.equals(tokens.get(i).value(), cmd)) {
                idx = i;
                break;
            }
        }
        return new ArrayList<>(tokens.subList(idx, tokens.size()));

    }
}