package com.taobao.arthas.core.shell.term.impl.http;

import java.nio.charset.Charset;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.security.AuthUtils;
import com.taobao.arthas.core.security.BasicPrincipal;
import com.taobao.arthas.core.security.SecurityAuthenticator;
import com.taobao.arthas.core.server.ArthasBootstrap;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSession;
import com.taobao.arthas.core.shell.term.impl.http.session.HttpSessionManager;
import com.taobao.arthas.core.util.StringUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.Attribute;

/**
 * 
 * @author hengyunabc 2021-03-03
 *
 */
public final class BasicHttpAuthenticatorHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(BasicHttpAuthenticatorHandler.class);

    private HttpSessionManager httpSessionManager;

    private SecurityAuthenticator securityAuthenticator = ArthasBootstrap.getInstance().getSecurityAuthenticator();

    public BasicHttpAuthenticatorHandler(HttpSessionManager httpSessionManager) {
        this.httpSessionManager = httpSessionManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!securityAuthenticator.needLogin()) {
            ctx.fireChannelRead(msg);
            return;
        }

        boolean authed = false;
        if (msg instanceof HttpRequest) {
            HttpRequest httpRequest = (HttpRequest) msg;

            // 判断session里是否有已登陆信息
            HttpSession session = httpSessionManager.getOrCreateHttpSession(ctx, httpRequest);
            if (session != null && session.getAttribute(ArthasConstants.SUBJECT_KEY) != null) {
                authed = true;
            }

            Principal principal = null;
            if (!authed) {
                // 判断请求header里是否带有 username/password
                principal = extractBasicAuthSubject(httpRequest);
                if (principal == null) {
                    // 判断 url里是否有 username/password
                    principal = extractBasicAuthSubjectFromUrl(httpRequest);
                }
            }
            if (!authed && principal == null) {
                // 判断是否本地连接
                principal = AuthUtils.localPrincipal(ctx);
            }
            Subject subject = securityAuthenticator.login(principal);
            if (subject != null) {
                authed = true;
                if (session != null) {
                    session.setAttribute(ArthasConstants.SUBJECT_KEY, subject);
                }
            }

            if (!authed) {
                // restricted resource, so send back 401 to require valid username/password
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);
                response.headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"arthas webconsole\"");
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

                ctx.writeAndFlush(response);
                // close the channel
                ctx.channel().close();
                return;
            }

        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse) {
            // write cookie
            HttpResponse response = (HttpResponse) msg;
            Attribute<HttpSession> attribute = ctx.channel().attr(HttpSessionManager.SESSION_KEY);
            HttpSession session = attribute.get();
            if (session != null) {
                HttpSessionManager.setSessionCookie(response, session);
            }
        }
        super.write(ctx, msg, promise);
    }

    /**
     * 从url参数里提取 ?username=hello&password=world
     * 
     * @param request
     * @return
     */
    protected static BasicPrincipal extractBasicAuthSubjectFromUrl(HttpRequest request) {
        QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> parameters = queryDecoder.parameters();

        List<String> passwords = parameters.get(ArthasConstants.PASSWORD_KEY);
        if (passwords == null || passwords.size() == 0) {
            return null;
        }
        String password = passwords.get(0);

        String username = ArthasConstants.DEFAULT_USERNAME;
        List<String> usernames = parameters.get(ArthasConstants.USERNAME_KEY);
        if (usernames != null && !usernames.isEmpty()) {
            username = usernames.get(0);
        }
        BasicPrincipal principal = new BasicPrincipal(username, password);
        logger.debug("Extracted Basic Auth principal from url: {}", principal);
        return principal;
    }

    /**
     * Extracts the username and password details from the HTTP basic header
     * Authorization.
     * <p/>
     * This requires that the <tt>Authorization</tt> HTTP header is provided, and
     * its using Basic. Currently Digest is <b>not</b> supported.
     *
     * @return {@link HttpPrincipal} with username and password details, or
     *         <tt>null</tt> if not possible to extract
     */
    protected static BasicPrincipal extractBasicAuthSubject(HttpRequest request) {
        String auth = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (auth != null) {
            String constraint = StringUtils.before(auth, " ");
            if (constraint != null) {
                if ("Basic".equalsIgnoreCase(constraint.trim())) {
                    String decoded = StringUtils.after(auth, " ");
                    if (decoded == null) {
                        logger.error("Extracted Basic Auth principal failed, bad auth String: {}", auth);
                        return null;
                    }
                    // the decoded part is base64 encoded, so we need to decode that
                    ByteBuf buf = Unpooled.wrappedBuffer(decoded.getBytes());
                    ByteBuf out = Base64.decode(buf);
                    String userAndPw = out.toString(Charset.defaultCharset());
                    String username = StringUtils.before(userAndPw, ":");
                    String password = StringUtils.after(userAndPw, ":");
                    BasicPrincipal principal = new BasicPrincipal(username, password);
                    logger.debug("Extracted Basic Auth principal from HTTP header: {}", principal);
                    return principal;
                }
            }
        }
        return null;
    }

}
