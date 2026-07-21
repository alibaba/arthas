package com.alibaba.arthas.tunnel.server;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;

public class TunnelSocketFrameHandlerTest {

    @Test
    public void shouldSendAValidCloseFrameWhenAgentIsMissing() {
        EmbeddedChannel channel = new EmbeddedChannel(new TunnelSocketFrameHandler(new TunnelServer()));
        try {
            channel.pipeline().fireUserEventTriggered(new HandshakeComplete("/ws?method=connectArthas&id=missing",
                    EmptyHttpHeaders.INSTANCE, null));

            Assertions.assertThatThrownBy(channel::checkException).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Can not find arthas agent by id: [missing]");

            CloseWebSocketFrame closeFrame = channel.readOutbound();
            try {
                Assertions.assertThat(closeFrame).isNotNull();
                Assertions.assertThat(closeFrame.statusCode()).isEqualTo(4000);
                Assertions.assertThat(WebSocketCloseStatus.isValidStatusCode(closeFrame.statusCode())).isTrue();
                Assertions.assertThat(closeFrame.reasonText()).isEqualTo("Can not find arthas agent by id: [missing]");
            } finally {
                if (closeFrame != null) {
                    closeFrame.release();
                }
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
