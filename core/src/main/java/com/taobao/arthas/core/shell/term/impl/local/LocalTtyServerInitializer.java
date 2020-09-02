package com.taobao.arthas.core.shell.term.impl.local;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;


public class LocalTtyServerInitializer extends ChannelInitializer<LocalChannel> {

  private final ChannelGroup group;
  private final Consumer<TtyConnection> handler;
  private EventExecutorGroup workerGroup;

  public LocalTtyServerInitializer(ChannelGroup group, Consumer<TtyConnection> handler, EventExecutorGroup workerGroup) {
      this.group = group;
      this.handler = handler;
    this.workerGroup = workerGroup;
  }

  @Override
  protected void initChannel(LocalChannel ch) throws Exception {

    ChannelPipeline pipeline = ch.pipeline();
//    pipeline.addLast(new HttpServerCodec());
//    pipeline.addLast(new ChunkedWriteHandler());
//    pipeline.addLast(new HttpObjectAggregator(64 * 1024));
//    pipeline.addLast(workerGroup, "HttpRequestHandler", new HttpRequestHandler("/ws", new File("arthas-output")));
//    pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
//    pipeline.addLast(new TtyWebSocketFrameHandler(group, handler));

    pipeline.addLast(new LocalTtyChannelHandler(group, handler));

  }
}
