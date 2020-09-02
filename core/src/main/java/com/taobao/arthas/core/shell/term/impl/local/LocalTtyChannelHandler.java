package com.taobao.arthas.core.shell.term.impl.local;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.termd.core.function.Consumer;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.tty.TtyConnection;

import java.util.concurrent.TimeUnit;

/**
 * @author gongdewei 2020/9/1
 */
public class LocalTtyChannelHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {


    private final ChannelGroup group;
    private final Consumer<TtyConnection> handler;
    private HttpTtyConnection conn;
    private ChannelHandlerContext context;

    public LocalTtyChannelHandler(ChannelGroup group, Consumer<TtyConnection> handler) {
        this.group = group;
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        context = ctx;
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
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        conn.writeToDecoder(msg.text());
    }
}
