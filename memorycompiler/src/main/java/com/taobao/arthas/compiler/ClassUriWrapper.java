package com.taobao.arthas.compiler;

/*-
 * #%L
 * compiler
 * %%
 * Copyright (C) 2017 - 2018 SkaLogs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.net.URI;

/**
 * 类URI包装器
 *
 * <p>用于封装类的全限定名和对应的URI资源位置。这是一个简单的值对象，
 * 主要用于在动态编译过程中关联类的名称和其资源位置。</p>
 *
 * @author arthas
 * @since 2017-2018
 */
public class ClassUriWrapper {

    /**
     * 类资源的URI标识
     * 指向类文件的位置，可以是本地文件路径或网络资源
     */
    private final URI uri;

    /**
     * 类的全限定名
     * 例如：com.taobao.arthas.compiler.ClassUriWrapper
     */
    private final String className;

    /**
     * 构造函数
     *
     * @param className 类的全限定名
     * @param uri 类资源的URI标识
     */
    public ClassUriWrapper(String className, URI uri) {
        this.className = className;
        this.uri = uri;
    }

    /**
     * 获取类资源的URI
     *
     * @return 类资源的URI标识
     */
    public URI getUri() {
        return uri;
    }

    /**
     * 获取类的全限定名
     *
     * @return 类的全限定名
     */
    public String getClassName() {
        return className;
    }
}
