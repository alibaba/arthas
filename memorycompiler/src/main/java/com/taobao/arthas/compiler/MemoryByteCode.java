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

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * 内存字节码类
 *
 * <p>该类继承自SimpleJavaFileObject，用于在内存中存储编译后的字节码。
 * 它是Java编译器输出目标和内存存储之间的桥梁，使得编译结果可以直接保存在内存中，
 * 而不需要写入磁盘文件。</p>
 *
 * <p>主要功能：
 * <ul>
 *   <li>接收Java编译器输出的字节码</li>
 *   <li>在内存中维护字节码数据</li>
 *   <li>提供对字节码的访问接口</li>
 *   <li>支持类名和URI之间的转换</li>
 * </ul>
 * </p>
 *
 * <p>使用场景：
 * <ul>
 *   <li>动态代码生成和编译</li>
 *   <li>热加载和代码替换</li>
 *   <li>表达式求值</li>
 *   <li>脚本语言集成</li>
 * </ul>
 * </p>
 */
public class MemoryByteCode extends SimpleJavaFileObject {

    /**
     * 包分隔符
     *
     * <p>Java中使用点号(.)来分隔包名中的各个部分，如"com.example.demo"。</p>
     */
    private static final char PKG_SEPARATOR = '.';

    /**
     * 目录分隔符
     *
     * <p>在URI和文件路径中，使用斜杠(/)来分隔目录层级，
     * 对应包结构的层次关系。</p>
     */
    private static final char DIR_SEPARATOR = '/';

    /**
     * 类文件后缀
     *
     * <p>Java编译后的类文件使用".class"作为文件扩展名。</p>
     */
    private static final String CLASS_FILE_SUFFIX = ".class";

    /**
     * 字节数组输出流
     *
     * <p>用于在内存中存储编译后的字节码数据。
     * 编译器通过openOutputStream()方法获取输出流，
     * 然后将编译结果写入这个流中。</p>
     */
    private ByteArrayOutputStream byteArrayOutputStream;

    /**
     * 构造函数 - 根据类名创建内存字节码对象
     *
     * <p>创建一个MemoryByteCode对象，用于接收编译器输出的字节码。
     * 构造函数会将类名转换为URI格式，符合SimpleJavaFileObject的要求。</p>
     *
     * @param className Java类的全限定名（如"com.example.MyClass"）
     */
    public MemoryByteCode(String className) {
        // 将类名转换为URI格式：byte:///com/example/MyClass.class
        super(URI.create("byte:///" + className.replace(PKG_SEPARATOR, DIR_SEPARATOR)
                + Kind.CLASS.extension), Kind.CLASS);
    }

    /**
     * 构造函数 - 根据类名和字节数组输出流创建内存字节码对象
     *
     * <p>创建一个MemoryByteCode对象，并使用已有的字节数组输出流。
     * 这个构造函数用于已经存在字节数据的情况。</p>
     *
     * @param className Java类的全限定名
     * @param byteArrayOutputStream 已有的字节数组输出流
     * @throws URISyntaxException 如果URI语法错误
     */
    public MemoryByteCode(String className, ByteArrayOutputStream byteArrayOutputStream)
            throws URISyntaxException {
        // 调用基本构造函数创建对象
        this(className);
        // 保存字节数组输出流的引用
        this.byteArrayOutputStream = byteArrayOutputStream;
    }

    /**
     * 打开输出流
     *
     * <p>返回一个OutputStream，编译器通过这个输出流将编译后的字节码写入内存。
     * 这是Java Compiler API调用的关键方法，编译器会获取这个流并写入字节码。</p>
     *
     * @return 用于写入字节码的输出流
     * @throws IOException 如果发生I/O错误
     */
    @Override
    public OutputStream openOutputStream() throws IOException {
        // 延迟初始化：只在第一次需要时创建输出流
        if (byteArrayOutputStream == null) {
            byteArrayOutputStream = new ByteArrayOutputStream();
        }
        return byteArrayOutputStream;
    }

    /**
     * 获取编译后的字节码
     *
     * <p>返回编译器生成的类文件的字节数组。
     * 这个字节数组可以被类加载器用来定义类。</p>
     *
     * @return 包含类字节码的字节数组
     */
    public byte[] getByteCode() {
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 获取类名
     *
     * <p>从URI中提取Java类的全限定名。
     * 这个方法将URI格式转换回标准的Java类名格式。</p>
     *
     * <p>转换示例：
     * <ul>
     *   <li>URI: /com/example/MyClass.class</li>
     *   <li>类名: com.example.MyClass</li>
     * </ul>
     * </p>
     *
     * @return Java类的全限定名
     */
    public String getClassName() {
        // 获取URI名称
        String className = getName();
        // 将目录分隔符转换为包分隔符
        className = className.replace(DIR_SEPARATOR, PKG_SEPARATOR);
        // 去掉开头的斜杠和.class后缀，得到标准的类名格式
        className = className.substring(1, className.indexOf(CLASS_FILE_SUFFIX));
        return className;
    }

}
