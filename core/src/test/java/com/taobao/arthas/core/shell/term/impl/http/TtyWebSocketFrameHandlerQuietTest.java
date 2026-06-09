package com.taobao.arthas.core.shell.term.impl.http;

import com.taobao.arthas.core.shell.session.Session;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
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
    void isQuietRequestShouldReadFallbackUriFromChannelAttribute() {
        EmbeddedChannel channel = new EmbeddedChannel(new ChannelInboundHandlerAdapter());
        ChannelHandlerContext context = channel.pipeline().firstContext();
        channel.attr(TtyWebSocketFrameHandler.REQUEST_URI).set("/ws?quiet=true");

        assertThat(TtyWebSocketFrameHandler.isQuietRequest(context, null)).isTrue();
        assertThat(TtyWebSocketFrameHandler.isQuietRequest(context, "/ws?quiet=false")).isFalse();
    }

    @Test
    void channelReadShouldCloseChannelWhenHandshakeIsNotCompleted() {
        EmbeddedChannel channel = new EmbeddedChannel(new TtyWebSocketFrameHandler(
                new DefaultChannelGroup(GlobalEventExecutor.INSTANCE),
                connection -> {
                }));

        channel.writeInbound(new TextWebSocketFrame("help"));

        assertThat(channel.isOpen()).isFalse();
    }

    @Test
    void extSessionsShouldCarryQuietFlag() {
        Map<String, Object> extSessions = new ExtHttpTtyConnection(null, true).extSessions();

        assertThat(extSessions).containsEntry(Session.QUIET, Boolean.TRUE);
    }
}
