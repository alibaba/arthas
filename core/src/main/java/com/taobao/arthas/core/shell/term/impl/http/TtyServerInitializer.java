package com.taobao.arthas.core.shell.term.impl.http;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;


/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyServerInitializer extends ChannelInitializer<SocketChannel> {

  private final ChannelGroup group;
  private final Consumer<TtyConnection> handler;
  private EventExecutorGroup workerGroup;
  private HttpSessionManager httpSessionManager;

  public TtyServerInitializer(ChannelGroup group, Consumer<TtyConnection> handler, EventExecutorGroup workerGroup, HttpSessionManager httpSessionManager) {
      this.group = group;
      this.handler = handler;
      this.workerGroup = workerGroup;
      this.httpSessionManager = httpSessionManager;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {

    ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new HttpServerCodec());
    pipeline.addLast(new ChunkedWriteHandler());
    pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));
    pipeline.addLast(new BasicHttpAuthenticatorHandler(httpSessionManager));
    pipeline.addLast(workerGroup, "HttpRequestHandler", new HttpRequestHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH));
    pipeline.addLast(new WebSocketServerProtocolHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH, null, false, ArthasConstants.MAX_HTTP_CONTENT_LENGTH, false, true));
    pipeline.addLast(new IdleStateHandler(0, 0, ArthasConstants.WEBSOCKET_IDLE_SECONDS));
    pipeline.addLast(new TtyWebSocketFrameHandler(group, handler));
  }
}
