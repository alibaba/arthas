package com.taobao.arthas.core.shell.term.impl.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.termd.core.function.Consumer;
import io.termd.core.tty.TtyConnection;

import com.taobao.arthas.common.ArthasConstants;

/**
 * 
 * @author hengyunabc 2020-09-02
 *
 */
public class LocalTtyServerInitializer extends ChannelInitializer<LocalChannel> {

    private final ChannelGroup group;
    private final Consumer<TtyConnection> handler;
    private EventExecutorGroup workerGroup;

    public LocalTtyServerInitializer(ChannelGroup group, Consumer<TtyConnection> handler,
            EventExecutorGroup workerGroup) {
        this.group = group;
        this.handler = handler;
        this.workerGroup = workerGroup;
    }

    @Override
    protected void initChannel(LocalChannel ch) throws Exception {

        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpObjectAggregator(ArthasConstants.MAX_HTTP_CONTENT_LENGTH));
        pipeline.addLast(workerGroup, "HttpRequestHandler", new HttpRequestHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH));
        pipeline.addLast(new WebSocketServerProtocolHandler(ArthasConstants.DEFAULT_WEBSOCKET_PATH, true));
        pipeline.addLast(new IdleStateHandler(0, 0, ArthasConstants.WEBSOCKET_IDLE_SECONDS));
        pipeline.addLast(new TtyWebSocketFrameHandler(group, handler));
    }
}
