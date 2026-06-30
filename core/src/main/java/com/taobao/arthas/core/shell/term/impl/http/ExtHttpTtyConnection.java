package com.taobao.arthas.core.shell.term.impl.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.shell.session.Session;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSession;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.termd.core.http.HttpTtyConnection;

/**
 * 从http请求传递过来的 session 信息。解析websocket创建的 term 还需要登录验证问题
 * 
 * @author hengyunabc 2021-03-04
 *
 */
public class ExtHttpTtyConnection extends HttpTtyConnection {
    private ChannelHandlerContext context;
    private final boolean quiet;

    public ExtHttpTtyConnection(ChannelHandlerContext context) {
        this(context, false);
    }

    public ExtHttpTtyConnection(ChannelHandlerContext context, boolean quiet) {
        this.context = context;
        this.quiet = quiet;
    }

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

    public Map<String, Object> extSessions() {
        Map<String, Object> result = new HashMap<String, Object>();
        if (quiet) {
            result.put(Session.QUIET, Boolean.TRUE);
        }
        if (context != null) {
            HttpSession httpSession = HttpSessionManager.getHttpSessionFromContext(context);
            if (httpSession != null) {
                Object subject = httpSession.getAttribute(ArthasConstants.SUBJECT_KEY);
                if (subject != null) {
                    result.put(ArthasConstants.SUBJECT_KEY, subject);
                }
                // pass userId from httpSession to arthas session
                Object userId = httpSession.getAttribute(ArthasConstants.USER_ID_KEY);
                if (userId != null) {
                    result.put(ArthasConstants.USER_ID_KEY, userId);
                }
            }
        }
        if (!result.isEmpty()) {
            return result;
        }
        return Collections.emptyMap();
    }

}
