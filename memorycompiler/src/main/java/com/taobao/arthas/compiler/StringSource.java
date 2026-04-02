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
import java.io.IOException;
import java.net.URI;

/**
 * 基于字符串的Java源代码表示类
 *
 * 该类继承自Java Compiler API的SimpleJavaFileObject，
 * 用于将存储在内存中的Java源代码字符串表示为一个可以被Java编译器识别和编译的文件对象。
 *
 * 核心功能：
 * - 将字符串形式的Java源代码包装成JavaFileObject
 * - 支持Java Compiler API的内存编译功能
 * - 无需创建物理文件即可编译Java代码
 *
 * 主要应用场景：
 * - 动态代码生成和编译（如Arthas的ognl表达式编译）
 * - 运行时代码生成（如代码热更新）
 * - 脚本引擎实现
 * - 代码模板编译
 *
 * 技术实现：
 * - 使用自定义的URI scheme（"string:///"）来标识内存中的源代码
 * - 将类名转换为文件系统路径格式（包名用斜杠分隔）
 * - 保持Java源代码的Kind.SOURCE类型
 *
 * @author Arthas Team (基于SkaLogs的compiler项目)
 * @see javax.tools.JavaCompiler
 * @see javax.tools.SimpleJavaFileObject
 */
public class StringSource extends SimpleJavaFileObject {

    /**
     * Java源代码的内容
     *
     * 存储完整的Java源代码字符串，该字符串应该是符合Java语法的完整类定义。
     * 使用final修饰，确保内容在对象创建后不可修改，保证线程安全性。
     *
     * 例如：
     * <pre>
     * "package com.example;\n" +
     * "public class MyClass {\n" +
     * "    public void test() {\n" +
     * "        System.out.println(\"Hello\");\n" +
     * "    }\n" +
     * "}"
     * </pre>
     */
    private final String contents;

    /**
     * 构造函数 - 创建字符串源代码对象
     *
     * 该构造函数将类名和源代码内容封装为一个JavaFileObject，
     * 可以被Java Compiler API识别和编译。
     *
     * 实现细节：
     * 1. 构建URI：使用"string:///"作为scheme，表示这是内存中的源代码
     * 2. 路径转换：将类名中的点号替换为斜杠，转换为文件系统路径格式
     *    例如："com.example.MyClass" -> "com/example/MyClass"
     * 3. 添加扩展名：自动添加Java源文件的扩展名（.java）
     *    完整URI示例："string:///com/example/MyClass.java"
     * 4. 设置类型：指定为Kind.SOURCE，表示这是Java源代码文件
     *
     * URI格式说明：
     * - scheme: "string" - 自定义协议，标识这是内存中的源代码
     * - path: "/" + 类名路径 - 表示类的包结构和类名
     * - extension: ".java" - Java源文件的标准扩展名
     *
     * @param className 完整的类名（包含包名），格式如 "com.example.MyClass"
     * @param contents Java源代码的内容字符串，应该是完整的类定义
     */
    public StringSource(String className, String contents) {
        // 调用父类SimpleJavaFileObject的构造函数
        // 参数1: 创建URI对象，格式为 "string:///包路径/类名.java"
        //        - className.replace('.', '/') 将包名中的点号转换为路径分隔符
        //        - Kind.SOURCE.extension 获取Java源文件的扩展名（".java"）
        // 参数2: 指定文件类型为Kind.SOURCE（Java源代码）
        super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);

        // 保存源代码内容
        this.contents = contents;
    }

    /**
     * 获取源代码内容
     *
     * 该方法实现了JavaFileObject接口的getCharContent方法，
     * 返回字符串形式的源代码内容供编译器使用。
     *
     * 编译流程：
     * 1. Java Compiler API调用此方法获取源代码内容
     * 2. 将返回的CharSequence传递给编译器
     * 3. 编译器解析并编译源代码
     * 4. 生成编译后的class文件（通常也在内存中）
     *
     * @param ignoreEncodingErrors 是否忽略编码错误
     *                             - true: 忽略编码错误，即使存在编码问题也继续处理
     *                             - false: 遇到编码错误时抛出异常
     *                             由于我们使用的是字符串（Unicode），不涉及文件编码，
     *                             所以该参数通常被忽略
     * @return 源代码内容的字符序列，即存储的Java代码字符串
     * @throws IOException 如果发生I/O错误（在内存操作中通常不会抛出）
     */
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        // 直接返回存储的源代码字符串
        // 字符串本身就是CharSequence接口的实现
        return contents;
    }

}
