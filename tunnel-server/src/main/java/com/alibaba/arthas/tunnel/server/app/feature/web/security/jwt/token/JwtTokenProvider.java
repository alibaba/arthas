/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token;

import com.alibaba.arthas.tunnel.server.app.feature.web.security.exception.UnauthorizedException;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.config.JwtConfig;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.constant.JwtConstants;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.token.AccessToken;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.util.ServletUtils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT 令牌提供器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@Getter
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider implements InitializingBean {

	private final JwtConfig jwtConfig;

	private final JwtTokenStore tokenStore;

	private JwtParser jwtParser;

	private Key key;

	private long tokenValidityInMilliseconds;

	private long tokenValidityInMillisecondsForRememberMe;

	@Override
	public void afterPropertiesSet() {
		byte[] keyBytes = jwtConfig.getBase64Secret() != null ?
			Decoders.BASE64.decode(jwtConfig.getBase64Secret()) :
			jwtConfig.getSecret().getBytes();
		key = Keys.hmacShaKeyFor(keyBytes);
		this.jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
		this.tokenValidityInMilliseconds = 1000 * jwtConfig.getTokenValidityInSeconds();
		this.tokenValidityInMillisecondsForRememberMe = 1000 * jwtConfig.getTokenValidityInSecondsForRememberMe();
	}

	public AccessToken createToken(Authentication authentication, boolean rememberMe, Map<String, Object> claims) {
		if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
			String authorities = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
			claims.put(JwtConstants.AUTHORITIES_KEY, authorities);
		}

		return createToken(authentication.getName(), rememberMe, claims);
	}

	public AccessToken createToken(String subject, boolean rememberMe, Map<String, Object> claims) {
		long now = (new Date()).getTime();
		Date expiration;
		if (rememberMe) {
			expiration = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
		} else {
			expiration = new Date(now + this.tokenValidityInMilliseconds);
		}

		String value = Jwts
			.builder()
			.setSubject(subject)
			.addClaims(claims)
			.signWith(key, SignatureAlgorithm.HS512)
			.setExpiration(expiration)
			.compact();

		AccessToken accessToken = AccessToken.builder()
			.value(value)
			.expiration(expiration)
			.build();
		if (tokenStore != null) {
			tokenStore.storeAccessToken(accessToken);
		}
		return accessToken;
	}

	public void validateToken(AccessToken accessToken) {
		try {
			if (tokenStore != null && !tokenStore.validateAccessToken(accessToken)) {
				throw new UnauthorizedException("存储的令牌不存在");
			}
			jwtParser.parseClaimsJws(accessToken.getValue());
		} catch (ExpiredJwtException e) {
			log.debug(e.getMessage(), e);
			throw new UnauthorizedException("令牌已失效");
		} catch (UnsupportedJwtException e) {
			log.debug(e.getMessage(), e);
			throw new UnauthorizedException("不支持的令牌");
		} catch (MalformedJwtException e) {
			log.debug(e.getMessage(), e);
			throw new UnauthorizedException("令牌格式错误");
		} catch (SecurityException e) {
			log.debug(e.getMessage(), e);
			throw new UnauthorizedException("校验令牌不通过");
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage(), e);
			throw new UnauthorizedException("校验令牌异常");
		}
	}

	public void clearToken(AccessToken accessToken) {
		if (tokenStore != null) {
			tokenStore.removeAccessToken(accessToken);
		}
	}

	public Authentication getAuthentication(AccessToken accessToken) {
		Claims claims = parseClaims(accessToken);

		Collection<? extends GrantedAuthority> authorities = Collections.emptyList();
		if (claims.containsKey(JwtConstants.AUTHORITIES_KEY)) {
			authorities = Arrays
				.stream(claims.get(JwtConstants.AUTHORITIES_KEY).toString().split(","))
				.filter(auth -> !auth.trim().isEmpty())
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());
		}

		User principal = new User(claims.getSubject(), Strings.EMPTY, authorities);
		return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
	}

	public Claims parseClaims(AccessToken accessToken) {
		return jwtParser.parseClaimsJws(accessToken.getValue()).getBody();
	}

	public String getUsername() {
		HttpServletRequest request = ServletUtils.getRequest();
		String bearerToken = request.getHeader(jwtConfig.getHeader());
		if (StringUtils.isBlank(bearerToken) ||
				!bearerToken.startsWith(JwtConstants.BEARER_PREFIX)) {
			return null;
		}
		AccessToken accessToken = AccessToken.builder()
					.value(bearerToken.substring(JwtConstants.BEARER_PREFIX.length()))
					.build();
		Authentication authentication = getAuthentication(accessToken);
		return authentication.getName();
	}
}
