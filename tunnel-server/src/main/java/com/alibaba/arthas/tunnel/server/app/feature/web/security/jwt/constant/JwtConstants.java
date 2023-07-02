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

package com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.constant;

import io.jsonwebtoken.Claims;
import lombok.experimental.UtilityClass;

/**
 * JWT 常量定义
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@UtilityClass
public class JwtConstants {

	/** Bearer 认证类型 */
	public static String BEARER_TYPE = "Bearer";

	/** Bearer 认证前缀 */
	public static final String BEARER_PREFIX = BEARER_TYPE + " ";

	public static final String AUDIENCE = Claims.AUDIENCE;

	public static final String EXPIRATION = Claims.EXPIRATION;

	public static final String ID = Claims.ID;

	public static final String ISSUED_AT = Claims.ISSUED_AT;

	public static final String ISSUER = Claims.ISSUER;

	public static final String NOT_BEFORE = Claims.NOT_BEFORE;

	public static final String SUBJECT = Claims.SUBJECT;

	public static final String AUTHORITIES_KEY = "authorities";
}
