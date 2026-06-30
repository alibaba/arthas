package com.taobao.arthas.core.shell.impl;

import com.taobao.arthas.core.shell.ShellServer;
import com.taobao.arthas.core.shell.cli.Completion;
import com.taobao.arthas.core.shell.command.CommandResolver;
import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.system.impl.InternalCommandManager;
import com.taobao.arthas.core.shell.system.impl.JobControllerImpl;
import com.taobao.arthas.core.shell.term.SignalHandler;
import com.taobao.arthas.core.shell.term.Term;
import io.termd.core.function.Function;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ShellImplQuietTest {

    @Test
    void initShouldWriteWelcomeForNormalSession() {
        RecordingTerm term = new RecordingTerm("xterm");
        ShellImpl shell = newShell(term);

        shell.setWelcome("welcome");
        shell.init();

        assertThat(term.output()).isEqualTo("welcome\n");
    }

    @Test
    void initShouldSkipWelcomeForArthasAgentTerminalType() {
        RecordingTerm term = new RecordingTerm("arthas-agent");
        ShellImpl shell = newShell(term);

        shell.setWelcome("welcome");
        shell.init();
        shell.readline();

        assertThat(term.output()).isEmpty();
        assertThat(term.readlinePrompt()).isEqualTo("[arthas@123]$ ");
        assertThat((Object) shell.session().get(Session.QUIET)).isEqualTo(Boolean.TRUE);
    }

    private ShellImpl newShell(Term term) {
        InternalCommandManager commandManager =
                new InternalCommandManager(Collections.<CommandResolver>emptyList());
        return new ShellImpl((ShellServer) null, term, commandManager, null, 123L, new JobControllerImpl());
    }

    private static final class RecordingTerm implements Term {
        private final String type;
        private final StringBuilder output = new StringBuilder();
        private Session session;
        private String readlinePrompt;

        private RecordingTerm(String type) {
            this.type = type;
        }

        @Override
        public Term resizehandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public Term stdinHandler(Handler<String> handler) {
            return this;
        }

        @Override
        public Term stdoutHandler(Function<String, String> handler) {
            return this;
        }

        @Override
        public Term write(String data) {
            output.append(data);
            return this;
        }

        @Override
        public long lastAccessedTime() {
            return System.currentTimeMillis();
        }

        @Override
        public Term echo(String text) {
            output.append(text);
            return this;
        }

        @Override
        public Term setSession(Session session) {
            this.session = session;
            return this;
        }

        @Override
        public Term interruptHandler(SignalHandler handler) {
            return this;
        }

        @Override
        public Term suspendHandler(SignalHandler handler) {
            return this;
        }

        @Override
        public void readline(String prompt, Handler<String> lineHandler) {
            this.readlinePrompt = prompt;
        }

        @Override
        public void readline(String prompt, Handler<String> lineHandler, Handler<Completion> completionHandler) {
            this.readlinePrompt = prompt;
        }

        @Override
        public Term closeHandler(Handler<Void> handler) {
            return this;
        }

        @Override
        public void close() {
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public int width() {
            return 80;
        }

        @Override
        public int height() {
            return 24;
        }

        private String output() {
            return output.toString();
        }

        private String readlinePrompt() {
            return readlinePrompt;
        }
    }
}
