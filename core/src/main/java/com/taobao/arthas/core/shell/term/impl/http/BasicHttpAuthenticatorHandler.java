package com.taobao.arthas.core.shell.term.impl.http;

import com.alibaba.arthas.deps.org.slf4j.Logger;
import com.alibaba.arthas.deps.org.slf4j.LoggerFactory;
import com.taobao.arthas.common.ArthasConstants;
import com.taobao.arthas.core.security.AuthUtils;
import com.taobao.arthas.core.security.BasicPrincipal;
import com.taobao.arthas.core.security.BearerPrincipal;
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
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;

import javax.security.auth.Subject;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.List;
import java.util.Map;

import static com.taobao.arthas.mcp.server.util.McpAuthExtractor.SUBJECT_ATTRIBUTE_KEY;


/**
 * 基础 HTTP 认证处理器
 * <p>
 * 该处理器负责处理 Arthas HTTP 接口的认证逻辑，支持以下认证方式：
 * <ul>
 *   <li>HTTP Basic Auth：通过 Authorization header 中的 Basic 认证</li>
 *   <li>Bearer Token：通过 Authorization header 中的 Bearer 认证（用于 MCP 请求）</li>
 *   <li>URL 参数认证：通过 URL 参数中的 username 和 password 进行认证</li>
 *   <li>本地连接认证：对于本地连接自动通过认证</li>
 * </ul>
 * </p>
 * <p>
 * 该处理器继承自 ChannelDuplexHandler，可以同时处理入站和出站事件
 * </p>
 *
 * @author hengyunabc 2021-03-03
 */
public final class BasicHttpAuthenticatorHandler extends ChannelDuplexHandler {
    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(BasicHttpAuthenticatorHandler.class);

    /**
     * HTTP 会话管理器
     * <p>
     * 用于创建、获取和管理 HTTP 会话
     * </p>
     */
    private HttpSessionManager httpSessionManager;

    /**
     * 安全认证器
     * <p>
     * 从 ArthasBootstrap 获取，用于执行实际的用户认证逻辑
     * </p>
     */
    private SecurityAuthenticator securityAuthenticator = ArthasBootstrap.getInstance().getSecurityAuthenticator();

    /**
     * 构造函数
     *
     * @param httpSessionManager HTTP 会话管理器，用于管理 HTTP 会话
     */
    public BasicHttpAuthenticatorHandler(HttpSessionManager httpSessionManager) {
        this.httpSessionManager = httpSessionManager;
    }

    /**
     * 处理入站消息（读取请求）
     * <p>
     * 该方法实现了 HTTP 认证的核心逻辑：
     * <ol>
     *   <li>过滤非 HTTP 请求消息</li>
     *   <li>从 URL 中提取 userId 并存入会话</li>
     *   <li>检查是否需要登录认证</li>
     *   <li>尝试从会话中获取已认证的主体</li>
     *   <li>如果未认证，尝试从多种方式提取认证信息并登录</li>
     *   <li>如果认证失败，返回 401 未授权响应</li>
     * </ol>
     * </p>
     *
     * @param ctx 通道处理器上下文
     * @param msg 读取到的消息对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 先处理非 HttpRequest 消息，直接传递给下一个处理器
        if (!(msg instanceof HttpRequest)) {
            ctx.fireChannelRead(msg);
            return;
        }

        // 转换为 HTTP 请求对象
        HttpRequest httpRequest = (HttpRequest) msg;
        // 获取或创建 HTTP 会话
        HttpSession session = httpSessionManager.getOrCreateHttpSession(ctx, httpRequest);

        // 无论是否需要登录认证，都从 URL 中提取 userId
        extractAndSetUserIdFromUrl(httpRequest, session);

        // 检查是否需要登录认证
        if (!securityAuthenticator.needLogin()) {
            ctx.fireChannelRead(msg);
            return;
        }

        // 标记是否已认证
        boolean authed = false;

        // 判断 session 里是否有已登录信息
        if (session != null) {
            Object subjectObj = session.getAttribute(ArthasConstants.SUBJECT_KEY);
            if (subjectObj != null) {
                // 会话中已有认证主体
                authed = true;
                setAuthenticatedSubject(ctx, session, subjectObj);
            }
        }

        Principal principal = null;
        // 判断是否为 MCP 请求
        boolean isMcpRequest = isMcpRequest(httpRequest);

        // 如果会话中未认证，尝试从请求中提取认证信息
        if (!authed) {
            if (isMcpRequest) {
                // MCP 请求：支持 Bearer Token 和 Basic Auth
                principal = extractMcpAuthSubject(httpRequest);
            } else {
                // 普通 Web 请求：优先使用 Basic Auth
                principal = extractBasicAuthSubject(httpRequest);
                if (principal == null) {
                    // 如果 header 中没有，尝试从 URL 参数中提取
                    principal = extractBasicAuthSubjectFromUrl(httpRequest);
                }
            }
            if (principal == null) {
                // 如果以上方式都没有提取到认证信息，判断是否为本地连接
                principal = AuthUtils.localPrincipal(ctx);
            }
            // 使用认证器进行登录认证
            Subject subject = securityAuthenticator.login(principal);
            if (subject != null) {
                // 认证成功
                authed = true;
                setAuthenticatedSubject(ctx, session, subject);
            }
        }

        // 如果仍未认证，返回 401 未授权响应
        if (!authed) {
            // restricted resource, so send back 401 to require valid username/password
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);

            if (isMcpRequest) {
                // MCP 请求支持 Bearer 和 Basic 两种认证方式
                response.headers()
                        .add(HttpHeaderNames.WWW_AUTHENTICATE, "Bearer realm=\"arthas mcp\"")
                        .add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"arthas mcp\"");
            } else {
                // 普通 Web 控制台只支持 Basic 认证
                response.headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"arthas webconsole\"");
            }

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);

            ctx.writeAndFlush(response);
            // close the channel
            ctx.channel().close();
            return;
        }

        // 认证成功，将请求传递给下一个处理器
        ctx.fireChannelRead(msg);
    }

    /**
     * 设置已认证的主体信息
     * <p>
     * 将认证成功的主体信息设置到通道和会话中，以便后续使用
     * </p>
     *
     * @param ctx     通道处理器上下文
     * @param session HTTP 会话
     * @param subject 认证主体对象
     */
    private void setAuthenticatedSubject(ChannelHandlerContext ctx, HttpSession session, Object subject) {
        // 将主体设置到通道属性中，供后续处理器使用
        ctx.channel().attr(SUBJECT_ATTRIBUTE_KEY).set(subject);
        // 同时将主体设置到会话属性中，以便会话期间保持认证状态
        if (session != null) {
            session.setAttribute(ArthasConstants.SUBJECT_KEY, subject);
        }
    }

