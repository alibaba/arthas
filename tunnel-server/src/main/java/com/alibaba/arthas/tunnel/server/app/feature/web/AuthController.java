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

package com.alibaba.arthas.tunnel.server.app.feature.web;

import com.alibaba.arthas.tunnel.server.app.feature.dto.SingleResponse;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.jwt.token.JwtTokenService;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.token.AccessToken;
import com.alibaba.arthas.tunnel.server.app.feature.web.security.user.LoginUserDetails;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.x
 */
@RequiredArgsConstructor
@Slf4j
@RestController
public class AuthController {

    private final JwtTokenService jwtTokenService;

    @PostMapping("/api/auth")
    public SingleResponse<AccessToken> authenticate(@RequestBody LoginUserDetails user) {
        AccessToken accessToken = jwtTokenService.authenticate(user, Maps.newHashMap());
        return SingleResponse.of(accessToken);
    }
}
