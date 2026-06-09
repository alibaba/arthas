/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taobao.arthas.core.shell.term.impl.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.termd.core.function.Consumer;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.tty.TtyConnection;

import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  private final ChannelGroup group;
  private final Consumer<TtyConnection> handler;
  private ChannelHandlerContext context;
  private HttpTtyConnection conn;

  public TtyWebSocketFrameHandler(ChannelGroup group, Consumer<TtyConnection> handler) {
    this.group = group;
    this.handler = handler;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    context = ctx;
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
      // Netty 会先发旧事件，再发带 requestUri 的 HandshakeComplete；这里延迟兜底，优先读取 query。
      ctx.executor().execute(new Runnable() {
        @Override
        public void run() {
          handleHandshakeComplete(ctx, null);
        }
      });
    } else if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
      WebSocketServerProtocolHandler.HandshakeComplete handshakeComplete =
          (WebSocketServerProtocolHandler.HandshakeComplete) evt;
      handleHandshakeComplete(ctx, handshakeComplete.requestUri());
    } else if (evt instanceof IdleStateEvent) {
      ctx.writeAndFlush(new PingWebSocketFrame());
    } else {
      super.userEventTriggered(ctx, evt);
    }
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    HttpTtyConnection tmp = conn;
    context = null;
    conn = null;
    if (tmp != null) {
      Consumer<Void> closeHandler = tmp.getCloseHandler();
      if (closeHandler != null) {
        closeHandler.accept(null);
      }
    }
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    conn.writeToDecoder(msg.text());
  }

  private void handleHandshakeComplete(ChannelHandlerContext ctx, String requestUri) {
    if (conn != null) {
      return;
    }
    ctx.pipeline().remove(HttpRequestHandler.class);
    group.add(ctx.channel());
    conn = new ExtHttpTtyConnection(context, isQuietRequest(requestUri));
    handler.accept(conn);
  }

  static boolean isQuietRequest(String requestUri) {
    if (requestUri == null) {
      return false;
    }
    List<String> values = new QueryStringDecoder(requestUri).parameters().get("quiet");
    if (values == null) {
      return false;
    }
    for (String value : values) {
      if ("true".equalsIgnoreCase(value)) {
        return true;
      }
    }
    return false;
  }
}
