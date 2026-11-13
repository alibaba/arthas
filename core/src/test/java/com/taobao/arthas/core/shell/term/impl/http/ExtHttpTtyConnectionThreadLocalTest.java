package com.taobao.arthas.core.shell.term.impl.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.InternalThreadLocalMap;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ExtHttpTtyConnectionThreadLocalTest {

    /**
     * watch/trace output is written from business threads. Writing must not leave Netty thread-local state on
     * them: it would keep ArthasClassLoader reachable from those threads after stop.
     */
    @Test
    void writeFromOtherThreadShouldNotLeaveNettyThreadLocalState() throws Exception {
        EventLoopGroup group = new DefaultEventLoopGroup(1);
        LocalAddress address = new LocalAddress("ext-http-tty-connection-thread-local-test");
        BlockingQueue<String> received = new LinkedBlockingQueue<String>();
        CompletableFuture<ChannelHandlerContext> clientContext = new CompletableFuture<ChannelHandlerContext>();
        try {
            new ServerBootstrap().group(group).channel(LocalServerChannel.class)
                    .childHandler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) {
                            try {
                                received.add(((TextWebSocketFrame) msg).text());
                            } finally {
                                ReferenceCountUtil.release(msg);
                            }
                        }
                    }).bind(address).sync();
            Channel client = new Bootstrap().group(group).channel(LocalChannel.class)
                    .handler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void handlerAdded(ChannelHandlerContext ctx) {
                            clientContext.complete(ctx);
                        }
                    }).connect(address).sync().channel();
            ExtHttpTtyConnection connection = new ExtHttpTtyConnection(clientContext.get(5, TimeUnit.SECONDS));

            AtomicBoolean threadLocalStateLeft = new AtomicBoolean();
            Thread businessThread = new Thread(() -> {
                connection.write("hello".getBytes(StandardCharsets.UTF_8));
                threadLocalStateLeft.set(InternalThreadLocalMap.getIfSet() != null);
            });
            businessThread.start();
            businessThread.join(5000);

            assertThat(received.poll(5, TimeUnit.SECONDS)).isEqualTo("hello");
            assertThat(threadLocalStateLeft.get()).isFalse();
            client.close().sync();
        } finally {
            group.shutdownGracefully(0, 1, TimeUnit.SECONDS).sync();
        }
    }
}
