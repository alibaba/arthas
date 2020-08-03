package com.taobao.arthas.core.shell.term.impl.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

import java.io.File;


/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyServerInitializer extends ChannelInitializer<SocketChannel> {

  private final ChannelGroup group;
  private final Consumer<TtyConnection> handler;
  private EventExecutorGroup workerGroup;

  public TtyServerInitializer(ChannelGroup group, Consumer<TtyConnection> handler, EventExecutorGroup workerGroup) {
      this.group = group;
      this.handler = handler;
    this.workerGroup = workerGroup;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {

    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new ChunkedWriteHandler());
    pipeline.addLast(new HttpObjectAggregator(64 * 1024));
    pipeline.addLast(workerGroup, "HttpRequestHandler", new HttpRequestHandler("/ws", new File("arthas-output")));
    pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
    pipeline.addLast(new TtyWebSocketFrameHandler(group, handler));
  }
}
