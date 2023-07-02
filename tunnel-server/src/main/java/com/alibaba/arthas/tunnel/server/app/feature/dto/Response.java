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

package com.alibaba.arthas.tunnel.server.app.feature.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * 响应
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Data
public class Response implements Serializable {

	private static final long serialVersionUID = 1L;

	private boolean success;

	private String errCode;

	private String errMessage;

	public static Response buildSuccess() {
		Response response = new Response();
		response.setSuccess(true);
		return response;
	}

	public static Response buildFailed(String errCode, String errMessage) {
		Response response = new Response();
		response.setSuccess(false);
		response.setErrCode(errCode);
		response.setErrMessage(errMessage);
		return response;
	}
}
