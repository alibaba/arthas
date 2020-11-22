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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.termd.core.function.Consumer;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.tty.TtyConnection;

import java.util.concurrent.TimeUnit;

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
      ctx.pipeline().remove(HttpRequestHandler.class);
      group.add(ctx.channel());
      conn = new HttpTtyConnection() {
        @Override
        protected void write(byte[] buffer) {
          ByteBuf byteBuf = Unpooled.buffer();
          byteBuf.writeBytes(buffer);
          if (context != null) {
            context.writeAndFlush(new TextWebSocketFrame(byteBuf));
          }
        }

        @Override
        public void schedule(Runnable task, long delay, TimeUnit unit) {
          if (context != null) {
            context.executor().schedule(task, delay, unit);
          }
        }

        @Override
        public void execute(Runnable task) {
          if (context != null) {
            context.executor().execute(task);
          }
        }

        @Override
        public void close() {
          if (context != null) {
            context.close();
          }
        }
      };
      handler.accept(conn);
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

  public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    conn.writeToDecoder(msg.text());
  }
}
