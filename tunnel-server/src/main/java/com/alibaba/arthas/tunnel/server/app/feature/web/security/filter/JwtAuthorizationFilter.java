package com.alibaba.arthas.tunnel.server.app.feature.web.security.filter;

import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.constant.JwtConstants;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenProvider;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.token.AccessToken;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.util.ServletUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.PathMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final String TOKEN_IS_REQUIRED = "Token is required";

    private final JwtTokenProvider jwtTokenProvider;

    private final PathMatcher pathMatcher;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager,
                                  JwtTokenProvider jwtTokenProvider, PathMatcher pathMatcher) {
        super(authenticationManager);
        this.jwtTokenProvider = jwtTokenProvider;
        this.pathMatcher = pathMatcher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        if (!isAnonymousUrls(request)) {
            AccessToken accessToken = resolveToken(request);
            if (accessToken == null) {
                ServletUtils.wrap(response, HttpServletResponse.SC_UNAUTHORIZED, "USER-AUTH-400", TOKEN_IS_REQUIRED);
                return;
            }
            try {
                this.jwtTokenProvider.validateToken(accessToken);
            } catch (Exception e) {
                ServletUtils.wrap(response, HttpServletResponse.SC_UNAUTHORIZED, "USER-AUTH-400", e.getMessage());
                return;
            }
            Authentication authentication = this.jwtTokenProvider.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }

    private AccessToken resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtTokenProvider.getJwtConfig().getHeader());
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(JwtConstants.BEARER_PREFIX)) {
            return AccessToken.builder().value(bearerToken.substring(JwtConstants.BEARER_PREFIX.length())).build();
        }
        return null;
    }

    private boolean isAnonymousUrls(HttpServletRequest request) {
        List<String> anonymousUrls = jwtTokenProvider.getJwtConfig().getAnonymousUrls();
        if (anonymousUrls == null || anonymousUrls.isEmpty()) {
            return false;
        }
        String requestURI = request.getRequestURI();
        return anonymousUrls.stream().anyMatch(url -> pathMatcher.match(url, requestURI));
    }
}