    /**
     * 处理出站消息（写入响应）
     * <p>
     * 在发送 HTTP 响应时，将会话 cookie 设置到响应头中
     * </p>
     *
     * @param ctx     通道处理器上下文
     * @param msg     要写入的消息对象
     * @param promise 写入操作的 Promise
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof HttpResponse) {
            // write cookie
            HttpResponse response = (HttpResponse) msg;
            // 从通道属性中获取会话
            Attribute<HttpSession> attribute = ctx.channel().attr(HttpSessionManager.SESSION_KEY);
            HttpSession session = attribute.get();
            if (session != null) {
                // 设置会话 cookie 到响应头
                HttpSessionManager.setSessionCookie(response, session);
            }
        }
        // 调用父类方法继续处理写入操作
        super.write(ctx, msg, promise);
    }

    /**
     * 从 URL 参数里提取 userId 并存入 HttpSession
     * <p>
     * 从请求的 URL 查询参数中提取 userId 参数值，并存入会话属性中
     * </p>
     *
     * @param request HTTP 请求对象
     * @param session HTTP 会话对象
     */
    protected static void extractAndSetUserIdFromUrl(HttpRequest request, HttpSession session) {
        if (session == null) {
            return;
        }
        // 解码 URL 查询参数
        QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> parameters = queryDecoder.parameters();

        // 获取 userId 参数
        List<String> userIds = parameters.get(ArthasConstants.USER_ID_KEY);
        if (userIds != null && !userIds.isEmpty()) {
            String userId = userIds.get(0);
            if (!StringUtils.isBlank(userId)) {
                // 将 userId 存入会话
                session.setAttribute(ArthasConstants.USER_ID_KEY, userId);
                logger.debug("Extracted userId from url: {}", userId);
            }
        }
    }

    /**
     * 从 URL 参数里提取用户名和密码
     * <p>
     * 支持从 URL 查询参数中提取认证信息，格式为：?username=hello&password=world
     * 如果未提供 username 参数，则使用默认用户名
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 提取到的 BasicPrincipal 对象，如果缺少密码参数则返回 null
     */
    protected static BasicPrincipal extractBasicAuthSubjectFromUrl(HttpRequest request) {
        // 解码 URL 查询参数
        QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> parameters = queryDecoder.parameters();

        // 获取密码参数（必需）
        List<String> passwords = parameters.get(ArthasConstants.PASSWORD_KEY);
        if (passwords == null || passwords.size() == 0) {
            return null;
        }
        String password = passwords.get(0);

        // 获取用户名参数（可选，使用默认值）
        String username = ArthasConstants.DEFAULT_USERNAME;
        List<String> usernames = parameters.get(ArthasConstants.USERNAME_KEY);
        if (usernames != null && !usernames.isEmpty()) {
            username = usernames.get(0);
        }
        // 创建认证主体
        BasicPrincipal principal = new BasicPrincipal(username, password);
        logger.debug("Extracted Basic Auth principal from url: {}", principal);
        return principal;
    }

