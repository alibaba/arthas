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
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.termd.core.function.Consumer;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.tty.TtyConnection;


/**
 * TTY WebSocket 帧处理器
 *
 * 该处理器用于处理 WebSocket 连接的文本帧，并将其转换为 TTY 连接。
 * 它是 WebSocket 协议处理链中的最后一环，负责实际的终端交互。
 *
 * 主要功能：
 * 1. 处理 WebSocket 握手完成事件，创建 TTY 连接
 * 2. 处理接收到的 WebSocket 文本帧，将其写入 TTY 解码器
 * 3. 处理连接空闲事件，发送 Ping 帧保持连接活跃
 * 4. 处理连接断开事件，清理资源并通知关闭处理器
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyWebSocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

  /**
   * 通道组，用于管理所有活跃的 WebSocket 连接
   */
  private final ChannelGroup group;

  /**
   * TTY 连接处理器
   * 当新的 TTY 连接创建时，会调用该处理器
   */
  private final Consumer<TtyConnection> handler;

  /**
   * 通道处理器上下文
   * 用于向客户端发送消息和执行其他通道操作
   */
  private ChannelHandlerContext context;

  /**
   * HTTP TTY 连接
   * 封装了 WebSocket 连接，提供 TTY 终端功能
   */
  private HttpTtyConnection conn;

  /**
   * 构造 TTY WebSocket 帧处理器
   *
   * @param group 通道组，用于管理所有活跃的 WebSocket 连接
   * @param handler TTY 连接处理器，处理新的 TTY 连接
   */
  public TtyWebSocketFrameHandler(ChannelGroup group, Consumer<TtyConnection> handler) {
    this.group = group;
    this.handler = handler;
  }

  /**
   * 当通道激活时调用
   *
   * 该方法在 WebSocket 连接建立后被调用（注意：此时握手可能还未完成）。
   * 保存通道上下文，以便后续使用。
   *
   * @param ctx 通道处理器上下文
   * @throws Exception 可能抛出的异常
   */
  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    // 保存通道上下文，用于后续向客户端发送消息
    context = ctx;
  }

  /**
   * 当用户事件触发时调用
   *
   * 该方法处理以下事件：
   * 1. WebSocket 握手完成事件：创建 TTY 连接并调用处理器
   * 2. 空闲状态事件：发送 Ping 帧保持连接活跃
   * 3. 其他事件：传递给父类处理
   *
   * @param ctx 通道处理器上下文
   * @param evt 用户事件
   * @throws Exception 可能抛出的异常
   */
  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    // 检查是否是 WebSocket 握手完成事件
    if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
      // 握手完成后，不再需要 HTTP 请求处理器，从管道中移除
      ctx.pipeline().remove(HttpRequestHandler.class);
      // 将当前通道添加到通道组中
      group.add(ctx.channel());
      // 创建扩展的 HTTP TTY 连接
      conn = new ExtHttpTtyConnection(context);
      // 调用处理器处理新的 TTY 连接
      handler.accept(conn);
    } else if (evt instanceof IdleStateEvent) {
      // 如果是空闲状态事件，发送 Ping 帧
      // 这用于保持连接活跃，检测连接是否仍然有效
      ctx.writeAndFlush(new PingWebSocketFrame());
    } else {
      // 其他事件传递给父类处理
      super.userEventTriggered(ctx, evt);
    }
  }

  /**
   * 当通道变为非活跃状态时调用
   *
   * 该方法在连接断开时被调用，负责清理资源并通知关闭处理器。
   *
   * @param ctx 通道处理器上下文
   * @throws Exception 可能抛出的异常
   */
  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    // 保存连接的引用，避免在清理过程中被修改
    HttpTtyConnection tmp = conn;
    // 清空上下文和连接引用
    context = null;
    conn = null;
    // 如果连接存在，调用关闭处理器
    if (tmp != null) {
      // 获取关闭处理器
      Consumer<Void> closeHandler = tmp.getCloseHandler();
      // 如果关闭处理器存在，调用它
      if (closeHandler != null) {
        closeHandler.accept(null);
      }
    }
  }

  /**
   * 读取 WebSocket 文本帧
   *
   * 该方法在接收到 WebSocket 文本帧时被调用。
   * 将文本帧的内容写入 TTY 解码器进行处理。
   *
   * @param ctx 通道处理器上下文
   * @param msg WebSocket 文本帧
   * @throws Exception 可能抛出的异常
   */
  @Override
  public void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    // 将文本帧的内容写入 TTY 解码器
    // 解码器会将文本解析为终端命令并执行
    conn.writeToDecoder(msg.text());
  }
}
