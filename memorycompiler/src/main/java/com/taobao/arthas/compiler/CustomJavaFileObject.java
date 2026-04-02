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

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

/**
 * 自定义Java文件对象
 *
 * <p>实现了JavaFileObject接口，用于表示一个编译后的类文件。
 * 该类主要用于动态编译场景，通过URI来定位和读取已编译的类文件。</p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>支持通过URI读取类文件</li>
 *   <li>只读模式，不支持写入操作</li>
 *   <li>简化了JavaFileObject接口的部分实现</li>
 * </ul>
 *
 * @author arthas
 * @since 2017-2018
 */
public class CustomJavaFileObject implements JavaFileObject {

    /**
     * 类的全限定名
     * 例如：com.taobao.arthas.compiler.CustomJavaFileObject
     */
    private final String className;

    /**
     * 类文件的URI标识
     * 用于定位和读取类文件资源
     */
    private final URI uri;

    /**
     * 构造函数
     *
     * @param className 类的全限定名
     * @param uri 类文件的URI标识
     */
    public CustomJavaFileObject(String className, URI uri) {
        this.uri = uri;
        this.className = className;
    }

    /**
     * 获取类文件的URI标识
     *
     * @return 类文件的URI
     */
    public URI toUri() {
        return uri;
    }

    /**
     * 打开输入流以读取类文件内容
     *
     * @return 类文件的输入流
     * @throws IOException 如果打开流失败
     */
    public InputStream openInputStream() throws IOException {
        // 将URI转换为URL并打开流
        return uri.toURL().openStream();
    }

    /**
     * 打开输出流
     *
     * @return 输出流
     * @throws UnsupportedOperationException 不支持写入操作，始终抛出此异常
     */
    public OutputStream openOutputStream() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取类的名称
     *
     * @return 类的全限定名
     */
    public String getName() {
        return this.className;
    }

    /**
     * 打开字符读取器
     *
     * @param ignoreEncodingErrors 是否忽略编码错误
     * @return 字符读取器
     * @throws UnsupportedOperationException 不支持此操作，始终抛出此异常
     */
    public Reader openReader(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取字符内容
     *
     * @param ignoreEncodingErrors 是否忽略编码错误
     * @return 字符内容
     * @throws UnsupportedOperationException 不支持此操作，始终抛出此异常
     */
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        throw new UnsupportedOperationException();
    }

    /**
     * 打开字符写入器
     *
     * @return 字符写入器
     * @throws IOException 如果发生I/O错误
     * @throws UnsupportedOperationException 不支持写入操作，始终抛出此异常
     */
    public Writer openWriter() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取最后修改时间
     *
     * @return 最后修改时间（毫秒），此实现始终返回0
     */
    public long getLastModified() {
        return 0;
    }

    /**
     * 删除此文件对象
     *
     * @return 如果删除成功返回true
     * @throws UnsupportedOperationException 不支持此操作，始终抛出此异常
     */
    public boolean delete() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取文件对象的类型
     *
     * @return 文件类型，此实现返回Kind.CLASS表示这是一个类文件
     */
    public Kind getKind() {
        return Kind.CLASS;
    }

    /**
     * 检查名称是否兼容
     *
     * @param simpleName 简单类名
     * @param kind 文件类型
     * @return 如果名称兼容返回true，否则返回false
     */
    public boolean isNameCompatible(String simpleName, Kind kind) {
        // 检查是否是CLASS类型，并且类名以给定的简单名称结尾
        return Kind.CLASS.equals(getKind())
                && this.className.endsWith(simpleName);
    }

    /**
     * 获取嵌套类型
     *
     * @return 嵌套类型
     * @throws UnsupportedOperationException 不支持此操作，始终抛出此异常
     */
    public NestingKind getNestingKind() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取访问级别修饰符
     *
     * @return 访问级别修饰符
     * @throws UnsupportedOperationException 不支持此操作，始终抛出此异常
     */
    public Modifier getAccessLevel() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取类的全限定名
     *
     * @return 类的全限定名
     */
    public String getClassName() {
        return this.className;
    }


    /**
     * 获取对象的字符串表示
     *
     * @return 对象的字符串表示，包含类名和URI
     */
    public String toString() {
        return this.getClass().getName() + "[" + this.toUri() + "]";
    }
}