    /**
     * 从 HTTP Basic 认证头中提取用户名和密码
     * <p>
     * 从 HTTP Authorization 请求头中提取 Basic 认证的凭据信息。
     * Authorization 头的格式为：Basic base64(username:password)
     * </p>
     * <p>
     * 注意：目前不支持 Digest 认证
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 包含用户名和密码的 BasicPrincipal 对象，如果无法提取则返回 null
     */
    protected static BasicPrincipal extractBasicAuthSubject(HttpRequest request) {
        // 获取 Authorization 请求头
        String auth = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (auth != null) {
            // 提取认证方案（Basic、Bearer 等）
            String constraint = StringUtils.before(auth, " ");
            if (constraint != null) {
                // 检查是否为 Basic 认证
                if ("Basic".equalsIgnoreCase(constraint.trim())) {
                    // 获取 Base64 编码的凭据部分
                    String decoded = StringUtils.after(auth, " ");
                    if (decoded == null) {
                        logger.error("Extracted Basic Auth principal failed, bad auth String: {}", auth);
                        return null;
                    }
                    // the decoded part is base64 encoded, so we need to decode that
                    // Base64 解码
                    ByteBuf buf = Unpooled.wrappedBuffer(decoded.getBytes());
                    ByteBuf out = Base64.decode(buf);
                    String userAndPw = out.toString(Charset.defaultCharset());
                    // 分离用户名和密码
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

    /**
     * 判断是否为 MCP（Model Context Protocol）请求
     * <p>
     * 通过检查请求路径是否与配置的 MCP 端点匹配来判断
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 如果是 MCP 请求返回 true，否则返回 false
     */
    protected static boolean isMcpRequest(HttpRequest request) {
        try {
            // 解析请求 URI 的路径部分
            String path = new java.net.URI(request.uri()).getPath();
            if (path == null) {
                return false;
            }

            // 获取配置的 MCP 端点
            String mcpEndpoint = ArthasBootstrap.getInstance().getConfigure().getMcpEndpoint();
            if (mcpEndpoint == null || mcpEndpoint.trim().isEmpty()) {
                // MCP 服务器未配置，不处理 MCP 请求
                return false;
            }

            // 检查路径是否匹配 MCP 端点
            return mcpEndpoint.equals(path);
        } catch (Exception e) {
            logger.debug("Failed to parse request URI: {}", request.uri(), e);
            return false;
        }
    }

    /**
     * 为 MCP 请求提取认证主体
     * <p>
     * 支持三种认证方式，按优先级依次尝试：
     * <ol>
     *   <li>Bearer Token 认证（从 Authorization header）</li>
     *   <li>Basic Auth 认证（从 Authorization header）</li>
     *   <li>URL 参数认证（从查询参数）</li>
     * </ol>
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 提取到的认证主体对象，如果都无法提取则返回 null
     */
    protected static Principal extractMcpAuthSubject(HttpRequest request) {
        // 首先尝试 Bearer Token 认证
        BearerPrincipal tokenPrincipal = extractBearerTokenSubject(request);
        if (tokenPrincipal != null) {
            return tokenPrincipal;
        }

        // 然后尝试 Basic Auth 认证
        BasicPrincipal basicPrincipal = extractBasicAuthSubject(request);
        if (basicPrincipal != null) {
            return basicPrincipal;
        }

        // 最后尝试从 URL 参数提取
        return extractBasicAuthSubjectFromUrl(request);
    }

    /**
     * 从 Authorization header 中提取 Bearer Token
     * <p>
     * 从 HTTP Authorization 请求头中提取 Bearer Token。
     * Authorization 头的格式为：Bearer &lt;token&gt;
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 包含 Bearer Token 的 BearerPrincipal 对象，如果无法提取则返回 null
     */
    protected static BearerPrincipal extractBearerTokenSubject(HttpRequest request) {
        // 获取 Authorization 请求头
        String auth = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (auth != null) {
            // 提取认证方案
            String constraint = StringUtils.before(auth, " ");
            if (constraint != null) {
                // 检查是否为 Bearer 认证
                if ("Bearer".equalsIgnoreCase(constraint.trim())) {
                    // 获取 Token 部分
                    String token = StringUtils.after(auth, " ");
                    if (token == null || token.trim().isEmpty()) {
                        logger.error("Extracted Bearer Token failed, bad auth String: {}", auth);
                        return null;
                    }
                    // 创建 Bearer 认证主体
                    BearerPrincipal principal = new BearerPrincipal(token.trim());
                    logger.debug("Extracted Bearer Token principal: {}", principal);
                    return principal;
                }
            }
        }
        return null;
    }

}
