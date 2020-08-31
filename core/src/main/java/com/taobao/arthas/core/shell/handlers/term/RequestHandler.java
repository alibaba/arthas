package com.taobao.arthas.core.shell.handlers.term;

import com.taobao.arthas.core.shell.handlers.Handler;
import com.taobao.arthas.core.shell.term.impl.TermImpl;
import io.netty.channel.ChannelHandlerContext;
import io.termd.core.function.Consumer;

/**
 * @author beiwei30 on 23/11/2016.
 */
public class RequestHandler implements Consumer<String> {
    private TermImpl term;
    private final Handler<String> lineHandler;

    public RequestHandler(TermImpl term, Handler<String> lineHandler) {
        this.term = term;
        this.lineHandler = lineHandler;
    }

    /**
     * #### telnet收到命令后，
     * 通过中间件处理(比如{@link io.termd.core.telnet.netty.TelnetChannelHandler#channelRead(ChannelHandlerContext, Object)})
     * 最终调到这里
     * @param line
     */
    @Override
    public void accept(String line) {
        term.setInReadline(false);
        lineHandler.handle(line);
    }
}
