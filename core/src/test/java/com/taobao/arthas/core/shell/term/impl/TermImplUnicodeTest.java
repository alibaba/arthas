package com.taobao.arthas.core.shell.term.impl;

import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;
import io.termd.core.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TermImplUnicodeTest {

    @Test
    void readlineShouldPreserveChineseInput() {
        AtomicReference<Consumer<int[]>> stdinHandler = new AtomicReference<Consumer<int[]>>();
        List<Integer> output = new ArrayList<Integer>();
        TtyConnection connection = mock(TtyConnection.class);
        when(connection.size()).thenReturn(new Vector(80, 24));
        when(connection.getStdinHandler()).thenAnswer(invocation -> stdinHandler.get());
        when(connection.stdoutHandler()).thenReturn(codePoints -> {
            for (int codePoint : codePoints) {
                output.add(codePoint);
            }
        });
        doAnswer(invocation -> {
            stdinHandler.set(invocation.getArgument(0));
            return null;
        }).when(connection).setStdinHandler(any());

        TermImpl term = new TermImpl(connection);
        AtomicReference<String> line = new AtomicReference<String>();
        term.readline("$ ", line::set);

        stdinHandler.get().accept(Helper.toCodePoints("echo 中文\r"));

        assertThat(line).hasValue("echo 中文");
        assertThat(output).doesNotContain(7);
    }
}
