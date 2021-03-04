package com.taobao.arthas.core.shell.term.impl.http.session;

import java.util.Collections;
import java.util.Set;

import com.taobao.arthas.common.ArthasConstants;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * <pre>
 * netty里的http session管理。因为同一域名的不同端口共享cookie，所以需要共用。
 * </pre>
 * 
 * @author hengyunabc 2021-03-03
 *
 */
public class HttpSessionManager {
    public static AttributeKey<HttpSession> SESSION_KEY = AttributeKey.valueOf("session");

    private LRUCache<String, HttpSession> sessions = new LRUCache<String, HttpSession>(1024);

    public HttpSessionManager() {

    }

    private HttpSession getSession(HttpRequest httpRequest) {
        // TODO 增加从 url中获取 session id 功能？

        Set<Cookie> cookies;
        String value = httpRequest.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        for (Cookie cookie : cookies) {
            if (ArthasConstants.ASESSION_KEY.equals(cookie.name())) {
                String sessionId = cookie.value();
                return sessions.get(sessionId);
            }
        }
        return null;
    }

    public static HttpSession getHttpSessionFromContext(ChannelHandlerContext ctx) {
        return ctx.channel().attr(SESSION_KEY).get();
    }

    public HttpSession getOrCreateHttpSession(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        // 尝试用 ctx 和从 cookie里读取出 session
        Attribute<HttpSession> attribute = ctx.channel().attr(SESSION_KEY);
        HttpSession httpSession = attribute.get();
        if (httpSession != null) {
            return httpSession;
        }
        httpSession = getSession(httpRequest);
        if (httpSession != null) {
            attribute.set(httpSession);
            return httpSession;
        }
        // 创建session，并设置到ctx里
        httpSession = newHttpSession();
        attribute.set(httpSession);
        return httpSession;
    }

    private HttpSession newHttpSession() {
        SimpleHttpSession session = new SimpleHttpSession();
        this.sessions.put(session.getId(), session);
        return session;
    }

    public static void setSessionCookie(HttpResponse response, HttpSession session) {
        response.headers().add(HttpHeaderNames.SET_COOKIE,
                ServerCookieEncoder.STRICT.encode(ArthasConstants.ASESSION_KEY, session.getId()));
    }

    public void start() {

    }

    public void stop() {
        sessions.clear();
    }

}
