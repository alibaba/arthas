package com.taobao.arthas.core.shell.term.impl.http;

import com.taobao.arthas.core.shell.session.Session;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TtyWebSocketFrameHandlerQuietTest {

    @Test
    void isQuietRequestShouldOnlyAcceptQuietTrue() {
        assertThat(TtyWebSocketFrameHandler.isQuietRequest("/ws?quiet=true")).isTrue();
        assertThat(TtyWebSocketFrameHandler.isQuietRequest("/ws?quiet=TRUE")).isTrue();
        assertThat(TtyWebSocketFrameHandler.isQuietRequest("/ws?quiet=false")).isFalse();
        assertThat(TtyWebSocketFrameHandler.isQuietRequest("/ws")).isFalse();
        assertThat(TtyWebSocketFrameHandler.isQuietRequest(null)).isFalse();
    }

    @Test
    void extSessionsShouldCarryQuietFlag() {
        Map<String, Object> extSessions = new ExtHttpTtyConnection(null, true).extSessions();

        assertThat(extSessions).containsEntry(Session.QUIET, Boolean.TRUE);
    }
}
