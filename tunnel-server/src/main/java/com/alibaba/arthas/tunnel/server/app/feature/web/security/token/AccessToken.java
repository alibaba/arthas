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

package com.alibaba.arthas.tunnel.server.app.feature.web.security.token;

import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.constant.JwtConstants;
import lombok.*;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 访问令牌
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class AccessToken {

	/**
	 * 访问令牌
	 */
	private String value;

	/**
	 * 刷新令牌
	 */
	private RefreshToken refreshToken;

	/**
	 * 过期时间
	 */
	private Date expiration;

	/**
	 * 授权类型
	 */
	private String tokenType = JwtConstants.BEARER_TYPE.toLowerCase();

	/**
	 * 作用域
	 */
	private Set<String> scope;

	/**
	 * 附加信息
	 */
	private Map<String, Object> additionalInformation = Collections.emptyMap();

	public int getExpiresIn() {
		return expiration != null ? Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L).intValue() : 0;
	}

	public void setExpiresIn(int delta) {
		setExpiration(new Date(System.currentTimeMillis() + delta));
	}

	public boolean isExpired() {
		return expiration != null && expiration.before(new Date());
	}
}
