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

import lombok.*;

/**
 * 响应（单条记录）
 *
 * @author Frank Zhang
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 2.4.13
 */
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@Data
public class SingleResponse<T> extends Response {

	private static final long serialVersionUID = 1L;

	private T data;

	public static <T> SingleResponse<T> of(T data) {
		SingleResponse<T> response = new SingleResponse<>();
		response.setSuccess(true);
		response.setData(data);
		return response;
	}
}
