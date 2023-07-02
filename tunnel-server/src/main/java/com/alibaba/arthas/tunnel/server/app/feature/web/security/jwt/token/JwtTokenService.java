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
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.constant.JwtConstants;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.token.AccessToken;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.user.LoginUserDetails;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

/**
 * JWT 令牌验证服务
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

	private static final String AUTHENTICATE_BAD_CREDENTIALS = "JWT authenticated failed due to bad credentials：{}";

	private static final String AUTHENTICATE_EXCEPTION = "JWT authenticated failed, caught exception: {}";

	private final AuthenticationManager authenticationManager;

	private final JwtTokenProvider jwtTokenProvider;

	/**
	 * 认证
	 *
	 * @param login 登录信息
	 * @param claims 附加信息
	 * @return 访问令牌
	 */
	public AccessToken authenticate(LoginUserDetails login, Map<String, Object> claims) {
		try {
			UsernamePasswordAuthenticationToken authenticationToken =
				new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());
			Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			if (authentication.getAuthorities() != null) {
				if (claims == null) {
					claims = Maps.newHashMap();
				}
				StringBuilder authorities = new StringBuilder();
				for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
					authorities.append(grantedAuthority.getAuthority()).append(",");
				}
				authorities.deleteCharAt(authorities.length() - 1);
				claims.put(JwtConstants.AUTHORITIES_KEY, authorities);
			}

			return jwtTokenProvider.createToken(authentication, login.isRememberMe(), claims);
		} catch (BadCredentialsException ex) {
			log.error(AUTHENTICATE_BAD_CREDENTIALS, ex.getMessage(), ex);
			throw new UnauthorizedException(ex.getMessage());
		} catch (Exception ex) {
			log.error(AUTHENTICATE_EXCEPTION, ex.getMessage(), ex);
			throw ex;
		}
	}
}
